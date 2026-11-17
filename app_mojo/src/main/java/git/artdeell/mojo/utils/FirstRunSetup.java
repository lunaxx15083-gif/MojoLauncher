package git.artdeell.mojo.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/**
 * Shown once on the very first launch: language, accent color, icon style and a performance preset
 * in one place, so the launcher can look and work the way the user wants from the start.
 */
public final class FirstRunSetup {
    private static final String PREF_SETUP_DONE = "setupDone";

    private FirstRunSetup() {}

    /** Shows the setup on the first launch; afterwards only the one-time offer of the tour runs */
    public static void maybeShow(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(PREF_SETUP_DONE, false)) {
            HelpGuide.maybeOfferOnFirstRun(activity);
            return;
        }
        prefs.edit().putBoolean(PREF_SETUP_DONE, true).apply();
        show(activity, prefs);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static Spinner spinner(Activity activity, int arrayRes) {
        Spinner spinner = new Spinner(activity);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private static void addRow(LinearLayout root, Activity activity, int labelRes, Spinner spinner) {
        TextView label = new TextView(activity);
        label.setText(labelRes);
        label.setPadding(0, dp(activity, 12), 0, dp(activity, 2));
        root.addView(label);
        root.addView(spinner);
    }

    private static void show(final Activity activity, final SharedPreferences prefs) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), dp(activity, 8));

        TextView message = new TextView(activity);
        message.setText(R.string.setup_message);
        root.addView(message);

        DeviceDatabase.Soc detected = DeviceDatabase.detect();
        TextView detectedView = new TextView(activity);
        detectedView.setText(activity.getString(R.string.device_detected, detected.name, detected.gpu));
        detectedView.setPadding(0, dp(activity, 10), 0, 0);
        root.addView(detectedView);

        Button myDevice = new Button(activity);
        myDevice.setText(R.string.setup_my_device);
        myDevice.setOnClickListener(v -> DevicePicker.show(activity, () -> {
            DeviceDatabase.Soc chosen = DeviceDatabase.current(activity);
            detectedView.setText(activity.getString(R.string.device_detected, DeviceDatabase.currentName(activity), chosen.gpu));
        }));
        root.addView(myDevice, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Spinner language = spinner(activity, R.array.language_names);
        final Spinner accent = spinner(activity, R.array.accent_color_names);
        final Spinner icons = spinner(activity, R.array.icon_style_names);
        final Spinner preset = spinner(activity, R.array.preset_names);
        addRow(root, activity, R.string.preference_language_title, language);
        addRow(root, activity, R.string.preference_accent_color_title, accent);
        addRow(root, activity, R.string.preference_icon_style_title, icons);
        addRow(root, activity, R.string.setup_label_preset, preset);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(root);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.setup_title)
                .setView(scroll)
                .setCancelable(false)
                .setPositiveButton(R.string.setup_apply, (dialog, which) -> {
                    String lang = activity.getResources().getStringArray(R.array.language_values)[language.getSelectedItemPosition()];
                    String color = activity.getResources().getStringArray(R.array.accent_color_values)[accent.getSelectedItemPosition()];
                    String iconStyle = activity.getResources().getStringArray(R.array.icon_style_values)[icons.getSelectedItemPosition()];
                    String presetId = activity.getResources().getStringArray(R.array.preset_values)[preset.getSelectedItemPosition()];

                    boolean changed = !lang.equals(prefs.getString(LocaleUtils.PREF_APP_LANGUAGE, "system"))
                            || !color.equals(prefs.getString(AccentColor.PREF_ACCENT, "green"))
                            || !iconStyle.equals(prefs.getString(AccentColor.PREF_ICON_STYLE, "white"));

                    prefs.edit()
                            .putString(LocaleUtils.PREF_APP_LANGUAGE, lang)
                            .putString(AccentColor.PREF_ACCENT, color)
                            .putString(AccentColor.PREF_ICON_STYLE, iconStyle)
                            .commit();
                    DeviceProfile.applyPreset(activity, presetId);
                    LauncherPreferences.loadPreferences(activity);

                    if (changed) {
                        // The new language and colors are read when the activity is created
                        activity.recreate();
                    } else {
                        HelpGuide.maybeOfferOnFirstRun(activity);
                    }
                })
                .setNegativeButton(R.string.setup_skip, (dialog, which) -> HelpGuide.maybeOfferOnFirstRun(activity))
                .show();
    }
}
