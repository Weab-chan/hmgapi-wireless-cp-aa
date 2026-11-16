package hmgapi.hook;

public final class Cluster {
    public static boolean available = true;
    public static Cluster of(android.content.Context c) { return available ? new Cluster() : null; }
}
