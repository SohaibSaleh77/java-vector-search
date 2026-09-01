package vdb.metric;

/** Euclidean (L2) distance. */
public class Euclidean implements Metric {
    @Override
    public float compute(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return (float) Math.sqrt(sum);
    }
}
