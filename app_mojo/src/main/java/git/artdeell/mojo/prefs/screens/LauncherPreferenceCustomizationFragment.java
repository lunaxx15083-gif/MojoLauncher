package git.artdeell.mojo.prefs.screens;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.Preference;

import git.artdeell.mojo.R;
import git.artdeell.mojo.utils.AccentColor;
import git.artdeell.mojo.utils.LocaleUtils;

/** Appearance settings: language, accent color and icon style */
public class LauncherPreferenceCustomizationFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_customization);

        // The new look is read when an activity is created, so recreate it right after the value is saved
        Preference.OnPreferenceChangeListener applyChange = (preference, newValue) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) activity.recreate();
            });
            return true;
        };
        requirePreference(LocaleUtils.PREF_APP_LANGUAGE).setOnPreferenceChangeListener(applyChange);
        requirePreference(AccentColor.PREF_ACCENT).setOnPreferenceChangeListener(applyChange);
        requirePreference(AccentColor.PREF_ICON_STYLE).setOnPreferenceChangeListener(applyChange);
    }
}
