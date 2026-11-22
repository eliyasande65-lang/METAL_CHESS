# METAL CHESS

Native Android chess application, built with Kotlin and a small C++17 NDK core.

## METAL CHESS V2

V2 is being developed on the dedicated `v2-design-foundation` branch. It is a design-first, fully offline upgrade that preserves the working V1 chess foundation and the existing metallic gold Staunton pieces.

### V2 principles
- Keep the current gold chess-piece collection; improve fit, centering, and scale without replacing the artwork.
- Premium graphite interface with restrained metallic-gold accents.
- Preserve Stockfish, legal move handling, and existing gameplay.
- Keep essential features, profiles, saved games, and training data on-device.
- Build and verify each milestone before marking it complete.

See [V2_IMPLEMENTATION_PLAN.md](V2_IMPLEMENTATION_PLAN.md) for the phased scope and acceptance criteria.

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
Pushes to `main` and `v2-design-foundation` trigger Android debug builds. The APK outputs are uploaded as the workflow artifact `metal-chess-debug-apks`.

## Existing foundation
- Legal move generation and validation through Chesslib
- Interactive board with legal-move highlights
- Original CC0 3D Staunton models rendered to transparent metallic piece images during CI
- Offline synthesized piece-drop, capture and check sounds with an in-game toggle
- Local two-player play
- Stockfish engine and difficulty settings
- Check, checkmate and draw status
- Board flip and new-game controls
- Game saving and replay
- Android NDK C++17 library target

V2 development is incremental. Features in the implementation plan are not considered complete until implemented and tested.
