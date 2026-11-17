package git.artdeell.mojo.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import git.artdeell.mojo.R;

/**
 * A list of real phones and tablets with the chip (and GPU) they use, plus a list of chips.
 * Used to pick settings for the actual hardware instead of a one-size-fits-all scenario.
 * The data is written from general knowledge and may contain mistakes: if a device is missing
 * or wrong, the user can pick the closest one or one of the generic classes.
 */
public final class DeviceDatabase {
    public static final String FAMILY_ADRENO = "adreno";
    public static final String FAMILY_MALI = "mali";
    public static final String FAMILY_POWERVR = "powervr";
    public static final String FAMILY_OTHER = "other";

    public static final String PREF_SELECTED_DEVICE = "selectedDevice";
    public static final String PREF_SELECTED_SOC = "selectedDeviceSoc";

    public static final class Soc {
        public final String id;
        public final String name;
        public final String family;
        public final String gpu;
        public final int tier;
        final String[] codes;

        Soc(String id, String name, String family, String gpu, int tier, String[] codes) {
            this.id = id;
            this.name = name;
            this.family = family;
            this.gpu = gpu;
            this.tier = tier;
            this.codes = codes;
        }
    }

    public static final class Device {
        public final String name;
        public final int nameRes;
        public final Soc soc;

        Device(String name, int nameRes, Soc soc) {
            this.name = name;
            this.nameRes = nameRes;
            this.soc = soc;
        }

        public String displayName(Context context) {
            return nameRes != 0 ? context.getString(nameRes) : name;
        }

        public String label(Context context) {
            if (soc.id.startsWith("generic")) return displayName(context);
            return displayName(context) + " \u2014 " + soc.name + " (" + soc.gpu + ")";
        }
    }

