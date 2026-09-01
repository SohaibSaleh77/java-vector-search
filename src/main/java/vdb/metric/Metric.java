package vdb.metric;

/** Distance between two vectors. Lower means more similar. */
public interface Metric {
    float compute(float[] a, float[] b);
}
