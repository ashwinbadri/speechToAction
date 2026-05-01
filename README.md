# Voice Time Actions

`Voice Time Actions` is a native Android app built for the Wispr Actions take-home challenge.

It focuses on a single narrow voice-to-action experience:

- speak a timer or alarm request
- transcribe it on-device
- resolve it into a structured action
- preview the result
- launch the Android Clock intent

The app is intentionally opinionated around **voice-powered time actions** instead of trying to be a general assistant.

## Why This Use Case

I chose **timers and alarms** because they are a strong fit for voice:

- they are short, frequent, and naturally spoken
- they are often used hands-free while cooking, working out, or getting ready
- they map cleanly to Android system actions
- they can be executed locally without needing app-specific integrations

This made it possible to go deep on one polished experience instead of building a shallow general-purpose assistant.

## Product Experience

The app is designed around a voice-first flow:

1. open the app
2. grant microphone permission
3. tap the hero mic button
4. speak a phrase like:
   - `set a timer for 10 minutes for pasta`
   - `wake me up at 6 AM`
   - `set alarm to remind me about doing task at 5 PM today`
5. see the live transcript
6. see the resolved action preview
7. tap `Run Action`
8. launch the Android Clock timer or alarm flow

The UI is intentionally optimized around:

- a single hero mic interaction
- fast visual feedback while listening
- a human-readable action preview
- clear post-action follow-up states

## Architecture

The app is structured in a testable MVVM style with clean seams between speech, parsing, execution, and UI.

### Layers

- `speech/`
  - wraps Android speech recognition
  - converts platform callbacks into domain speech events
- `action/domain/`
  - defines `VoiceAction`
  - defines `VoiceActionParser`
  - defines `VoiceActionExecutor`
- `action/data/`
  - deterministic timer/alarm parsers
  - ML Kit Prompt API parser
  - Android clock executor
- `voiceaction/ui/`
  - Compose screen
  - UI state
  - ViewModel

### Main flow

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

The app uses two on-device strategies for intent resolution:

### 1. Native speech recognition

Speech transcription uses Android's built-in:

- `android.speech.SpeechRecognizer`
- `RecognizerIntent`

The app requests partial results and prefers offline recognition where available.

### 2. Structured action parsing

The app supports two parser paths:

- **Primary path:** ML Kit Prompt API with on-device Gemini Nano when available
- **Fallback path:** deterministic local parsing for timers and alarms

This gives the app a graceful degradation model:

- supported devices get on-device LLM-based resolution
- unsupported devices still work fully on-device via the rule-based parser

That fallback path is especially important because Prompt API availability depends on device support.

## Prompt Engineering

The LLM parser is intentionally constrained.

Instead of allowing free-form generation, it is asked to return strict JSON for a small schema:

```json
{
  "intent": "SET_TIMER" | "SET_ALARM" | "UNKNOWN_ACTION",
  "duration_seconds": number | null,
  "hour": number | null,
  "minute": number | null,
  "meridiem": "AM" | "PM" | null,
  "timezone": string | null,
  "label": string | null,
  "confidence": number
}
```

Important design choices:

- the prompt supports only a small set of intents
- the output must be JSON only
- the response is validated in code before use
- when model output is incomplete or malformed, the app falls back to deterministic parsing

I also added defensive logic around model output because small on-device models can under-specify details like:

- `PM` vs `AM`
- labels
- timezone normalization

## Fallback Parser Design

The fallback parser is not just a keyword matcher.

It supports:

- timer phrases
  - `set a timer for 10 minutes`
  - `wake me in 20 minutes`
  - `remind me in 15 minutes to check the oven`
- alarm phrases
  - `set an alarm for 7 AM`
  - `wake me up at 6`
  - `remind me at 7 PM to call my friend`
  - `set alarm to remind me about doing task at 5 PM today`

The fallback path also handles:

- 12-hour to 24-hour normalization
- `AM` / `PM` / `a.m.` / `p.m.` variants
- alarm labels before or after the time
- basic timezone-aware alarm conversion

## Action Execution

The app executes actions via Android public Clock intents:

- timers: `AlarmClock.ACTION_SET_TIMER`
- alarms: `AlarmClock.ACTION_SET_ALARM`

This keeps the final action execution native, reliable, and on-device.

## Device Support Notes

ML Kit Prompt API support is device-dependent.

I didn't have a supported personal device to fully test the Gemini Nano path, so I focused on strengthening the fallback on-device flow.

On unsupported devices, the app still works through the fallback parser, which means the full speech-to-action flow remains usable without network access.

In practice:

- Prompt API may not be available on all phones
- the fallback parser is the compatibility path
- both paths remain fully on-device

This was an intentional product decision: the app should still feel complete even when the on-device LLM path is unavailable.

## UX Decisions

Some of the UI decisions were made specifically to make the app feel more like a product than a parser demo:

- a hero mic interaction instead of a text-box-first screen
- live listening animation
- a visible model status state
- collapsible technical details in debug mode only
- encouraging animated permission state
- follow-up suggestions after action execution

## Testing

The project includes unit coverage around:

- timer parsing
- alarm parsing
- model JSON parsing
- LLM parser fallback behavior
- ViewModel state transitions
- timezone conversion

Recent local verification:

```bash
./gradlew assembleDebug testDebugUnitTest
```

## Limitations

- Prompt API availability depends on device support
- Android public APIs support creating timers and alarms well, but not rich control over existing running timers
- speech transcript quality can vary by device and installed speech services
- the current app is intentionally narrow and does not attempt broad multi-domain assistant behavior

## If I Had More Time

- add a clarification flow for incomplete commands
  - example: `set a timer` -> `for how long?`
- polish alarm phrasing coverage even further
- add a small recent-actions history
- refine release/demo assets and walkthrough video

## How To Run

1. Open the project in Android Studio.
2. Run on a physical Android device or emulator.
3. Grant microphone permission.
4. Tap the mic and speak a timer or alarm command.
5. Tap `Run Action` to launch the Clock intent.

## Example Phrases

- `Set a timer for 10 minutes for pasta`
- `Wake me in 20 minutes`
- `Set an alarm for 7 AM`
- `Wake me up at 5:30 PM today`
- `Set alarm to remind me about doing task at 5 PM today`

## Code Walkthrough

Key files:

- [MainActivity.kt](./app/src/main/java/com/example/voicetotext/MainActivity.kt)
- [AndroidSpeechRecognizer.kt](./app/src/main/java/com/example/voicetotext/speech/data/AndroidSpeechRecognizer.kt)
- [LlmVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/LlmVoiceActionParser.kt)
- [TimerVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/TimerVoiceActionParser.kt)
- [AlarmVoiceActionParser.kt](./app/src/main/java/com/example/voicetotext/action/data/AlarmVoiceActionParser.kt)
- [AndroidVoiceActionExecutor.kt](./app/src/main/java/com/example/voicetotext/action/data/AndroidVoiceActionExecutor.kt)
- [VoiceActionViewModel.kt](./app/src/main/java/com/example/voicetotext/voiceaction/ui/VoiceActionViewModel.kt)
- [VoiceActionScreen.kt](./app/src/main/java/com/example/voicetotext/voiceaction/ui/VoiceActionScreen.kt)
