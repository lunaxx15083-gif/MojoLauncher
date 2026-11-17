package git.artdeell.mojo.tasks;

import static git.artdeell.mojo.MojoApplication.sExecutorService;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.utils.FileUtils;

/**
 * Imports data (worlds, mods, resource packs, settings, versions) from the game folder of any
 * other launcher (Pojav, Mojo, ...) that the user picked with the system folder picker.
 * Existing files are never overwritten. Everything goes into the currently selected profile.
 */
public class FolderImporter {
    public static final int OPT_WORLDS = 1;
    public static final int OPT_MODS = 1 << 1;
    public static final int OPT_RESOURCE_PACKS = 1 << 2;
    public static final int OPT_SHADERS = 1 << 3;
    public static final int OPT_SCREENSHOTS = 1 << 4;
    public static final int OPT_SETTINGS = 1 << 5;
    public static final int OPT_VERSIONS = 1 << 6;
    public static final int OPT_SERVERS = 1 << 7;
    public static final int OPT_CONFIG = 1 << 8;
    public static final int OPT_MAPS = 1 << 9;
    public static final int OPT_LOGS = 1 << 10;

    private static final String TAG = "FolderImporter";
    private static final String[] PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
    };
    private static final String[] GAME_DIR_MARKERS = {
            "saves", "mods", "resourcepacks", "shaderpacks", "options.txt", "versions", "screenshots", "servers.dat"
    };
    private static final String[] SETTINGS_FILES = {
            "options.txt", "optionsof.txt", "optionsshaders.txt"
    };
    private static final String[] MAP_FOLDERS = {
            "journeymap", "XaeroWorldMap", "XaeroWaypoints", "schematics"
    };
    private static final String[] LOG_FOLDERS = {
            "logs", "crash-reports"
    };

    private static final class Entry {
        final String id;
        final String name;
        final boolean dir;
        final long size;

        Entry(String id, String name, boolean dir, long size) {
            this.id = id;
            this.name = name;
            this.dir = dir;
            this.size = size;
        }
    }

    private static final class Job {
        final Entry source;
        final File dest;

        Job(Entry source, File dest) {
            this.source = source;
            this.dest = dest;
        }
    }

    private final Activity mActivity;
    private final ContentResolver mResolver;
    private final Uri mTreeUri;
    private final int mOptions;

    private File mGameDir;
    private long mTotalBytes;
    private long mTotalFiles;
    private long mDoneBytes;
    private long mDoneFiles;
    private int mCopied;
    private int mSkipped;
    private long mLastReport;

    public FolderImporter(Activity activity, Uri treeUri, int options) {
        mActivity = activity;
        mResolver = activity.getContentResolver();
        mTreeUri = treeUri;
        mOptions = options;
    }

    /** Must be called from the UI thread */
    public void start() {
        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            Toast.makeText(mActivity, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        mGameDir = instance.getGameDirectory();
        if (!FileUtils.ensureDirectorySilently(mGameDir)) {
            Toast.makeText(mActivity, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
            return;
        }
        sExecutorService.submit(this::execute);
    }

    private void toast(final int resId) {
        Tools.runOnUiThread(() -> Toast.makeText(mActivity, resId, Toast.LENGTH_LONG).show());
    }

    private void execute() {
        Log.i(TAG, "Begin folder import");
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, 0);
        try {
            String rootId = locateRootId();
            if (rootId == null) {
                toast(R.string.migration_not_found);
                return;
            }
            List<Job> jobs = buildJobs(list(rootId));
            if (jobs.isEmpty()) {
                toast(R.string.migration_not_found);
                return;
            }

            long[] totals = new long[2];
            for (Job job : jobs) {
                if (job.source.dir) {
                    count(job.source.id, totals);
                } else {
                    totals[0]++;
                    totals[1] += job.source.size;
                }
            }
            mTotalFiles = totals[0];
            mTotalBytes = totals[1];

            StatFs stat = new StatFs(new File(Tools.DIR_GAME_HOME).getAbsolutePath());
            if (mTotalBytes > stat.getAvailableBytes()) {
                toast(R.string.migration_no_space);
                return;
            }

            for (Job job : jobs) {
                if (job.source.dir) {
                    copyDir(job.source.id, job.dest);
                } else {
                    copyFile(job.source, job.dest);
                }
            }

            final int copied = mCopied;
            final int skipped = mSkipped;
            Tools.runOnUiThread(() -> Toast.makeText(mActivity,
                    mActivity.getString(R.string.migration_done, copied, skipped), Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Log.e(TAG, "Failed to import the folder", e);
            toast(R.string.migration_progress_failed);
            Tools.showErrorRemote(e);
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.DATA_MIGRATION);
            Log.i(TAG, "End folder import");
        }
    }

    private List<Entry> list(String parentId) {
        List<Entry> result = new ArrayList<>();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, parentId);
        try (Cursor cursor = mResolver.query(children, PROJECTION, null, null, null)) {
            if (cursor == null) return result;
            while (cursor.moveToNext()) {
                String mime = cursor.getString(2);
                long size = cursor.isNull(3) ? 0L : cursor.getLong(3);
                result.add(new Entry(cursor.getString(0), cursor.getString(1),
                        DocumentsContract.Document.MIME_TYPE_DIR.equals(mime), size));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to list a folder", e);
        }
        return result;
    }

    private static Entry find(List<Entry> entries, String name, boolean wantDir) {
        for (Entry entry : entries) {
            if (entry.dir == wantDir && name.equals(entry.name)) return entry;
        }
        return null;
    }

    private static boolean looksLikeGameDir(List<Entry> entries) {
        for (Entry entry : entries) {
            for (String marker : GAME_DIR_MARKERS) {
                if (marker.equals(entry.name)) return true;
            }
        }
        return false;
    }

    /** Finds the folder that actually holds saves/mods/versions inside what the user picked */
    private String locateRootId() {
        String rootId = DocumentsContract.getTreeDocumentId(mTreeUri);
        List<Entry> top = list(rootId);
        if (looksLikeGameDir(top)) return rootId;

        Entry minecraft = find(top, ".minecraft", true);
        if (minecraft != null && looksLikeGameDir(list(minecraft.id))) return minecraft.id;

        Entry files = find(top, "files", true);
        if (files != null) {
            List<Entry> inFiles = list(files.id);
            if (looksLikeGameDir(inFiles)) return files.id;
            Entry nested = find(inFiles, ".minecraft", true);
            if (nested != null && looksLikeGameDir(list(nested.id))) return nested.id;
        }
        return null;
    }

    private void addJob(List<Job> jobs, List<Entry> root, String name, boolean dir, File dest) {
        Entry entry = find(root, name, dir);
        if (entry != null) jobs.add(new Job(entry, dest));
    }

    private List<Job> buildJobs(List<Entry> root) {
        List<Job> jobs = new ArrayList<>();
        if ((mOptions & OPT_WORLDS) != 0) addJob(jobs, root, "saves", true, new File(mGameDir, "saves"));
        if ((mOptions & OPT_MODS) != 0) addJob(jobs, root, "mods", true, new File(mGameDir, "mods"));
        if ((mOptions & OPT_RESOURCE_PACKS) != 0) addJob(jobs, root, "resourcepacks", true, new File(mGameDir, "resourcepacks"));
        if ((mOptions & OPT_SHADERS) != 0) addJob(jobs, root, "shaderpacks", true, new File(mGameDir, "shaderpacks"));
        if ((mOptions & OPT_SCREENSHOTS) != 0) addJob(jobs, root, "screenshots", true, new File(mGameDir, "screenshots"));
        if ((mOptions & OPT_SETTINGS) != 0) {
            for (String name : SETTINGS_FILES) addJob(jobs, root, name, false, new File(mGameDir, name));
        }
        if ((mOptions & OPT_SERVERS) != 0) addJob(jobs, root, "servers.dat", false, new File(mGameDir, "servers.dat"));
        if ((mOptions & OPT_CONFIG) != 0) addJob(jobs, root, "config", true, new File(mGameDir, "config"));
        if ((mOptions & OPT_MAPS) != 0) {
            for (String name : MAP_FOLDERS) addJob(jobs, root, name, true, new File(mGameDir, name));
        }
        if ((mOptions & OPT_LOGS) != 0) {
            for (String name : LOG_FOLDERS) addJob(jobs, root, name, true, new File(mGameDir, name));
        }
        if ((mOptions & OPT_VERSIONS) != 0) addJob(jobs, root, "versions", true, new File(Tools.DIR_HOME_VERSION));
        return jobs;
    }

    private void count(String dirId, long[] totals) {
        for (Entry entry : list(dirId)) {
            if (entry.dir) {
                count(entry.id, totals);
            } else {
                totals[0]++;
                totals[1] += entry.size;
            }
        }
    }

    private static String safeName(String name) {
        if (name == null || name.isEmpty() || name.equals("..") || name.equals(".")) return "_";
        return name.replace('/', '_').replace('\\', '_');
    }

    private void copyDir(String sourceId, File destDir) throws IOException {
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new IOException("Cannot create " + destDir);
        }
        for (Entry entry : list(sourceId)) {
            File target = new File(destDir, safeName(entry.name));
            if (entry.dir) {
                copyDir(entry.id, target);
            } else {
                copyFile(entry, target);
            }
        }
    }

    private void copyFile(Entry entry, File target) throws IOException {
        mDoneFiles++;
        mDoneBytes += entry.size;
        if (target.exists() && target.length() == entry.size) {
            mSkipped++;
            report(entry.name);
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create " + parent);
        }
        Uri uri = DocumentsContract.buildDocumentUriUsingTree(mTreeUri, entry.id);
        try (InputStream in = mResolver.openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Cannot open " + entry.name);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        mCopied++;
        report(entry.name);
    }

    private void report(String name) {
        long now = System.currentTimeMillis();
        if (now - mLastReport < 200) return;
        mLastReport = now;
        int percent;
        if (mTotalBytes > 0) {
            percent = (int) Math.min(100, mDoneBytes * 100 / mTotalBytes);
        } else if (mTotalFiles > 0) {
            percent = (int) Math.min(100, mDoneFiles * 100 / mTotalFiles);
        } else {
            percent = 0;
        }
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, percent,
                mActivity.getString(R.string.migration_progress_copying, name));
    }
}
