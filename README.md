# WatchMind

A minimal Wear OS chat app that talks to OpenAI-compatible APIs.

## Features
- Chat UI optimized for Wear OS
- Settings screen for API key, base URL, and model
- Uses OpenAI-compatible `/chat/completions` endpoint

## Setup
1. Open the project in Android Studio.
2. Run the app on a Wear OS device or emulator.
3. Open **Settings** in the app to save:
   - API Key (e.g. `sk-...`)
   - Base URL (default `https://api.openai.com/v1`)
   - Model (e.g. `gpt-4o-mini`)
4. Go back to **Chat** and start sending messages.

## Notes
- The app stores settings in `SharedPreferences` on-device.
- The OpenAI-compatible API must support the `chat/completions` route.
