package hmgapi.hook;

import android.content.Context;

public final class Car {
    public static final int REGION_US = 1, REGION_CANADA = 4;
    public static int step = 105;
    public static boolean aaGuiding, cpGuiding;
    public static int stepOfSpec() { return step; }
    public static int region() { return REGION_CANADA; }
    public static boolean androidAutoGuiding(Context c) { return aaGuiding; }
    public static boolean carPlayGuiding(Context c) { return cpGuiding; }
}
