package vdb.store;

import vdb.metric.Metric;
import vdb.model.Doc;
import vdb.model.Result;
import java.util.*;
import java.util.concurrent.locks.*;

/** In-memory store. ReadWriteLock allows concurrent reads, exclusive writes. */
public class MemStore implements Store {
    private final List<Doc> docs = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(Doc doc) {
        lock.writeLock().lock();
        try { docs.add(doc); } finally { lock.writeLock().unlock(); }
    }

    @Override
    public List<Result> search(float[] query, int k, Metric metric) {
        lock.readLock().lock();
        try {
            PriorityQueue<Result> heap = new PriorityQueue<>(Collections.reverseOrder());
            for (Doc d : docs) {
                float dist = metric.compute(d.vector(), query);
                if (heap.size() < k) {
                    heap.offer(new Result(d.id(), dist, d.metadata()));
                } else if (dist < heap.peek().distance()) {
                    heap.poll();
                    heap.offer(new Result(d.id(), dist, d.metadata()));
                }
            }
            List<Result> out = new ArrayList<>(heap);
            out.sort(Comparator.naturalOrder());
            return out;
        } finally { lock.readLock().unlock(); }
    }

    /** Copy of current docs. Does not clear. Used when flushing to disk. */
    public List<Doc> snapshot() {
        lock.readLock().lock();
        try { return new ArrayList<>(docs); } finally { lock.readLock().unlock(); }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try { return docs.size(); } finally { lock.readLock().unlock(); }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try { docs.clear(); } finally { lock.writeLock().unlock(); }
    }
}
