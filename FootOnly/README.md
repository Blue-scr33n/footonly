# FootOnly for Android

A small, privacy-first Android image-censoring MVP. Open `FootOnly/` in Android Studio and run the `app` configuration on a device or emulator (Android 8.0+).

## What it does

- Selects an image using Android's system photo picker.
- Pixelates the selected image locally, on demand.
- Saves the censored result as a new image under `Pictures/FootOnly`; it never overwrites the original.
- Does not request broad storage access, network access, screen-capture access, or background permissions.

## Scope

This is a simple user-initiated image tool, not an automatic nudity detector or an across-other-apps screen filter. The repository's uploaded ZIP contains an ONNX model placeholder rather than a model, so automatic detection cannot be made reliable or privacy-preserving without a real, licensed model. The pixelation applies to the whole image; it does not identify or selectively censor body parts.
