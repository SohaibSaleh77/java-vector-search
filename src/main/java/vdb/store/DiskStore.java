package vdb.store;

import vdb.metric.Metric;
import vdb.model.Doc;
import vdb.model.Result;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** Read-only store backed by a memory-mapped .seg file. */
public class DiskStore implements Store {
    private final RandomAccessFile file;
    private final MappedByteBuffer buffer;
    private final int dims;
    private final int count;

    public DiskStore(Path path) throws IOException {
        this.file = new RandomAccessFile(path.toFile(), "r");
        FileChannel ch = file.getChannel();
        this.buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        if (buffer.remaining() < 8)
            throw new IOException("Corrupt segment: " + path);
        this.dims  = buffer.getInt();
        this.count = buffer.getInt();
    }

    /** Dimensions stored in this segment. */
    public int dims() { return dims; }

    /** All document IDs in this segment. Used for duplicate detection on startup. */
    public List<String> ids() {
        ByteBuffer r = buffer.duplicate();
        r.position(8);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idLen = r.getInt();
            if (idLen < 0 || idLen > r.remaining())
                throw new IllegalStateException("bad id length");
            byte[] idBytes = new byte[idLen];
            r.get(idBytes);
            result.add(new String(idBytes, StandardCharsets.UTF_8));
            r.position(r.position() + dims * Float.BYTES);
            int metaLen = r.getInt();
            if (metaLen < 0 || metaLen > r.remaining())
                throw new IllegalStateException("bad meta length");
            r.position(r.position() + metaLen);
        }
        return result;
    }

    @Override
    public void add(Doc doc) {
        throw new UnsupportedOperationException("DiskStore is read-only");
    }

    @Override
    public List<Result> search(float[] query, int k, Metric metric) {
        PriorityQueue<Result> heap = new PriorityQueue<>(Collections.reverseOrder());
        ByteBuffer r = buffer.duplicate();
        r.position(8);

        float[] vec = new float[dims];
        for (int i = 0; i < count; i++) {
            int idLen = r.getInt();
            if (idLen < 0 || idLen > r.remaining())
                throw new IllegalStateException("bad id length");
            byte[] idBytes = new byte[idLen];
            r.get(idBytes);
            String id = new String(idBytes, StandardCharsets.UTF_8);

            for (int v = 0; v < dims; v++) vec[v] = r.getFloat();

            int metaLen = r.getInt();
            if (metaLen < 0 || metaLen > r.remaining())
                throw new IllegalStateException("bad meta length");
            byte[] metaBytes = new byte[metaLen];
            r.get(metaBytes);
            String meta = new String(metaBytes, StandardCharsets.UTF_8);

            float dist = metric.compute(vec, query);
            if (heap.size() < k) {
                heap.offer(new Result(id, dist, meta));
            } else if (dist < heap.peek().distance()) {
                heap.poll();
                heap.offer(new Result(id, dist, meta));
            }
        }
        List<Result> out = new ArrayList<>(heap);
        out.sort(Comparator.naturalOrder());
        return out;
    }

    @Override
    public int size() { return count; }

    @Override
    public void close() throws IOException { file.close(); }
}
