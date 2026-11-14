package git.artdeell.mojo.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;

/**
 * Describes the device (SoC, GPU family, CPU layout) and picks first-run defaults
 * for devices that are not Adreno based. Defaults are written only once per profile
 * version and never overwrite a value the user has already set.
 */
public final class DeviceProfile {
    private static final String TAG = "DeviceProfile";
    private static final String KEY_PROFILE_VERSION = "deviceProfileVersion";
    private static final int PROFILE_VERSION = 1;

    public enum GpuFamily { ADRENO, MALI, OTHER }

    private DeviceProfile() {}

    public static GpuFamily gpuFamily() {
        GpuUtils.GLInfo info = GpuUtils.getGlInfo();
        if (info.isAdreno()) return GpuFamily.ADRENO;
        if (info.isArm()) return GpuFamily.MALI;
        return GpuFamily.OTHER;
    }

    public static String socName() {
        if (SDK_INT >= Build.VERSION_CODES.S) {
            String model = Build.SOC_MODEL;
            if (model != null && !model.isEmpty() && !"unknown".equalsIgnoreCase(model)) {
                String maker = Build.SOC_MANUFACTURER;
                if (maker != null && !maker.isEmpty() && !"unknown".equalsIgnoreCase(maker)) {
                    return maker + " " + model;
                }
                return model;
            }
        }
        return Build.HARDWARE == null ? "unknown" : Build.HARDWARE;
    }

    /** Human readable summary shown in the experimental settings screen */
    public static String describe(Context context) {
        GpuUtils.GLInfo info = GpuUtils.getGlInfo();
        return context.getString(R.string.preference_device_info_summary,
                socName(),
                info.renderer,
                info.glesMajorVersion,
                Runtime.getRuntime().availableProcessors(),
                Tools.getTotalDeviceMemory(context));
    }

    /**
     * Cores of a phone SoC usually run at different top frequencies (little, middle, big).
     * Compares the first and the last core, the same way the launcher does elsewhere.
     */
    private static boolean hasMixedCoreFrequencies() {
        int coreCount = Runtime.getRuntime().availableProcessors();
        if (coreCount < 6) return false;
        try {
            String first = Tools.read("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").trim();
            String last = Tools.read("/sys/devices/system/cpu/cpu" + (coreCount - 1) + "/cpufreq/cpuinfo_max_freq").trim();
            return !first.equals(last);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Could not read CPU frequencies", e);
            return false;
        }
    }

    /** Pinning the render thread to the fastest core is tried by default on Mali/Immortalis with mixed cores */
    private static boolean wantsBigCoreAffinity() {
        return gpuFamily() == GpuFamily.MALI && hasMixedCoreFrequencies();
    }

    /** Graphics and performance options that are reset by "Restore optimal settings" */
    private static final String[] OPTIMAL_KEYS = {
            "renderer", "resolutionRatio", "sustainedPerformance", "alternate_surface", "force_vsync",
            "use_angle", "zinkPreferSystemDriver", "bigCoreAffinity", "freedrenoSysmem", "ubwcWorkaround", "allocation"
    };

    /**
     * Forgets the user's graphics/performance choices so that the launcher falls back to the values
     * it picks for this device, then applies the device profile again.
     */
    public static void restoreOptimalSettings(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : OPTIMAL_KEYS) editor.remove(key);
        editor.remove(KEY_PROFILE_VERSION);
        editor.commit();
        applyFirstRunDefaults(prefs);
    }

    public static void applyFirstRunDefaults(SharedPreferences prefs) {
        if (prefs.getInt(KEY_PROFILE_VERSION, 0) >= PROFILE_VERSION) return;
        SharedPreferences.Editor editor = prefs.edit();
        try {
            if (!prefs.contains("bigCoreAffinity") && wantsBigCoreAffinity()) {
                editor.putBoolean("bigCoreAffinity", true);
                Log.i(TAG, "Enabled big core affinity by default for " + socName());
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to pick device defaults", t);
        }
        editor.putInt(KEY_PROFILE_VERSION, PROFILE_VERSION);
        editor.apply();
    }
}
