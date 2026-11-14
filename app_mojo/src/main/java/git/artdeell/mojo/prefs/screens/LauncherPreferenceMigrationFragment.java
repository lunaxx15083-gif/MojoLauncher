package git.artdeell.mojo.prefs.screens;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import git.artdeell.mojo.tasks.DataMigrator;
import git.artdeell.mojo.tasks.FolderImporter;

/** Transfer of worlds, mods and settings from the game folder of another launcher */
public class LauncherPreferenceMigrationFragment extends LauncherPreferenceFragment {
    public static final String TAG = "MigrationFragment";

    private final ActivityResultLauncher<Uri> mFolderLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) confirmAndStart(uri);
            });

    private final ActivityResultLauncher<Uri> mFullMojoLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri == null) return;
                new AlertDialog.Builder(getLauncherActivity())
                        .setTitle(R.string.migration_progress_warning_title)
                        .setMessage(R.string.migration_progress_warning_summary)
                        .setPositiveButton(android.R.string.ok, (d, w) -> new DataMigrator(getLauncherActivity(), uri).migrateData())
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_migration);

        requirePreference("migrationStart").setOnPreferenceClickListener(preference -> {
            if (!ProgressLayout.ensureNoOngoingTasks(getContext())) return true;
            if (collectOptions() == 0) {
                Toast.makeText(requireContext(), R.string.migration_nothing_selected, Toast.LENGTH_LONG).show();
                return true;
            }
            mFolderLauncher.launch(null);
            return true;
        });

        requirePreference("migrationFullMojo").setOnPreferenceClickListener(preference -> {
            if (!ProgressLayout.ensureNoOngoingTasks(getContext())) return true;
            mFullMojoLauncher.launch(null);
            return true;
        });
    }

    private int collectOptions() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int options = 0;
        if (prefs.getBoolean("migrate_worlds", false)) options |= FolderImporter.OPT_WORLDS;
        if (prefs.getBoolean("migrate_mods", true)) options |= FolderImporter.OPT_MODS;
        if (prefs.getBoolean("migrate_resourcepacks", true)) options |= FolderImporter.OPT_RESOURCE_PACKS;
        if (prefs.getBoolean("migrate_shaders", true)) options |= FolderImporter.OPT_SHADERS;
        if (prefs.getBoolean("migrate_screenshots", false)) options |= FolderImporter.OPT_SCREENSHOTS;
        if (prefs.getBoolean("migrate_settings", true)) options |= FolderImporter.OPT_SETTINGS;
        if (prefs.getBoolean("migrate_versions", true)) options |= FolderImporter.OPT_VERSIONS;
        return options;
    }

    private void confirmAndStart(Uri treeUri) {
        final int options = collectOptions();
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.migration_confirm_title)
                .setMessage(R.string.migration_confirm_message)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        new FolderImporter(requireActivity(), treeUri, options).start())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
