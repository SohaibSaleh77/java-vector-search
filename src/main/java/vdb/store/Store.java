package vdb.store;

import vdb.metric.Metric;
import vdb.model.Doc;
import vdb.model.Result;
import java.io.Closeable;
import java.util.List;

/** A collection of vectors that can be searched. */
public interface Store extends Closeable {
    void add(Doc doc);
    List<Result> search(float[] query, int k, Metric metric);
    int size();
}
