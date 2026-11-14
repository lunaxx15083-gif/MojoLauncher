package git.artdeell.mojo.prefs.screens;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.utils.HelpGuide;

/** Guide, links and tools that used to live on the main screen */
public class LauncherPreferenceHelpFragment extends LauncherPreferenceFragment {
    public static final String TAG = "HelpFragment";

    private final ActivityResultLauncher<Object> mJarLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), data -> {
                if (data != null) Tools.launchModInstaller(requireContext(), data);
            });

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_help);

        requirePreference("helpGuide").setOnPreferenceClickListener(preference -> {
            HelpGuide.showGuide(requireActivity());
            return true;
        });
        requirePreference("helpWiki").setOnPreferenceClickListener(preference -> {
            Tools.openURL(requireActivity(), Tools.URL_HOME);
            return true;
        });
        requirePreference("helpSocial").setOnPreferenceClickListener(preference -> {
            Tools.openURL(requireActivity(), getString(R.string.social_media_invite));
            return true;
        });
        requirePreference("helpShareLogs").setOnPreferenceClickListener(preference -> {
            Tools.shareLog(requireContext());
            return true;
        });
        requirePreference("helpInstallJar").setOnPreferenceClickListener(preference -> {
            if (!ProgressLayout.ensureNoOngoingTasks(requireContext())) return true;
            mJarLauncher.launch(null);
            return true;
        });
    }
}
