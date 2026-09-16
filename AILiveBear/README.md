# AI Live Bear — Stage 1

Native Android app (Kotlin + Jetpack Compose) for an AI virtual character
that will eventually react to TikTok LIVE comments. This is **Stage 1
only**: project foundation, 3D rendering, idle animation, settings screen.
No AI, no TTS, no real TikTok connection yet — those are later stages.

## 1. Engine choice: Filament

**[Filament](https://github.com/google/filament)** (`com.google.android.filament`),
Google's open-source real-time physically-based rendering engine, was
chosen over Godot / a full game engine:

- **Native-Android-first, not a separate app shell.** Filament renders into
  a plain `SurfaceView`/`Surface`, so it drops straight into a normal
  Activity/Compose hierarchy. Godot instead owns its own window
  (`GodotFragment`) and is happier as a standalone engine-driven app;
  mixing it cleanly with a Compose Settings screen and a gesture-driven
  navigation model (this app's Stage 1 requirement) is much more awkward.
- **Lightweight and mobile-tuned.** Filament is built specifically to be
  small and efficient on phones/tablets — relevant since this app is meant
  to run continuously during a LIVE stream, not just for short sessions.
- **glTF 2.0 support via `gltfio`**, including skeletal animation and morph
  targets out of the box — exactly what a later facial rig (mouth shapes
  for lip sync, blend-shape eyes/eyebrows) will need, with no separate
  asset pipeline required.
- **Kotlin-first API**, actively maintained by Google, used in production
  AR/3D Android apps.

The trade-off: Filament is lower-level than a full game engine, so some
things (procedural placeholder geometry without a `matc`-compiled custom
material) aren't trivially available — this is exactly why the placeholder
approach below uses a real glTF file instead of code-generated geometry.

## 2. Project structure

```
AILiveBear/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/models/          # placeholder 3D asset goes here (see below)
│       ├── res/values/             # strings.xml, themes.xml
│       └── java/com/ailivebear/app/
│           ├── MainActivity.kt
│           ├── AILiveBearApp.kt           # Application/service locator
│           ├── core/
│           │   ├── model/                 # CharacterState, EmotionType (shared contracts)
│           │   └── util/                  # AppLog
│           ├── character/
│           │   ├── CharacterManager.kt    # coordinates everything below
│           │   ├── engine/                # FilamentEngine, ModelLoader, CharacterRenderView
│           │   └── animation/             # Breathing/Blink/HeadMovement/AnimationController
│           ├── background/                # BackgroundManager (Filament Skybox)
│           ├── ai/                        # AIProvider interface (no impl yet)
│           ├── tts/                       # TTSProvider interface (no impl yet)
│           ├── tiktok/                    # TikTokCommentProvider interface + MockCommentProvider
│           ├── settings/                  # SettingsRepository (DataStore), SettingsScreen (Compose)
│           └── ui/                        # MainScreen, RootScreen (swipe navigation), theme/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

Each top-level package under `com.ailivebear.app` is one of the modules
requested for Stage 1 (character / AI / TTS / TikTok / settings /
background), with a clean interface boundary so later stages add files
inside these packages rather than restructuring them.

## 3. Dependencies / versions used

- AGP 8.5.2, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26
- Jetpack Compose via `compose-bom:2026.04.01`
- `com.google.android.filament:filament-android:1.76.1`
- `com.google.android.filament:gltfio-android:1.76.1`
- `androidx.datastore:datastore-preferences:1.1.1`
- `kotlinx-coroutines-android:1.8.1`

These were current as of when this project was generated — bump them in
`app/build.gradle.kts` / the root `build.gradle.kts` if Android Studio
flags newer stable versions when you open the project.

## 4. Setup / build instructions

### Option A — GitHub Actions (no local Android Studio needed)

`.github/workflows/build.yml` is included. Push this project to a GitHub
repo and it builds a debug APK automatically on every push to `main` (or
run it manually from the **Actions** tab → "Build APK" → **Run workflow**).
When the run finishes, open it and download the **AILiveBear-debug-apk**
artifact from the bottom of the summary page — that's a ready-to-install
APK. It uses `gradle/actions/setup-gradle` to run Gradle directly, so it
does **not** need `gradlew`/`gradle-wrapper.jar` (see note below). The
build succeeds with or without a placeholder model in
`assets/models/` (see section 5) — without one, the installed app shows
just the background.

### Option B — Android Studio

1. **Open in Android Studio** (Koala/Ladybug or newer recommended).
   Android Studio will offer to regenerate the Gradle wrapper JAR
   automatically on first sync — this repo includes
   `gradle/wrapper/gradle-wrapper.properties` (pointing at Gradle 8.7) but
   **not** the binary `gradle-wrapper.jar`, since it can't be produced as
   plain source here. If you'd rather do it manually before opening the
   project: install Gradle locally once, `cd` into the project root, and
   run `gradle wrapper`.
2. Let Gradle sync — it will pull Filament/Compose/etc. from Maven Central.
3. **Add a placeholder 3D model** (see next section) — the app builds and
   runs without one, but you won't see a character until you do.
4. Run on a device or emulator with **OpenGL ES 3.0+** support (declared
   as a required feature in the manifest).
5. You should see: a solid soft-colored background, and — once a
   placeholder model is in place — a gently breathing, swaying object on
   screen. Swipe right-to-left to open Settings; swipe left-to-right (or
   tap the back arrow) to return.

## 5. Placeholder 3D asset

Stage 1 intentionally ships **no bundled 3D model file**. See
`app/src/main/assets/models/README.md` for exact steps — short version:
drop any valid `.glb` (e.g. a free CC0 sample from the
[Khronos glTF-Sample-Assets](https://github.com/KhronosGroup/glTF-Sample-Assets)
repo) at `app/src/main/assets/models/bear_placeholder.glb`. If it's
missing, `ModelLoader` logs a warning and the app still runs fine with
just the background and camera — it will not crash.

## 6. Where Stage 2 connects

Everything in Stage 1 was built with these extension points in mind:

- **`ModelLoader`** loads whatever glTF is at `bear_placeholder.glb`.
  Stage 2 swaps in the real rigged Bubu-the-bear asset (same path, or a
  `characterId -> path` map in `CharacterManager.applySettings`) — no
  change to the loading code itself.
- **`CharacterManager.applyFrameAnimation`** currently applies
  breathing/head-sway to the whole model **root**, because Stage 1's
  placeholder has no named skeleton. Once the rigged asset exists, Stage 2
  should look up specific bones via `TransformManager` (head, jaw, eyes)
  by name instead, and move head-sway/pitch onto the head bone alone.
- **`FrameAnimation.blink`** (in `AnimationController`) is already
  computed every frame with natural randomized timing, but not applied to
  anything yet. Stage 2's `EyeController` should map it to an eye
  blend-shape/morph-target weight on the rigged asset.
- **`FrameAnimation.headPitchDegrees`** is likewise computed but unused —
  same story, once a head bone exists.
- **`AnimationController`** already threads `CharacterState` through;
  Stage 2's `EmotionController` and a `MouthController`/
  `LipSyncController` should become additional sub-controllers feeding the
  same per-frame `update()` → `CharacterManager.applyFrameAnimation` path,
  gated by `CharacterState` (e.g. suppress idle sway while `ANSWERING`).
- **`AIProvider`, `TTSProvider`, `TikTokCommentProvider`** interfaces (plus
  `MockCommentProvider`) are ready to implement and wire into
  `CharacterManager`/`AnimationController` without changing their public
  shape — this is what Stage 3/4 build against.
- **`SettingsScreen`** already has the TikTok username, AI provider + API
  key, character, and background fields, persisted via
  `SettingsRepository`. Later stages read from the same repository rather
  than adding new persistence.

## 7. TikTok connector (real, not mocked)

`tiktok/WebSocketTikTokCommentProvider.kt` is a real `TikTokCommentProvider`
implementation, talking to a TikTok-Live-Connector relay over a plain
WebSocket (default host `ws.adjdev.site`) — matching the contract confirmed
against a known-working reference client:

- Connects to `wss://<host>?username=<tiktok username>`.
- Parses incoming JSON flexibly (`{event/type, data}` wrapper or bare
  object; several possible field names for username/text), same as the
  reference.
- Auto-reconnects after a fixed delay (default 3s) if the socket drops
  while still supposed to be connected.
- Gift/follow parsing is **best-effort** (common `tiktok-live-connector`
  field names) and not yet confirmed against this relay's actual traffic —
  see the kdoc in that file before relying on it.

**Settings → TikTok LIVE** now has a "Test connection" panel so you can
verify, on-device, that comments actually arrive from a real LIVE session —
independent of the main character pipeline, which doesn't consume comments
yet. This panel is a verification tool only; it is not the production
path. The next stage should feed `WebSocketTikTokCommentProvider` into
`CharacterManager`/`AnimationController` directly (comment → AI → TTS →
reaction), replacing `MockCommentProvider` for real use while keeping the
mock available for testing without a live session.

## 8. What's deliberately NOT in Stage 1

Per the brief: no TikTok integration, no AI API calls, no dashboard/status
bar/chat/FPS/debug UI in normal mode, and the main screen contains only
the character + background. All animation is local, on-device, and
timer/random-driven.
