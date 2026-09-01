package vdb.store;

import vdb.model.Doc;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemStoreTest {
    @Test
    void addAndSnapshot() {
        MemStore s = new MemStore();
        s.add(new Doc("1", new float[]{0.1f}, "m1"));
        s.add(new Doc("2", new float[]{0.2f}, "m2"));
        assertEquals(2, s.size());
        assertEquals(2, s.snapshot().size());
        assertEquals(2, s.size());
    }
}
