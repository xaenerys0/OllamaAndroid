# Ollama Chat for Android

A native Android chat app for [Ollama Cloud](https://ollama.com/cloud) — think of the Claude or
ChatGPT apps, but powered by your own Ollama API key. It also works against any self-hosted
Ollama server.

## Features

- **Streaming chat** — assistant replies stream in token by token over Ollama's NDJSON chat API,
  with a stop button to interrupt generation (partial replies are kept)
- **Conversation history** — chats are stored locally in a Room database and browsable from a
  navigation drawer; deleting a conversation removes its messages
- **Model picker** — the top-bar dropdown lists the models available to your API key
  (via `GET /api/tags`) and lets you switch models per message
- **Reasoning control** — for models that report the `thinking` capability (via
  `POST /api/show`), a brain icon next to the message box lets you pick the reasoning level
  (Default / Off / On / Low / Medium / High); the model's thinking trace streams into a
  collapsible section of the reply bubble and is saved with the chat
- **Markdown-aware rendering** — fenced code blocks (with language label and horizontal scroll),
  inline code, bold, and italics render nicely in assistant bubbles
- **Settings** — API key (masked, stored on-device only), server URL (defaults to Ollama Cloud,
  point it at `http://your-host:11434` to self-host), and a connection test
- **Material 3 / Material You** — dynamic color on Android 12+, light and dark themes,
  edge-to-edge UI

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| Architecture | MVVM with `StateFlow`, unidirectional data flow |
| Persistence | Room (KSP) for chats, DataStore Preferences for settings |
| Networking | OkHttp with streaming NDJSON parsing, kotlinx.serialization |
| Build | Gradle version catalog, AGP 8.10, Java 17 toolchain |

## Getting started

1. Open the project in Android Studio (Meerkat or newer) and let Gradle sync,
   or build from the CLI:

   ```sh
   ./gradlew :app:assembleDebug
   ```

2. Install the APK on a device or emulator (Android 8.0 / API 26+).

3. Create an API key at **ollama.com → Settings → API Keys**.

4. In the app, open **Settings**, paste the key, tap **Test connection**, then **Save**.

5. Pick a model from the top-bar dropdown (e.g. `gpt-oss:120b`) and start chatting.

## Project layout

```
app/src/main/java/com/ollamaandroid/app/
├── OllamaApplication.kt        # manual DI container
├── MainActivity.kt             # NavHost (chat ⇄ settings)
├── data/
│   ├── network/                # OkHttp client + API DTOs (streaming NDJSON)
│   ├── db/                     # Room entities, DAO, database
│   ├── ChatRepository.kt       # chat orchestration
│   └── SettingsRepository.kt   # DataStore-backed settings
└── ui/
    ├── chat/                   # chat screen + view model (streaming state)
    ├── settings/               # settings screen + view model
    ├── components/             # lightweight markdown renderer
    └── theme/                  # Material 3 theme with dynamic color
```

## Notes on the API

The app talks to the standard Ollama HTTP API:

- `POST /api/chat` with `"stream": true` — responses arrive as one JSON object per line;
  each chunk carries a `message.content` delta until `"done": true`
- `GET /api/tags` — lists models available to the key
- Authentication uses `Authorization: Bearer <api key>` (ignored by unauthenticated
  self-hosted servers)

The API key is stored in the app's private DataStore and never leaves the device except in the
`Authorization` header to the server you configure.
