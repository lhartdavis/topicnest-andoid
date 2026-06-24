# Meeting Transcriber

A simple standalone native Android app for finding meeting recordings on the phone and sending them to OpenRouter for speech-to-text transcription.

## What It Does

- Discovers audio in `Music/Record/SoundRecord/` using MediaStore.
- Lets the user choose a recorder folder with Android's document tree picker when MediaStore is empty or permission is denied.
- Searches discovered filenames, keeps already-transcribed files in a separate read-only section, and remembers the compact/detailed discovery layout.
- Queues one or more files for transcription with WorkManager.
- Offers an autonomous mode that hourly queues new recordings from the past 48 hours when they are longer than 5 minutes and no longer than 3 hours.
- Stores jobs, notes, transcript text, raw provider JSON, and optional word/segment timestamps in Room.
- Plays the source audio from transcript detail and highlights the current word or segment when timestamps are available.
- Searches transcript titles, notes, and text with title matches ranked highest.
- Deletes transcript jobs and timestamp rows without deleting the original audio file.
- Shares plain text only with notes first, then transcript text.

## Run It

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on a device or emulator.

Command line build:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:assembleDebug
```

## Permissions

The app requests only the permissions needed for this workflow:

- `INTERNET` for OpenRouter.
- `READ_MEDIA_AUDIO` on Android 13+.
- `READ_EXTERNAL_STORAGE` on Android 12 and below.

It does not request `RECORD_AUDIO` because it does not record audio.

## OpenRouter API Key

Tap the key button in the top app bar and paste an OpenRouter API key. Use **Save & test** to call OpenRouter's current-key endpoint before queueing transcription jobs. The key icon is green when the key verifies, red when OpenRouter rejects it, and neutral when it has not been tested or could not be checked.

The app stores the key locally using AndroidX `EncryptedSharedPreferences` when available. If encrypted preferences cannot initialize on a device, the app falls back to regular private `SharedPreferences` with a TODO in code documenting the security tradeoff.

The key is never hardcoded in source and should not be committed.

## Default Folder

The default discovery target is:

```text
Music/Record/SoundRecord/
```

Use **Choose recorder folder** if the recorder app stores files elsewhere or MediaStore access is denied.

## MVP Limitations

- Audio is sent as a direct base64 JSON upload to OpenRouter.
- The default JSON upload warning threshold is 25 MB. The app estimates base64 expansion before warning because a 22 MB raw audio file becomes roughly 30 MB inside JSON.
- `RecursiveChunkingPolicy` defines the future splitting behavior for oversized files: recursively split by time until estimated upload windows fit the direct-upload limit, with small overlap around chunk boundaries.
- Bare `.aac` recorder files and oversized recordings are converted to temporary MP3 files with JavaCV/Bytedeco FFmpeg before upload. The app uses the GPL FFmpeg build so LAME-style MP3 encoding is available on device.
- If a converted MP3 still exceeds the JSON upload budget, the app uses `RecursiveChunkingPolicy` to split the source audio into smaller MP3 chunks, transcribes them sequentially, offsets timestamps back to the original timeline, and stores one merged transcript.
- If OpenRouter still rejects a small AAC/M4A request with HTTP 400 outside the MP3 path, the app retries once by decoding the audio to a temporary 16-bit PCM WAV file only when the estimated WAV upload still fits the direct JSON budget.
- The FFmpeg dependency is intentionally limited to 64-bit Android ABIs (`arm64-v8a`, `x86_64`) to match the published Bytedeco Android binaries and avoid packaging unsupported native targets.
- Future production work should consider streaming uploads or backend media processing if recordings become too large for phone CPU/cache constraints.
- Provider timestamp formats vary; the parser accepts text-only responses and optional segment/word timestamp arrays.

## Queue Recovery

The Transcripts tab includes recovery actions when jobs fail or appear stuck:

- **Retry failed** requeues all failed jobs.
- **Restart queue** cancels the current WorkManager chain and rebuilds it from queued/processing Room rows.
- **Stop stuck** marks queued/processing jobs as failed so they no longer block the workflow.

Use **Test key** first when jobs fail with HTTP 400 or auth-like errors. For `Broken pipe`, retry on a stable connection; the worker will convert or chunk oversized audio before retrying.

## Autonomous Mode

The circling-arrows button in the top app bar toggles autonomous mode. When enabled, WorkManager runs an hourly scan of the same MediaStore and chosen-folder sources used by Discovery. The scan queues only recordings with a known modified time in the past 48 hours, known duration greater than 5 minutes, and duration no longer than 3 hours. Any recording that already has a queued, processing, failed, or transcribed job is skipped so the app does not create duplicates.
