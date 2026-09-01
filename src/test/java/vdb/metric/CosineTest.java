package vdb.metric;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosineTest {
    @Test
    void identicalVectorsHaveZeroDistance() {
        Metric m = new Cosine();
        float d = m.compute(new float[]{1f, 2f, 3f}, new float[]{1f, 2f, 3f});
        assertTrue(d < 0.0001f, "got " + d);
    }

    @Test
    void orthogonalVectorsHaveMaxDistance() {
        Metric m = new Cosine();
        float d = m.compute(new float[]{1f, 0f}, new float[]{0f, 1f});
        assertTrue(Math.abs(d - 1f) < 0.0001f, "got " + d);
    }
}
