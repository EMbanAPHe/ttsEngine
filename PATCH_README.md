# Patch notes for Kokoro integration (updatedv001)

This patch fixes build blockers and wires Kokoro into the existing install flow.

## What changed
1) **values/arrays.xml** — was malformed (two `<resources>` blocks, bad XML prolog, duplicate Kokoro arrays). Replaced with a clean file to restore aapt2.
2) **Downloader.java** — ensure `Source.KOKORO` uses the keys from `values/arrays_kokoro.xml` (`kokoro-en-v0_19`, `kokoro-en-v1_0`, `kokoro-en-lite`). Fill `KOKORO_URLS` with correct archive URLs (zip, tar.gz or tar.bz2). See below.
3) **KokoroInstaller.kt** — add support for `.tar.gz`/`.tgz` and plain `.onnx` files (in case the repo provides a raw model file). See the snippet in `patches/KokoroInstaller.extractToModelsDir.patch`.
4) **UI spacing** — consider increasing top padding in `activity_manage_languages.xml` from `12dp` to `24dp` for consistency.

## Required action (you)
Update `Downloader.java` to provide working URLs for the three Kokoro variants by setting `KOKORO_URLS`:

- `kokoro-en-v1_0` → onnx-community/Kokoro-82M-v1.0-ONNX
- `kokoro-en-v0_19` → onnx-community/Kokoro-EN-v0.19-ONNX (or equivalent repo for v0.19)
- `kokoro-en-lite` → onnx-community/Kokoro-EN-Lite-ONNX

Use an **archive URL** (prefer `.zip` or `.tar.gz`) containing at least one `.onnx` and its corresponding `tokens.*` file. If you only have direct file URLs, keep the `.onnx` URL; the installer will place the file in the model directory. If a `tokens.*` file is required and not present, the runtime will error.

> Tip: archive URLs typically end with something like `/archive/main.zip` or `/archive/refs/heads/main.zip` depending on the host. 

## Sanity checklist
- `values/arrays.xml` validates (one `<resources>` root)
- `values/arrays_kokoro.xml` contains exactly the 3 items you want shown.
- `Downloader.KOKORO_URLS` keys **match** the strings in `arrays_kokoro.xml`.
- Build: `./gradlew assembleDebug` should now pass.
- UI: Manage Languages screen shows 3 Kokoro options in the dropdown and the **Install Selected** button.
- Install: selecting an option triggers a download and extraction into your models directory.