    // id | name | gpu family | gpu | tier 0..3 | chip codes (prefix match against SOC_MODEL / HARDWARE / BOARD)
    private static final String[] SOCS = {
            "sd8elite|Snapdragon 8 Elite|adreno|Adreno 830|3|SM8750",
            "sd8g3|Snapdragon 8 Gen 3|adreno|Adreno 750|3|SM8650",
            "sd8sg3|Snapdragon 8s Gen 3|adreno|Adreno 735|3|SM8635",
            "sd8g2|Snapdragon 8 Gen 2|adreno|Adreno 740|3|SM8550",
            "sd8pg1|Snapdragon 8+ Gen 1|adreno|Adreno 730|3|SM8475",
            "sd8g1|Snapdragon 8 Gen 1|adreno|Adreno 730|3|SM8450",
            "sd888|Snapdragon 888|adreno|Adreno 660|3|SM8350",
            "sd865|Snapdragon 865/870|adreno|Adreno 650|2|SM8250",
            "sd855|Snapdragon 855|adreno|Adreno 640|2|SM8150",
            "sd845|Snapdragon 845|adreno|Adreno 630|2|SDM845",
            "sd7pg3|Snapdragon 7+ Gen 3|adreno|Adreno 732|3|SM7675",
            "sd7g3|Snapdragon 7 Gen 3|adreno|Adreno 720|2|SM7550",
            "sd7pg2|Snapdragon 7+ Gen 2|adreno|Adreno 725|2|SM7475",
            "sd7sg2|Snapdragon 7s Gen 2|adreno|Adreno 710|2|SM7435",
            "sd7g1|Snapdragon 7 Gen 1|adreno|Adreno 644|2|SM7450",
            "sd778g|Snapdragon 778G|adreno|Adreno 642L|2|SM7325",
            "sd765g|Snapdragon 765G|adreno|Adreno 620|2|SM7250",
            "sd730|Snapdragon 730/732G|adreno|Adreno 618|1|SM7150",
            "sd6g1|Snapdragon 6 Gen 1|adreno|Adreno 710|1|SM6450",
            "sd6g3|Snapdragon 6 Gen 3|adreno|Adreno 710|1|SM6475",
            "sd695|Snapdragon 695|adreno|Adreno 619|1|SM6375",
            "sd680|Snapdragon 680/685|adreno|Adreno 610|1|SM6225",
            "sd662|Snapdragon 662/665|adreno|Adreno 610|1|SM6115,SM6125",
            "sd480|Snapdragon 480|adreno|Adreno 619|1|SM4350",
            "sd4g1|Snapdragon 4 Gen 1/2|adreno|Adreno 613|0|SM4375,SM4450",
            "sd460|Snapdragon 460|adreno|Adreno 610|0|SM4250",
            "d9400|Dimensity 9400|mali|Immortalis-G925|3|MT6991",
            "d9300|Dimensity 9300|mali|Immortalis-G720|3|MT6989",
            "d9200|Dimensity 9200|mali|Immortalis-G715|3|MT6985",
            "d9000|Dimensity 9000|mali|Mali-G710|3|MT6983",
            "d8300|Dimensity 8300|mali|Mali-G615|2|MT6897",
            "d8200|Dimensity 8200|mali|Mali-G610|2|MT6896",
            "d8100|Dimensity 8100/8000|mali|Mali-G610|2|MT6895",
            "d7200|Dimensity 7200|mali|Mali-G610|2|MT6886",
            "d1200|Dimensity 1100/1200|mali|Mali-G77|2|MT6893,MT6891",
            "d1080|Dimensity 900/1080/7050|mali|Mali-G68|2|MT6877",
            "d6100|Dimensity 6100+|mali|Mali-G57|1|MT6835",
            "d720|Dimensity 720|mali|Mali-G57|1|MT6853",
            "d700|Dimensity 700/6020/6080|mali|Mali-G57|1|MT6833",
            "g99|Helio G99|mali|Mali-G57|1|MT6789",
            "g96|Helio G96|mali|Mali-G57|1|MT6781",
            "g95|Helio G90T/G95|mali|Mali-G76|1|MT6785",
            "g85|Helio G80/G85/G88|mali|Mali-G52|1|MT6769,MT6768",
            "g35|Helio G35/P35|powervr|PowerVR GE8320|0|MT6765,MT6762",
            "e2400|Exynos 2400|other|Xclipse 940|3|S5E9945",
            "e2200|Exynos 2200|other|Xclipse 920|3|S5E9925",
            "e2100|Exynos 2100|mali|Mali-G78|3|S5E9840",
            "e990|Exynos 990|mali|Mali-G77|2|S5E9830",
            "e1480|Exynos 1480|other|Xclipse 530|2|S5E8845",
            "e1380|Exynos 1380|mali|Mali-G68|2|S5E8835",
            "e1280|Exynos 1280|mali|Mali-G68|2|S5E8825",
            "e1330|Exynos 1330|mali|Mali-G68|1|S5E8535",
            "zumapro|Google Tensor G4|mali|Mali-G715|3|ZUMAPRO",
            "zuma|Google Tensor G3|mali|Immortalis-G715|3|ZUMA",
            "gs201|Google Tensor G2|mali|Mali-G710|3|GS201",
            "gs101|Google Tensor|mali|Mali-G78|2|GS101",
            "t618|Unisoc T606/T612/T616/T618|mali|Mali-G52/G57|0|UMS9230,T606,T612,T616,T618,UMS512",
            "generic0|Entry-level|other|unknown|0|",
            "generic1|Mid-range|other|unknown|1|",
            "generic2|Upper mid-range|other|unknown|2|",
            "generic3|Flagship|other|unknown|3|"
    };

