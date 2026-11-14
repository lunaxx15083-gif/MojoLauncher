package git.artdeell.mojo.utils;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import git.artdeell.mojo.R;

/** The short tour of the launcher: shown on request and offered once on the first launch */
public final class HelpGuide {
    private static final String PREF_OFFER_SHOWN = "helpOfferShown";

    private HelpGuide() {}

    public static void showGuide(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.help_guide_title)
                .setMessage(R.string.help_guide_text)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Asks once whether the user wants a tour. Later launches do nothing. */
    public static void maybeOfferOnFirstRun(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(PREF_OFFER_SHOWN, false)) return;
        prefs.edit().putBoolean(PREF_OFFER_SHOWN, true).apply();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.help_offer_title)
                .setMessage(R.string.help_offer_message)
                .setPositiveButton(R.string.help_offer_yes, (dialog, which) -> showGuide(activity))
                .setNegativeButton(R.string.help_offer_no, null)
                .show();
    }
}
