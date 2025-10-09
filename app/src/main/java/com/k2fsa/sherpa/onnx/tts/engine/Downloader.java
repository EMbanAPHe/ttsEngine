package com.k2fsa.sherpa.onnx.tts.engine;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal Downloader kept for backward compatibility with existing layout.
 * For Kokoro we call KokoroInstaller directly from ManageLanguagesActivity.
 */
public class Downloader {

    private static void httpDownload(String url, File out) throws Exception {
        out.getParentFile().mkdirs();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        InputStream in = conn.getInputStream();
        FileOutputStream fo = new FileOutputStream(out);
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            fo.write(buf, 0, r);
        }
        fo.close();
        in.close();
    }

    // Legacy stub if something still calls startDownload to fetch piper/coqui
    public static void startDownload(Activity activity, String url, String outName) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                httpDownload(url, new File(activity.getFilesDir(), outName));
                activity.runOnUiThread(() -> Toast.makeText(activity, "Downloaded: " + outName, Toast.LENGTH_SHORT).show());
            } catch (Throwable t) {
                Log.e("Downloader", "Download failed", t);
                activity.runOnUiThread(() -> Toast.makeText(activity, "Download failed", Toast.LENGTH_LONG).show());
            }
        });
    }
}