    // name | chip id
    private static final String[] DEVICES = {
            "Google Pixel 6|gs101", "Google Pixel 6 Pro|gs101", "Google Pixel 6a|gs101",
            "Google Pixel 7|gs201", "Google Pixel 7 Pro|gs201", "Google Pixel 7a|gs201", "Google Pixel Tablet|gs201",
            "Google Pixel 8|zuma", "Google Pixel 8 Pro|zuma", "Google Pixel 8a|zuma",
            "Google Pixel 9|zumapro", "Google Pixel 9 Pro|zumapro", "Google Pixel 9 Pro XL|zumapro",
            "Samsung Galaxy S25 Ultra|sd8elite", "Samsung Galaxy S24 Ultra|sd8g3",
            "Samsung Galaxy S24 / S24+ (Exynos 2400)|e2400", "Samsung Galaxy S23 / S23+ / S23 Ultra|sd8g2",
            "Samsung Galaxy S22 series (Snapdragon)|sd8g1", "Samsung Galaxy S22 / S22+ (Exynos 2200)|e2200",
            "Samsung Galaxy S21 (Exynos 2100)|e2100", "Samsung Galaxy S20 FE 5G|sd865",
            "Samsung Galaxy A55 5G|e1480", "Samsung Galaxy A54 5G|e1380", "Samsung Galaxy A53 5G|e1280",
            "Samsung Galaxy A34 5G|d1080", "Samsung Galaxy A25 5G|e1280", "Samsung Galaxy M34 5G|e1280",
            "Samsung Galaxy A15|g99", "Samsung Galaxy A14|g85", "Samsung Galaxy A04|g35",
            "Samsung Galaxy Tab S9 series|sd8g2", "Samsung Galaxy Tab S8 series|sd8g1", "Samsung Galaxy Tab S7 / S7+|sd865",
            "Samsung Galaxy Tab S6 Lite (2024)|e1280", "Samsung Galaxy Tab A9+|sd695", "Samsung Galaxy Tab A8|t618",
            "Xiaomi 14 / 14 Ultra|sd8g3", "Xiaomi 13|sd8g2", "Xiaomi 13T|d8200", "Xiaomi 12|sd8g1", "Xiaomi 11T|d1200",
            "Xiaomi 11 Lite 5G NE|sd778g", "Xiaomi Mi 11|sd888", "Xiaomi Pad 6|sd865", "Xiaomi Pad 6 Pro|sd8pg1",
            "Redmi Note 13 (4G)|sd680", "Redmi Note 13 (5G)|d700", "Redmi Note 13 Pro (4G)|g99", "Redmi Note 13 Pro+ 5G|d7200",
            "Redmi Note 12 (4G)|sd680", "Redmi Note 12 Pro / Pro+ 5G|d1080", "Redmi Note 11|sd680",
            "Redmi 13C|g85", "Redmi 12|g85", "Redmi 12C|g85", "Redmi 10|g85", "Redmi Pad|g99", "Redmi Pad SE|sd680",
            "POCO X6 Pro|d8300", "POCO X5 Pro 5G|sd778g", "POCO F6 Pro|sd8g2", "POCO F5 Pro|sd8pg1", "POCO F5|sd7pg2",
            "POCO F4 / F3|sd865", "POCO M5|g99", "POCO C65|g85",
            "OnePlus 12|sd8g3", "OnePlus 11|sd8g2", "OnePlus 10 Pro|sd8g1", "OnePlus 9 Pro|sd888", "OnePlus 8T|sd865",
            "OnePlus Nord 3 5G|d9000", "OnePlus Nord CE 3 Lite|sd695", "OnePlus Nord CE 4|sd7g3", "OnePlus Pad|d9000",
            "Realme GT 5 Pro|sd8g3", "Realme 11 Pro|d1080", "Realme C55|g85", "Realme 9 Pro|sd695", "Realme Narzo 60 5G|d700",
            "OPPO Find X7|d9300", "OPPO Reno 11|d7200", "OPPO A78 5G|d700", "OPPO Pad Air|sd680",
            "vivo X100|d9300", "vivo iQOO 12|sd8g3", "vivo V29|sd778g", "vivo Y36|sd680",
            "Motorola Razr 40 Ultra|sd8pg1", "Motorola Moto G84 5G|sd695", "Motorola Moto G Stylus 5G (2023)|sd695",
            "Motorola Moto G Power 5G (2023)|d700",
            "Nothing Phone (1)|sd778g", "Nothing Phone (2)|sd8pg1", "Nothing Phone (2a)|d7200",
            "Honor Magic5 Pro|sd8g2", "Honor 90|sd7g1", "Honor X8a|g85", "Honor Pad X9|sd680",
            "ASUS ROG Phone 8 / 8 Pro|sd8g3", "ASUS ROG Phone 7|sd8g2", "ASUS ROG Phone 6|sd8pg1", "ASUS Zenfone 10|sd8g2",
            "Lenovo Tab P11 (2nd Gen)|g99", "Lenovo Legion Y700 (2022)|sd8pg1", "Lenovo Tab M10 Plus (3rd Gen)|g85",
            "Sony Xperia 1 V|sd8g2", "Sony Xperia 5 IV|sd8g1", "Sony Xperia 10 V|sd695"
    };

    private static final Map<String, Soc> SOC_MAP = new LinkedHashMap<>();
    private static final List<Device> DEVICE_LIST = new ArrayList<>();
    private static Soc sDetected;

