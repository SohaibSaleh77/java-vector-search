package vdb;

import vdb.metric.Cosine;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    private Path dir;
    private Engine db;

    @BeforeEach
    void setup() throws IOException {
        dir = Files.createTempDirectory("vdb");
        db = new Engine(dir, 3, 2, new Cosine());
    }

    @AfterEach
    void cleanup() throws IOException {
        if (db != null) { db.close(); db = null; }
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> {
            try { Files.delete(p); } catch (IOException ignored) { }
        });
    }

    @Test
    void insertAndSearch() throws Exception {
        db.insert("d1", new float[]{0.9f, 0.1f, 0.1f}, "sports");
        db.insert("d2", new float[]{0.1f, 0.9f, 0.1f}, "tech");
        db.insert("d3", new float[]{0.1f, 0.1f, 0.9f}, "finance");
        var r = db.search(new float[]{0.1f, 0.8f, 0.2f}, 1);
        assertEquals(1, r.size());
        assertEquals("d2", r.get(0).id());
        assertEquals("tech", r.get(0).metadata());
    }

    @Test
    void persistsAcrossRestart() throws Exception {
        db.insert("x1", new float[]{1f, 0f, 0f}, "a");
        db.insert("x2", new float[]{0f, 1f, 0f}, "b");
        db.insert("x3", new float[]{0f, 0f, 1f}, "c");
        db.close();
        db = null;
        db = new Engine(dir, 3, 2, new Cosine());
        var r = db.search(new float[]{1f, 0f, 0f}, 1);
        assertEquals(1, r.size());
        assertEquals("x1", r.get(0).id());
    }

    @Test
    void rejectsWrongDimension() {
        assertThrows(IllegalArgumentException.class,
            () -> db.insert("bad", new float[]{1f, 2f}, "m"));
    }

    @Test
    void rejectsDuplicateId() throws Exception {
        db.insert("dup", new float[]{1f, 0f, 0f}, "first");
        assertThrows(IllegalArgumentException.class,
            () -> db.insert("dup", new float[]{0f, 1f, 0f}, "second"));
    }

    @Test
    void doubleCloseIsSafe() throws Exception {
        db.insert("d1", new float[]{1f, 0f, 0f}, "m");
        db.close();
        db.close();
    }
}
