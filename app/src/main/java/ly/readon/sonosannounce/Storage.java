package ly.readon.sonosannounce;


import android.content.Context;
import android.content.SharedPreferences;


/**
 * @author : Bernd Verst(@berndverst)
 */
public class Storage {
    private Storage() { }

    static class StringUtil {
        static boolean isEmpty(String str) {
            return str == null || str.length() == 0;
        }
    }

    private static SharedPreferences getSharedPreference(Context ctx) {
        return ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    public static boolean putBoolean(Context ctx, String key, boolean value) {
        SharedPreferences prefs = getSharedPreference(ctx);
        return prefs != null && prefs.edit().putBoolean(key, value).commit();
    }

    public static boolean getBoolean(Context ctx, String key) {
        return !(ctx == null || StringUtil.isEmpty(key)) && getSharedPreference(ctx).getBoolean(key, false);
    }

    public static boolean putLong(Context ctx, String key, long value) {
        SharedPreferences prefs = getSharedPreference(ctx);
        return prefs != null && prefs.edit().putLong(key, value).commit();
    }

    public static long getLong(Context ctx, String key) {
        if (ctx == null || StringUtil.isEmpty(key)) {
            return 0L;
        }
        return getSharedPreference(ctx).getLong(key, 0L);
    }
}
