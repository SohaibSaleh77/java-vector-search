package vdb;

import vdb.metric.Metric;
import vdb.model.Doc;
import vdb.model.Result;
import vdb.store.*;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Vector database engine.
 * Inserts go into an in-memory store. When full, contents are flushed
 * to a memory-mapped file. Search fans out across all stores in parallel
 * using virtual threads and merges the top-K.
 */
public class Engine implements Closeable {
    private final Path storageDir;
    private final int dims;
    private final int maxMemSize;
    private final Metric metric;

    private volatile boolean closed = false;
    private MemStore active;
    private final Set<String> ids = ConcurrentHashMap.newKeySet();
    private final List<Store> stores = new CopyOnWriteArrayList<>();
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger counter = new AtomicInteger(0);

    public Engine(Path storageDir, int dims, int maxMemSize, Metric metric) throws IOException {
        this.storageDir = storageDir;
        this.dims       = dims;
        this.maxMemSize = maxMemSize;
        this.metric     = metric;
        Files.createDirectories(storageDir);
        loadDiskStores();
        this.active = new MemStore();
        this.stores.add(active);
    }

    // Reopen .seg files from a previous run. Validate dims, collect IDs,
    // clean up leftover .tmp files from a crashed flush.
    private void loadDiskStores() throws IOException {
        try (DirectoryStream<Path> tmpStream = Files.newDirectoryStream(storageDir, "*.tmp")) {
            for (Path p : tmpStream) {
                Files.deleteIfExists(p);
            }
        }
        int maxNum = -1;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(storageDir, "*.seg")) {
            for (Path p : ds) {
                DiskStore disk = new DiskStore(p);
                if (disk.dims() != dims) {
                    disk.close();
                    throw new IOException(
                        "segment " + p.getFileName() + " has " + disk.dims() +
                        " dims, expected " + dims);
                }
                stores.add(disk);
                ids.addAll(disk.ids());
                String name = p.getFileName().toString();
                try {
                    int n = Integer.parseInt(name.substring(4, name.length() - 4));
                    if (n > maxNum) maxNum = n;
                } catch (NumberFormatException | IndexOutOfBoundsException ignored) { }
            }
        }
        counter.set(maxNum + 1);
    }

    /** Insert a vector. Rejects duplicate IDs. Flushes to disk when full. */
    public synchronized void insert(String id, float[] vector, String metadata) throws IOException {
        if (closed) throw new IllegalStateException("engine is closed");
        if (vector.length != dims)
            throw new IllegalArgumentException(
                "expected " + dims + " dims, got " + vector.length);
        if (!ids.add(id))
            throw new IllegalArgumentException("duplicate id: " + id);
        active.add(new Doc(id, vector, metadata));
        if (active.size() >= maxMemSize) flush();
    }

    private void flush() throws IOException {
        List<Doc> snapshot = active.snapshot();
        Path path = storageDir.resolve("seg_" + counter.getAndIncrement() + ".seg");
        DiskStore disk = Flusher.flush(snapshot, dims, path);
        int idx = stores.indexOf(active);
        if (idx >= 0) stores.set(idx, disk); else stores.add(disk);
        MemStore fresh = new MemStore();
        stores.add(fresh);
        active = fresh;
    }

    /** Search all stores in parallel, merge into a global top-K. */
    public List<Result> search(float[] query, int k)
            throws InterruptedException, ExecutionException {
        if (closed) throw new IllegalStateException("engine is closed");
        List<Callable<List<Result>>> tasks = new ArrayList<>();
        for (Store s : stores)
            tasks.add(() -> s.search(query, k, metric));
        List<Future<List<Result>>> futures = pool.invokeAll(tasks);
        PriorityQueue<Result> heap = new PriorityQueue<>(Collections.reverseOrder());
        for (Future<List<Result>> f : futures) {
            for (Result r : f.get()) {
                if (heap.size() < k) heap.offer(r);
                else if (r.distance() < heap.peek().distance()) {
                    heap.poll();
                    heap.offer(r);
                }
            }
        }
        List<Result> out = new ArrayList<>(heap);
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /** Flush remaining docs, shut down threads, close all stores. Idempotent. */
    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;
        if (active != null && active.size() > 0) {
            List<Doc> snapshot = active.snapshot();
            Path path = storageDir.resolve("seg_" + counter.getAndIncrement() + ".seg");
            DiskStore disk = Flusher.flush(snapshot, dims, path);
            int idx = stores.indexOf(active);
            if (idx >= 0) stores.set(idx, disk);
        }
        active = null;
        pool.shutdown();
        try { pool.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        for (Store s : stores) {
            try { s.close(); } catch (IOException ignored) { }
        }
    }
}
