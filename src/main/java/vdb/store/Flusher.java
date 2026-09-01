package vdb.store;

import vdb.model.Doc;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/** Writes docs to a .seg file via temp file + atomic rename. */
public class Flusher {
    public static DiskStore flush(List<Doc> docs, int dims, Path out) throws IOException {
        Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmp.toFile())))) {
            dos.writeInt(dims);
            dos.writeInt(docs.size());
            for (Doc d : docs) {
                if (d.vector().length != dims)
                    throw new IllegalArgumentException(
                        "doc " + d.id() + " has " + d.vector().length + " dims, expected " + dims);
                byte[] idBytes = d.id().getBytes(StandardCharsets.UTF_8);
                dos.writeInt(idBytes.length);
                dos.write(idBytes);
                for (float v : d.vector()) dos.writeFloat(v);
                byte[] metaBytes = d.metadata().getBytes(StandardCharsets.UTF_8);
                dos.writeInt(metaBytes.length);
                dos.write(metaBytes);
            }
        }
        try {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        }
        return new DiskStore(out);
    }
}
