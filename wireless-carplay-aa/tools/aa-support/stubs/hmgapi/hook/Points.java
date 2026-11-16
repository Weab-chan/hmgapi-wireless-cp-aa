package hmgapi.hook;

public final class Points {
    public static final Object IMAGE = new Object(), ENUM = new Object();
    public static final int FOCUS_NATIVE = 17;
    public static final java.util.Map<Object, Object> receivers = new java.util.IdentityHashMap<Object, Object>();
    public static Object get(ClassLoader cl, String point, Object target) {
        if ("gpm.cluster_type.image".equals(point)) return IMAGE;
        if ("gpm.cluster_type.enum".equals(point)) return ENUM;
        if ("gpm.receiver.focus_native".equals(point)) return FOCUS_NATIVE;
        if ("gpm.navigation_status.receiver".equals(point)) {
            if (!receivers.containsKey(target)) throw new AssertionError("unregistered navigation status");
            return receivers.get(target);
        }
        throw new AssertionError("unexpected field: " + point);
    }
    public static Object call(ClassLoader cl, String point, Object target, Object... args) { return null; }
    public static java.lang.reflect.Field field(ClassLoader cl, String point) { return null; }
}
