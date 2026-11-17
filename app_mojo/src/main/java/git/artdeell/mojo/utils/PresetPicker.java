package git.artdeell.mojo.utils;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/** The "Presets" dialog: ready-made graphics and performance setups */
public final class PresetPicker {
    private PresetPicker() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        final String[] names = activity.getResources().getStringArray(R.array.preset_names);
        final String[] ids = activity.getResources().getStringArray(R.array.preset_values);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.preset_dialog_title)
                .setItems(names, (dialog, which) -> {
                    DeviceProfile.applyPreset(activity, ids[which]);
                    LauncherPreferences.loadPreferences(activity);
                    String shortName = names[which];
                    int colon = shortName.indexOf(':');
                    if (colon < 0) colon = shortName.indexOf('\uFF1A');
                    if (colon > 0) shortName = shortName.substring(0, colon);
                    Toast.makeText(activity, activity.getString(R.string.preset_applied, shortName), Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.preference_select_device_title, (dialog, which) -> DevicePicker.show(activity, null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
