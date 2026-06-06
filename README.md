# Meeting Transcriber

A simple standalone native Android app for finding meeting recordings on the phone and sending them to OpenRouter for speech-to-text transcription.

## What It Does

- Discovers audio in `Music/Record/SoundRecord/` using MediaStore.
- Lets the user choose a recorder folder with Android's document tree picker when MediaStore is empty or permission is denied.
- Queues one or more files for transcription with WorkManager.
- Stores jobs, notes, transcript text, raw provider JSON, and optional word/segment timestamps in Room.
- Plays the source audio from transcript detail and highlights the current word or segment when timestamps are available.
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

Tap the key button in the top app bar and paste an OpenRouter API key. The app stores it locally using AndroidX `EncryptedSharedPreferences` when available. If encrypted preferences cannot initialize on a device, the app falls back to regular private `SharedPreferences` with a TODO in code documenting the security tradeoff.

The key is never hardcoded in source and should not be committed.

## Default Folder

The default discovery target is:

```text
Music/Record/SoundRecord/
```

Use **Choose recorder folder** if the recorder app stores files elsewhere or MediaStore access is denied.

## MVP Limitations

- Audio is sent as a direct base64 JSON upload to OpenRouter.
- The default warning threshold is 25 MB. Larger files can still be attempted, but provider limits or timeouts may fail.
- Future production work should add audio chunking with `MediaExtractor`/`MediaMuxer` or a dedicated media pipeline.
- Provider timestamp formats vary; the parser accepts text-only responses and optional segment/word timestamp arrays.
