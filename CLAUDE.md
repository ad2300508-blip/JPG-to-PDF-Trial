# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Morph (formerly OmniConvert Studio; repo name "JPG to PDF Trial"): a single-module Android app (Kotlin, Jetpack Compose, Material3, package `com.example`, applicationId `com.aistudio.omniconvert.kxmpzq`, minSdk 26 / targetSdk 35). It converts and processes files fully on-device (images→PDF, PDF merge/compress/organize/→images, watermark & page numbers, password protect/unlock, PDF/DOCX→Markdown, image/audio conversion, ZIP create/extract, OCR) plus a Gemini AI hub. The UI is in Italian and follows the "Morph" design (light/dark/system theme).

## Build & test

There is no local Android SDK in the cloud environment; CI is the build check.

- CI: `.github/workflows/build-apk.yml` (JDK 21) runs on every push/PR:
  `./gradlew assembleDebug testDebugUnitTest -PversionCode=<run> -PversionName=1.0.<run>`; the APK is uploaded as the `Morph-APK` artifact (zipped). Pushes to `main` also publish the bare APK as a GitHub release `build-<run>`; the user downloads it from `https://github.com/ad2300508-blip/JPG-to-PDF-Trial/releases/latest/download/Morph.apk` (also attached as `OmniConvert-Studio.apk` for old links).
