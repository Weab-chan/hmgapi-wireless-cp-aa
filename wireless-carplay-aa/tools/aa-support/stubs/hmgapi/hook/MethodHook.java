package hmgapi.hook;

public abstract class MethodHook {
    protected void beforeHookedMethod(Param p) throws Throwable {}
    protected void afterHookedMethod(Param p) throws Throwable {}
    public final boolean skipsOriginal() throws Throwable {
        Param p = new Param();
        beforeHookedMethod(p);
        return p.returnEarly;
    }
    public final Param before(Object target, Object... args) throws Throwable {
        Param p = param(target, args);
        beforeHookedMethod(p);
        return p;
    }
    public final void after(Object target, Object... args) throws Throwable {
        afterHookedMethod(param(target, args));
    }
    private static Param param(Object target, Object[] args) {
        Param p = new Param();
        p.thisObject = target;
        p.args = args;
        return p;
    }
    public static final class Param {
        public Object thisObject;
        public Object[] args = new Object[0];
        private boolean returnEarly;
        public void setResult(Object result) { returnEarly = true; }
    }
}