    static {
        for (String line : SOCS) {
            String[] p = line.split("\\|", -1);
            String[] codes = p[5].isEmpty() ? new String[0] : p[5].split(",");
            SOC_MAP.put(p[0], new Soc(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]), codes));
        }
        for (String line : DEVICES) {
            String[] p = line.split("\\|");
            Soc soc = SOC_MAP.get(p[1]);
            if (soc != null) DEVICE_LIST.add(new Device(p[0], 0, soc));
        }
        int[] generic = {R.string.device_generic_0, R.string.device_generic_1, R.string.device_generic_2, R.string.device_generic_3};
        for (int i = 0; i < generic.length; i++) {
            DEVICE_LIST.add(new Device(null, generic[i], SOC_MAP.get("generic" + i)));
        }
    }

    private DeviceDatabase() {}

    public static List<Device> allDevices() {
        return DEVICE_LIST;
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /** Chip of the real device, found by its codes, or guessed from the GPU name */
    public static synchronized Soc detect() {
        if (sDetected != null) return sDetected;
        List<String> candidates = new ArrayList<>();
        if (SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null) {
            candidates.add(Build.SOC_MODEL.toUpperCase(Locale.ROOT));
        }
        if (Build.HARDWARE != null) candidates.add(Build.HARDWARE.toUpperCase(Locale.ROOT));
        if (Build.BOARD != null) candidates.add(Build.BOARD.toUpperCase(Locale.ROOT));
        for (Soc soc : SOC_MAP.values()) {
            for (String code : soc.codes) {
                for (String candidate : candidates) {
                    if (candidate.startsWith(code)) return sDetected = soc;
                }
            }
        }
        String renderer = null;
        try {
            renderer = GpuUtils.getGlInfo().renderer;
        } catch (Throwable ignored) {
        }
        return sDetected = fromGpu(renderer);
    }

    private static Soc fromGpu(String renderer) {
        String r = renderer == null ? "" : renderer;
        Matcher m = Pattern.compile("(?i)adreno[^0-9]*(\\d+)").matcher(r);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            int tier = n >= 730 ? 3 : n >= 640 ? 2 : n >= 610 ? 1 : 0;
            return new Soc("auto", "Adreno " + n, FAMILY_ADRENO, "Adreno " + n, tier, new String[0]);
        }
        m = Pattern.compile("(?i)immortalis[^0-9]*g?(\\d+)").matcher(r);
        if (m.find()) {
            return new Soc("auto", "Immortalis-G" + m.group(1), FAMILY_MALI, "Immortalis-G" + m.group(1), 3, new String[0]);
        }
        m = Pattern.compile("(?i)mali[^0-9]*g?(\\d+)").matcher(r);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            int tier = n >= 100 ? (n >= 710 ? 3 : 2) : (n >= 68 ? 2 : n >= 52 ? 1 : 0);
            return new Soc("auto", "Mali-G" + n, FAMILY_MALI, "Mali-G" + n, tier, new String[0]);
        }
        if (r.toLowerCase(Locale.ROOT).contains("powervr")) {
            return new Soc("auto", "PowerVR", FAMILY_POWERVR, "PowerVR", 0, new String[0]);
        }
        String gpu = r.isEmpty() ? "unknown" : r;
        return new Soc("auto", gpu, FAMILY_OTHER, gpu, 1, new String[0]);
    }

    /** The chip chosen by the user in the device list, or the detected one */
    public static Soc current(Context context) {
        String id = prefs(context).getString(PREF_SELECTED_SOC, null);
        if (id != null) {
            Soc soc = SOC_MAP.get(id);
            if (soc != null) return soc;
        }
        return detect();
    }

    public static String currentName(Context context) {
        String name = prefs(context).getString(PREF_SELECTED_DEVICE, null);
        return name != null ? name : current(context).name;
    }

    public static String summary(Context context) {
        Soc soc = current(context);
        return context.getString(R.string.device_current_summary, currentName(context), soc.gpu);
    }

    public static void select(Context context, Device device) {
        prefs(context).edit()
                .putString(PREF_SELECTED_DEVICE, device.displayName(context))
                .putString(PREF_SELECTED_SOC, device.soc.id)
                .commit();
    }
}
