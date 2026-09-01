package vdb.model;

import java.util.Objects;

/** A document: id, vector, metadata. Immutable. */
public record Doc(String id, float[] vector, String metadata) {
    public Doc {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(vector, "vector required");
        Objects.requireNonNull(metadata, "metadata required");
    }
}