- Auto-merge: `.github/workflows/auto-merge.yml` merges a same-repo, non-draft PR into `main` as soon as its `Build APK` run is green (label `no-automerge` to opt out), then dispatches `build-apk.yml` on `main` to publish the release. Only the PR's latest commit counts.
- CI job logs are not downloadable from the sandbox. On failure the workflow posts the Kotlin/Gradle errors as a **commit comment**: read them with `gh api repos/ad2300508-blip/JPG-to-PDF-Trial/commits/<sha>/comments --jq '.[].body'`.
- Unit tests: `app/src/test/java/com/example/` (`UtilsUnitTest` covers PageRange, FileNames, NaturalOrder, stamp config, password strength, Markdown parser).
- Local (with SDK): `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, single test: `./gradlew testDebugUnitTest --tests "com.example.PdfConverterUnitTest"`.
- No lint/format tooling is configured.
- Versioning defaults live in `app/build.gradle.kts` (`versionCode`/`versionName` project properties).
- If `debug.keystore` (or `debug.keystore.base64` in CI) is present at the repo root it is used for signing; otherwise the default debug key.
- GitHub: `gh` GraphQL is blocked in the cloud sandbox, so use REST (`gh api repos/.../pulls`) for PRs.

## Architecture

Standard layering under `app/src/main/java/com/example/`: `service/` (pure work: PDFBox-Android, ML Kit OCR, OkHttp for Gemini) → `viewmodel/` (one `AndroidViewModel` per tool, exposing StateFlow) → `ui/` screens → `model/` item/state classes. `util/` holds shared helpers: `OutputStorage` (all saving), `BitmapLoader` (EXIF-aware sampled decode), `PageRange`, `TextPdfWriter`, `UriInfo`, `FileNames`, `resultOf`/`userMessage` (Italian error text).

Key files: single-PDF tools share `SinglePdfViewModel` (`viewmodel/PdfToolsViewModels.kt`) and `SinglePdfScaffold` (`ui/PdfToolsScreens.kt`); watermark/password work is in `service/PdfStampService.kt` + `ui/PdfSecurityScreens.kt`; compress/organize/render in `service/PdfToolsService.kt`. Motion tokens, `motionSpec`, `pressScale`, `enterAnimation`, haptics: `ui/theme/Motion.kt`. Markdown rendering for Gemini: `ui/components/MarkdownText.kt`.

Things that need several files to understand:

- **Navigation shell** (`OmniConvertApp.kt`): no Navigation graph. `AppDestination(tab, tool)` + `AnimatedContent`; tabs are `MainTab` (Strumenti, Gemini, Cronologia, Impostazioni). Phone uses `MorphBottomBar` (hidden while a tool is open), tablets (≥ ~840dp) use `MorphRail` and the home shows a recents panel. `ToolContent` maps `ToolScreen` (defined in `ui/home/ToolCatalog.kt`) to tool screens; adding a tool = new `ToolScreen` entry in the catalog + a branch in `ToolContent`. Tool headers get their back arrow from the `LocalToolBack` CompositionLocal. Cross-tool hand-offs ("continue with merge/markdown", send to Gemini) are callbacks wired here. OCR ("Image to text") reuses the Markdown screen. Incoming SEND/SEND_MULTIPLE/VIEW intents become `SharedFiles` in `MainActivity`; `SharedFilesSheet` lets the user pick a tool (`toolsFor(mime)` uses each tool's `accepts`), then `sendToTool` hands the URIs over.
- **Theming**: colors are never hard-coded in screens. `ui/theme/Color.kt` defines `MorphColors` light/dark palettes exposed via `LocalMorphColors`; `OmniBg`, `OmniText`, `OmniAccent`… are `@Composable @ReadOnlyComposable` top-level getters (keep that one-getter-per-line form) so every screen follows the active theme. Legacy names (`Slate*`, `CyanAccent`, …) are aliases. `Tint*` are static category colors. `OmniConvertTheme(themeMode)` in `Theme.kt` provides the palette and sets system bar appearance.
- **Reusable UI**: `ui/components/OmniDesignSystem.kt` (tool header, dropzone, segmented control, action bar, progress banner) and `MorphCommon.kt` (icon tiles, chips, history rows, `ConversionResultView`, share/open helpers). New tool screens should compose these rather than restyle.
- **Persistence**: `data/AppPreferences` (theme, reduce motion, haptics, favourite tools) and `data/HistoryRepository` (≤200 entries, JSON in SharedPreferences) are singleton objects initialized in `MainActivity`. Every tool ViewModel calls `HistoryRepository.record(...)` on success. Gemini key/quota use their own prefs inside `GeminiApiService`.
- **Output files**: always via `OutputStorage.save(context, name, mime, OutputFolder.X) { out -> … }` → MediaStore `Download/Morph/<category>/` (API 29+, IS_PENDING) or app-specific Downloads + FileProvider below; write big outputs to a cache temp file first; sharing/opening uses content URIs with `FLAG_GRANT_READ_URI_PERMISSION`. Camera capture (`CameraCapture.kt`) uses a FileProvider (`res/xml/file_paths.xml`, authority `${applicationId}.fileprovider`) with `TakePicture`, so no CAMERA permission.
- **Gemini key**: `BuildConfig.GEMINI_API_KEY` is empty by default (see `.env.example`); the user normally enters the key in-app.

## Notes

- UI strings are Italian; keep new strings Italian. One exception: `GeminiHubScreen` compares `apiKeyStatus == "CONNECTED"` against a value emitted by the ViewModel — do not translate that literal.
- Not implemented: Word→PDF. Not tested on a physical device by Claude: verify UI changes through CI only.
- Animations must respect `LocalReduceMotion` (use `motionSpec`/`tweenSpec`). Build `ContentTransform(...)` explicitly in top-level functions (e.g. `stageTransition`); `using` only exists inside the `AnimatedContent` transition scope.
- Kotlin gotchas hit before: `/*` inside a KDoc (e.g. "image/*") opens a nested comment and breaks the file; no `break`/`continue` inside inline lambdas.

## Regole di lavoro (dall'utente)

- **Zero esplorazione cieca:** vai ai file indicati sopra; chiedi il percorso se non lo conosci invece di scansionare tutto il progetto.
- **Niente build automatiche:** non eseguire `./gradlew` dopo ogni modifica (log troppo lunghi). Solo su richiesta esplicita; la verifica normale è la CI su GitHub.
- **Risposte sintetiche:** modifica il codice senza rispiegarlo; a fine lavoro un elenco puntato di 2 righe.
- **File grandi:** usa `grep` o leggi solo i range di righe necessari per i file sopra le 300 righe.
- **Design:** usa i token `Omni*` / `MaterialTheme`; mai `Color(0xFF...)` dentro le singole schermate.
