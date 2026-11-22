# METAL CHESS V2 — Implementation Plan

## Product direction
- Release identity: METAL CHESS V2 — The Metallic Edition.
- Design-first, fully offline Android chess application.
- Preserve the existing gold Staunton chess-piece artwork as the default and original visual identity.
- Improve how those exact assets fit inside board squares. Do not replace, redraw, recolor, or regenerate the existing piece collection as part of the fit work.
- Preserve the working V1 chess rules, Stockfish integration, difficulty levels, game review, coach mode, sounds, and local gameplay.

## V2 branch
Development branch: `v2-design-foundation`  
Release version target: `2.0.0`

## Phase 1 — Visual foundation (active)
- [x] Create an isolated V2 development branch from `main`.
- [x] Set Android version metadata to 2.0.0 / versionCode 2.
- [x] Improve original bitmap piece fitting by cropping transparent padding, preserving source aspect ratio, centering each piece, and limiting its visible bounds to a safe area inside its square.
- [ ] Redesign the home dashboard with premium graphite surfaces and restrained metallic-gold accents.
- [ ] Redesign the game screen hierarchy, status area, controls, and move information without changing chess behavior.
- [ ] Establish reusable typography, spacing, panel, button, and icon styles.

## Phase 2 — Board and gameplay polish
- [ ] Refine board proportions and safe margins across phone sizes and orientations.
- [ ] Verify piece centering and visual scale for all 12 existing assets.
- [ ] Improve last-move, selected-square, legal-move, check, and coach highlights.
- [ ] Add distinct castling and promotion animation handling.
- [ ] Improve captured-piece display and move-history presentation.
- [ ] Add local timed games and Pass-and-Play clock controls.

## Phase 3 — Analysis studio
- [ ] Add live Stockfish evaluation and evaluation bar.
- [ ] Add best-move arrows and multi-line principal variations.
- [ ] Build move classification and accuracy calculations from engine evaluations.
- [ ] Improve replay controls and analysis navigation.
- [ ] Support PGN import/export and saved analysis.

## Phase 4 — Coach and training centre
- [ ] Improve coach explanations using engine evaluations and position context.
- [ ] Add bundled offline tactical puzzles and solution validation.
- [ ] Add checkmate, fork, pin, skewer, discovered-attack, opening-principle, and endgame lessons.
- [ ] Track local training progress.

## Phase 5 — Player profile and statistics
- [ ] Add local player profiles and rating history.
- [ ] Track wins, losses, draws, engine difficulty performance, and achievements.
- [ ] Add searchable game history and saved-game management.

## Phase 6 — Customization and release
- [ ] Add board material/color themes while retaining the current gold pieces as the default.
- [ ] Add sound, animation, board orientation, and piece-size preferences.
- [ ] Verify every core feature in airplane mode.
- [ ] Test small and large screens, state restoration, and long sessions.
- [ ] Run Android CI and install-test the final APK before release.

## Non-negotiable compatibility rules
1. Do not remove or weaken Stockfish or existing legal chess behavior.
2. Do not replace the current metallic gold pieces.
3. Keep essential gameplay and training data local/offline.
4. Build and verify each milestone before calling it complete.
5. Keep V1 `main` stable; develop V2 on its own branch until ready.
