# METAL CHESS

A native Android chess application focused on a premium graphite-and-metal interface.

## Build targets
- Android API 23+
- ARMv7 (32-bit): `armeabi-v7a`
- ARM64: `arm64-v8a`

## Build
Open the project in Android Studio, or run:

```bash
./gradlew assembleDebug
```

GitHub Actions builds debug APKs for both supported ARM ABIs when changes are pushed to `main`.

## Planned modules
- Legal chess rules and move history
- Play against the engine
- Local two-player
- Training and puzzles
- Endgame practice
- Move analysis

The engine and feature set will be expanded in stages.