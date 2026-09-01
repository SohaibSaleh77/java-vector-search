package vdb.model;

/** A search result: id, distance (lower = closer), metadata. */
public record Result(String id, float distance, String metadata)
        implements Comparable<Result> {
    @Override
    public int compareTo(Result o) {
        return Float.compare(this.distance, o.distance);
    }
}
