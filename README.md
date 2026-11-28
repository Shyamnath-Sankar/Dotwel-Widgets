# AnyWidgets

Nothing OS-inspired monochrome home-screen widget suite for Android by Dotwel Labs.

- 56 customizable widgets: dotted OLED clocks, analog clocks, date and calendar strips,
  battery and connectivity tiles, Bluetooth device cards, quotes, photo frames and live
  device telemetry.
- Jetpack Compose UI, app widget providers, Room and DataStore for local state.
- Package: `com.dotwellabs.anywidgets` · minSdk 24 · targetSdk 36

## Build

The release bundle is built by GitHub Actions, not on a developer machine.

- Push to `main`, or run the **Build Release AAB** workflow manually from the Actions tab.
- The workflow produces a signed `app-release.aab` artifact that can be uploaded to Play Console.

### Required repository secrets

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of `my-upload-key.jks` (upload key) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password for alias `upload` |

Recreate the base64 value with:

```bash
base64 -w0 my-upload-key.jks > keystore.b64
```

Keep `my-upload-key.jks` and its passwords out of the repository. Losing the upload key
means you must request an upload key reset from Google Play.

## Local development

Open the project in Android Studio, add a `.env` with `GEMINI_API_KEY` if AI features are
needed (see `.env.example`), then run on a device or emulator. Debug builds use the local
`debug.keystore` and do not need the upload key.
