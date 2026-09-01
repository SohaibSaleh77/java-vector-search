package vdb.metric;

/** Cosine distance: 1 - cosine similarity. 0 = identical direction. */
public class Cosine implements Metric {
    @Override
    public float compute(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 1f;
        return (float) (1.0 - dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
