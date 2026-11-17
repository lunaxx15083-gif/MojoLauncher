package git.artdeell.mojo.utils;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import git.artdeell.mojo.R;
import git.artdeell.mojo.prefs.LauncherPreferences;

/** A searchable list of real phones and tablets. Picking one applies settings made for its chip. */
public final class DevicePicker {
    private DevicePicker() {}

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    /** @param onApplied called after settings were applied, may be null */
    public static void show(final Activity activity, final Runnable onApplied) {
        if (activity == null || activity.isFinishing()) return;

        final List<DeviceDatabase.Device> all = DeviceDatabase.allDevices();
        final List<DeviceDatabase.Device> shown = new ArrayList<>(all);
        final List<String> labels = new ArrayList<>();
        for (DeviceDatabase.Device device : shown) labels.add(device.label(activity));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), 0);

        DeviceDatabase.Soc detected = DeviceDatabase.detect();
        TextView detectedView = new TextView(activity);
        detectedView.setText(activity.getString(R.string.device_detected, detected.name, detected.gpu));
        root.addView(detectedView);

        EditText search = new EditText(activity);
        search.setHint(R.string.device_search_hint);
        search.setSingleLine(true);
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, labels);
        ListView list = new ListView(activity);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 320)));

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.device_picker_title)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                adapter.clear();
                for (DeviceDatabase.Device device : all) {
                    String label = device.label(activity);
                    if (query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query)) {
                        shown.add(device);
                        adapter.add(label);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= shown.size()) return;
            DeviceDatabase.Device device = shown.get(position);
            DeviceProfile.selectDevice(activity, device);
            LauncherPreferences.loadPreferences(activity);
            Toast.makeText(activity, activity.getString(R.string.device_applied,
                    device.displayName(activity), device.soc.gpu), Toast.LENGTH_LONG).show();
            dialog.dismiss();
            if (onApplied != null) onApplied.run();
        });

        dialog.show();
    }
}
