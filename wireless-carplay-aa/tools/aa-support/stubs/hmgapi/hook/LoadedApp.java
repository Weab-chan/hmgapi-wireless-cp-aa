package hmgapi.hook;

public final class LoadedApp {
    public final String packageName, processName;
    public final ClassLoader classLoader = LoadedApp.class.getClassLoader();
    public final android.content.Context context = null;
    public LoadedApp(String name) { packageName = name; processName = name; }
}
