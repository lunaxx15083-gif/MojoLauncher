package git.artdeell.mojo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import git.artdeell.mojo.R;

/**
 * User selectable accent color and icon style (Customization settings screen).
 * The accent is applied through a theme overlay in BaseActivity and through
 * {@link #buttonBackground(Context)} for the green "Play"-style buttons.
 */
public final class AccentColor {
    public static final String PREF_ACCENT = "accentColor";
    public static final String PREF_ICON_STYLE = "iconStyle";

    private static final String DEFAULT_ACCENT = "green";
    private static final String DEFAULT_ICON_STYLE = "white";

    private AccentColor() {}

    private static String readString(Context context, String key, String fallback) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String value = prefs.getString(key, fallback);
            return value == null ? fallback : value;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static String accentName(Context context) {
        return readString(context, PREF_ACCENT, DEFAULT_ACCENT);
    }

    /** Style that overrides colorAccent, applied on top of the activity theme */
    public static int overlayStyle(Context context) {
        switch (accentName(context)) {
            case "blue": return R.style.AccentOverlay_Blue;
            case "red": return R.style.AccentOverlay_Red;
            case "purple": return R.style.AccentOverlay_Purple;
            case "orange": return R.style.AccentOverlay_Orange;
            case "pink": return R.style.AccentOverlay_Pink;
            case "teal": return R.style.AccentOverlay_Teal;
            default: return R.style.AccentOverlay_Green;
        }
    }

    public static int accentColor(Context context) {
        int colorRes;
        switch (accentName(context)) {
            case "blue": colorRes = R.color.accent_blue; break;
            case "red": colorRes = R.color.accent_red; break;
            case "purple": colorRes = R.color.accent_purple; break;
            case "orange": colorRes = R.color.accent_orange; break;
            case "pink": colorRes = R.color.accent_pink; break;
            case "teal": colorRes = R.color.accent_teal; break;
            default: colorRes = R.color.accent_green; break;
        }
        return ContextCompat.getColor(context, colorRes);
    }

    /** Rounded accent-colored button background with a ripple and a muted disabled state */
    public static Drawable buttonBackground(Context context) {
        float radius = 14f * context.getResources().getDisplayMetrics().density;
        int accent = accentColor(context);
        int disabledColor = ColorUtils.blendARGB(accent, 0xFF121518, 0.7f);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setCornerRadius(radius);
        normal.setColor(accent);

        GradientDrawable disabled = new GradientDrawable();
        disabled.setShape(GradientDrawable.RECTANGLE);
        disabled.setCornerRadius(radius);
        disabled.setColor(disabledColor);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
        states.addState(new int[]{}, normal);
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), states, null);
    }

    /** @return true if the icons should be recolored (anything but the default white) */
    public static boolean hasIconTint(Context context) {
        return !DEFAULT_ICON_STYLE.equals(readString(context, PREF_ICON_STYLE, DEFAULT_ICON_STYLE));
    }

    public static int iconTintColor(Context context) {
        switch (readString(context, PREF_ICON_STYLE, DEFAULT_ICON_STYLE)) {
            case "accent": return accentColor(context);
            case "gray": return ContextCompat.getColor(context, R.color.icon_gray);
            default: return Color.WHITE;
        }
    }

    /** Recolors the icons of every preference in the group (recursively) */
    public static void tintPreferenceIcons(Context context, PreferenceGroup group) {
        if (group == null || !hasIconTint(context)) return;
        int color = iconTintColor(context);
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            Drawable icon = preference.getIcon();
            if (icon != null) {
                Drawable tinted = icon.mutate();
                tinted.setTint(color);
                preference.setIcon(tinted);
            }
            if (preference instanceof PreferenceGroup) {
                tintPreferenceIcons(context, (PreferenceGroup) preference);
            }
        }
    }
}
