# Voice Time Actions

`Voice Time Actions` is a native Android app built for the Wispr Actions take-home challenge.

It focuses on one narrow experience: speak a timer or alarm request, resolve it on-device, preview it, and launch the Android Clock action.

## Why This Use Case

I chose timers and alarms because they are a natural fit for voice:

- short and frequent
- often used hands-free
- easy to map to Android system actions
- fully executable on-device

That made it possible to build one polished flow instead of a broad assistant demo.

## Product Flow

1. Open the app and grant microphone permission.
2. Tap the mic and speak a phrase like:
   - `set a timer for 10 minutes for pasta`
   - `wake me up at 6 AM`
   - `set alarm to remind me about doing task at 5 PM today`
3. See the live transcript.
4. See the resolved action preview.
5. Tap `Run Action` to launch the Clock intent.

## Architecture

The app uses a simple MVVM structure with clear seams:

- `speech/`
  wraps Android speech recognition and exposes speech events
- `action/domain/`
  defines `VoiceAction`, `VoiceActionParser`, and `VoiceActionExecutor`
- `action/data/`
  contains parser implementations, the ML Kit integration, and Android action execution
- `voiceaction/ui/`
  contains the Compose UI, UI state, and ViewModel

Main flow:

```text
SpeechRecognizer
  -> transcript
  -> VoiceActionParser
  -> VoiceAction
  -> UI preview
  -> VoiceActionExecutor
  -> Android Clock intent
```

## On-Device Strategy

Speech transcription uses Android's built-in:

- `android.speech.SpeechRecognizer`
- `RecognizerIntent`

For intent resolution, the app supports two on-device paths:

- primary: ML Kit Prompt API with Gemini Nano when supported
- fallback: deterministic local parsing for timers and alarms

This keeps the app usable offline even on unsupported devices.

## Prompting Approach

The LLM path is intentionally constrained. It is asked to return strict JSON for a very small schema, not free-form text.

The response is validated before use, and the app falls back to deterministic parsing if the output is incomplete or malformed.

## Fallback Parser

The fallback parser handles natural timer and alarm phrases such as:

- `set a timer for 10 minutes`
- `wake me in 20 minutes`
- `set an alarm for 7 AM`
- `wake me up at 5:30 PM today`
- `set alarm to remind me about doing task at 5 PM today`

It also handles:

- AM / PM normalization
- timer durations
- alarm labels before or after the time
- basic timezone-aware alarm conversion

## Action Execution

Actions are executed with Android public Clock intents:

- timers: `AlarmClock.ACTION_SET_TIMER`
- alarms: `AlarmClock.ACTION_SET_ALARM`

## Device Support

ML Kit Prompt API support depends on device support.

I didn't have a supported personal device to fully test the Gemini Nano path, so I focused on strengthening the fallback on-device flow.

Because of that, I did not fully validate the Gemini Nano execution path end to end on hardware.

On unsupported devices, the app still works end to end through the offline fallback parser.

## Testing

The project includes unit coverage for:

- timer parsing
- alarm parsing
- model JSON parsing
- LLM fallback behavior
- ViewModel state transitions
- timezone conversion

Recent verification:

```bash
./gradlew assembleDebug testDebugUnitTest
```

## Limitations

- Gemini Nano availability depends on device support
- Android public APIs are good for creating timers and alarms, not controlling existing running timers
- speech quality depends on the device and installed speech services
- the app is intentionally narrow and not a general assistant

## If I Had More Time

- add clarification for incomplete commands
- improve alarm phrasing coverage further
- add recent-action history
- polish demo assets

## Running The App

1. Open the project in Android Studio.
2. Run it on a device or emulator.
3. Grant microphone permission.
4. Speak a timer or alarm request.
5. Tap `Run Action`.

## Best Results

For the most reliable parsing, keep the phrase short and specific, include clear time information, and place labels after the time when possible, for example: `set an alarm for 5 PM for gym`.

## Key Files

- [MainActivity.kt](./app/src/main/java/com/example/voicetotext/MainActivity.kt)
- [AndroidSpeechRecognizer.kt](./app/src/main/java/com/example/voicetotext/speech/data/AndroidSpeechRecognizer.kt)
- [LlmVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/LlmVoiceActionParser.kt)
- [TimerVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/TimerVoiceActionParser.kt)
- [AlarmVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/AlarmVoiceActionParser.kt)
- [AndroidVoiceActionExecutor.kt](./app/src/main/java/com/example/voicetotext/action/data/AndroidVoiceActionExecutor.kt)
- [VoiceActionViewModel.kt](./app/src/main/java/com/example/voicetotext/voiceaction/ui/VoiceActionViewModel.kt)
- [VoiceActionRoute.kt](./app/src/main/java/com/example/voicetotext/voiceaction/ui/VoiceActionRoute.kt)
- [VoiceActionScreen.kt](./app/src/main/java/com/example/voicetotext/voiceaction/ui/VoiceActionScreen.kt)
