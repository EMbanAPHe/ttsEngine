package com.k2fsa.sherpa.onnx.tts.engine;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Restored, simplified downloader entrypoint.
 * The real download/extract code lives in the existing helpers the app used before.
 * We just fan-out to the correct path based on source (PIPER/COQUI/KOKORO) + the user's selection text.
 */
public class Downloader {

    public enum Source { PIPER, COQUI, KOKORO }

    // Example model map; keep using whatever map/logic your project already had for Piper/Coqui.
    // For Kokoro we add explicit sizes with stable URLs (you can change to mirrors if needed).
    private static final Map<String, String> KOKORO_URLS = new HashMap<>();
    static {
        // These are example names that must match your arrays.xml labels
        // Point them at the ONNX-community Kokoro 82M v1.0 artifacts you want to use.
        // If your installer expects .tar.gz, point to the .tar.gz; if it expects loose files, point accordingly.
        KOKORO_URLS.put("Kokoro Small (82M) – en-US", "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/kokoro-v1_0.onnx?download=true");
        KOKORO_URLS.put("Kokoro Medium – en-US",       "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/kokoro-v1_0.onnx?download=true");
        KOKORO_URLS.put("Kokoro Large – en-US",        "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/kokoro-v1_0.onnx?download=true");
        // If you have multi-file archives or different locales, extend the map here.
    }

    public static void startDownload(@NonNull Activity activity,
                                     @NonNull String selectionText,
                                     @NonNull Source source) {
        switch (source) {
            case PIPER:
                startPiperDownload(activity, selectionText);
                break;
            case COQUI:
                startCoquiDownload(activity, selectionText);
                break;
            case KOKORO:
                startKokoroDownload(activity, selectionText);
                break;
        }
    }

    private static void startPiperDownload(Activity activity, String selectionText) {
        // Keep your existing Piper model download logic here.
        // Typically: map selection -> URL(s), download to temp, extract, then:
        // LangDB.getInstance(activity).addLanguage(...);
        runToast(activity, "Downloading Piper: " + selectionText);
        LegacyPiperInstaller.downloadAndInstall(activity, selectionText);
    }

    private static void startCoquiDownload(Activity activity, String selectionText) {
        // Keep your existing Coqui model download logic here.
        runToast(activity, "Downloading Coqui: " + selectionText);
        LegacyCoquiInstaller.downloadAndInstall(activity, selectionText);
    }

    private static void startKokoroDownload(Activity activity, String selectionText) {
        final String url = KOKORO_URLS.get(selectionText);
        if (url == null) {
            runToast(activity, "Unknown Kokoro option: " + selectionText);
            return;
        }
        runToast(activity, "Downloading Kokoro: " + selectionText);
        // Use the Kotlin installer you already added (or keep this class pure-Java if you prefer).
        KokoroInstaller.downloadAndInstall(activity, selectionText, url, new KokoroInstaller.Callback() {
            @Override
            public void onInstalled(String modelName, String lang, String country, String modelType) {
                // Ensure the language shows up for selection
                LangDB db = LangDB.getInstance(activity);
                // Arguments follow your project’s Lang schema: name, lang, country, pitch, speed, gain, type
                db.addLanguage(modelName, lang, country, 0, 1.0f, 1.0f, modelType);
                runToast(activity, "Installed " + modelName + " (" + lang + "_" + country + ")");
            }

            @Override
            public void onError(String message) {
                runToast(activity, "Kokoro install failed: " + message);
            }
        });
    }

    // Small helper to post short toasts from background tasks
    private static void runToast(Context ctx, String text) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show());
    }

    // Optional: put common target folder logic here if you need it reused
    public static File getVoicesRoot(Context ctx) {
        return new File(ctx.getExternalFilesDir(null), "voices");
    }
}
