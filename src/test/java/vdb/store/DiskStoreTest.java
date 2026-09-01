package vdb.store;

import vdb.metric.Cosine;
import vdb.model.Doc;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiskStoreTest {
    @Test
    void flushAndReadBack() throws Exception {
        Path tmp = Files.createTempFile("seg", ".seg");
        try {
            List<Doc> docs = List.of(
                new Doc("a", new float[]{1f, 0f}, "ma"),
                new Doc("b", new float[]{0f, 1f}, "mb")
            );
            try (DiskStore s = Flusher.flush(docs, 2, tmp)) {
                assertEquals(2, s.size());
                assertEquals(2, s.dims());
                assertEquals(List.of("a", "b"), s.ids());
                var r = s.search(new float[]{1f, 0.1f}, 1, new Cosine());
                assertEquals(1, r.size());
                assertEquals("a", r.get(0).id());
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
