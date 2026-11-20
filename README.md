# METAL CHESS

Native Android chess application, built with Kotlin and a small C++17 NDK core.

## Build targets
- Android API 23+
- ARMv7 (32-bit): `armeabi-v7a`
- ARM64: `arm64-v8a`

## Build locally
Install Android Studio, Android SDK 35, NDK 27.2.12479018, CMake 3.22.1 and Gradle 8.9. From the project root:

```bash
gradle assembleDebug
```

## GitHub Actions
Every push to `main` triggers an Android debug build. The APK outputs are uploaded as the workflow artifact `metal-chess-debug-apks`.

## Current foundation
- Legal move generation and validation through Chesslib
- Interactive board with legal-move highlights
- Original CC0 3D Staunton models rendered to transparent metallic piece images during CI
- Offline synthesized piece-drop, capture and check sounds with an in-game toggle
- Local two-player play
- Check, checkmate and draw status
- Board flip and new-game controls
- Android NDK C++17 library target

## Roadmap
- Engine opponent and difficulty settings
- Training and tactical puzzles
- Endgame lab with configurable material
- Move list, analysis and PGN export
- Premium metallic piece rendering and 3D board mode

This repository is being developed incrementally. Features listed in the roadmap are not represented as complete until implemented and tested.
