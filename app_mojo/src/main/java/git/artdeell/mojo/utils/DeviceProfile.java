package git.artdeell.mojo.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

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

    // Target length of the short screen side in pixels, per chip tier 0..3
    private static final int[] SIDE_PERFORMANCE = {480, 576, 720, 900};
    private static final int[] SIDE_BALANCED = {540, 720, 900, 1080};
    private static final int[] SIDE_QUALITY = {720, 900, 1080, 1440};
    private static final int[] SIDE_BATTERY = {432, 480, 540, 720};

    private static int resolutionRatio(Context context, int targetSide) {
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int minSide = Math.min(metrics.widthPixels, metrics.heightPixels);
        if (minSide <= targetSide) return 100;
        int increment = context.getResources().getInteger(R.integer.resolution_seekbar_increment);
        int minimum = context.getResources().getInteger(R.integer.resolution_seekbar_min);
        float ratio = 100f * targetSide / minSide;
        int value = Math.round(ratio / increment) * increment;
        return Math.max(minimum, Math.min(100, value));
    }

    /**
     * Applies a named preset (auto, performance, balanced, quality, battery, compat)
     * using the chip tier of the detected or chosen device.
     */
    public static void applyPreset(Context context, String preset) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ("auto".equals(preset)) {
            restoreOptimalSettings(prefs);
            return;
        }
        DeviceDatabase.Soc soc = DeviceDatabase.current(context);
        int tier = Math.max(0, Math.min(3, soc.tier));
        boolean pinBigCores = DeviceDatabase.FAMILY_MALI.equals(soc.family) && hasMixedCoreFrequencies();

        int side;
        boolean sustained;
        boolean vsync;
        boolean affinity;
        boolean alternateSurface = tier >= 2;
        switch (preset) {
            case "performance":
                side = SIDE_PERFORMANCE[tier]; sustained = false; vsync = false; affinity = pinBigCores;
                break;
            case "quality":
                side = SIDE_QUALITY[tier]; sustained = true; vsync = true; affinity = pinBigCores;
                break;
            case "battery":
                side = SIDE_BATTERY[tier]; sustained = true; vsync = true; affinity = false;
                break;
            case "compat":
                side = SIDE_BALANCED[tier]; sustained = false; vsync = false; affinity = false; alternateSurface = false;
                break;
            default: // balanced
                side = SIDE_BALANCED[tier]; sustained = true; vsync = tier >= 2; affinity = pinBigCores;
                break;
        }
        prefs.edit()
                .putInt("resolutionRatio", resolutionRatio(context, side))
                .putBoolean("sustainedPerformance", sustained)
                .putBoolean("force_vsync", vsync)
                .putBoolean("bigCoreAffinity", affinity)
                .putBoolean("alternate_surface", alternateSurface)
                .commit();
    }

    /** Remembers the device picked from the list and applies balanced settings for it */
    public static void selectDevice(Context context, DeviceDatabase.Device device) {
        DeviceDatabase.select(context, device);
        applyPreset(context, "balanced");
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
