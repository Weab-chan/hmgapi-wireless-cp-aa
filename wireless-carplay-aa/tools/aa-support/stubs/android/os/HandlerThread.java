package android.os;

/** No host thread: tests explicitly drain the posted work in FIFO order. */
public final class HandlerThread {
    private final Looper looper = new Looper();
    public HandlerThread(String name) {}
    public void start() {}
    public Looper getLooper() { return looper; }
}
