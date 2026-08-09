# Changelog

## 3.1.0 - Eight-Version Coverage + Pipeline Fixes

### Added

- MixinExtras 0.5.4 dependency (self-contained via `include`)
- .editorconfig for consistent code style
- Dependabot tracking for GitHub Actions updates
- MC 26.1.2 support restored as `version-26_1_2` (v3.0.0 feature set, version `3.1.0-26.1.2`)
- New `version-26_2` subproject for MC 26.2 (version `3.1.0-26.2`); the HUD
  extraction moved from `Gui.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` to
  `Gui.extractRenderState(DeltaTracker, boolean, boolean)`, so the HUD mixin now targets
  `Hud.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` which kept the old signature
- Weapons display: spear (stab) hits are tracked and shown on the hit/average displays on 26.1.2
- Shadow toggle and shadow color options on 1.18.2 and 1.19.4 (were always-on black shadows)
- Hit/average display mode (number / blocks / M) now applies on 1.20.1
- 26.1.2 build: MC 26.1+ ships unobfuscated, so the subproject uses the no-remap loom plugin without a mappings line
- Full 1.21.x coverage: new `version-1_21_1` (1.21.1-1.21.4) and `version-1_21_5` (1.21.5) subprojects;
  1.21.x jars now declare exact supported game version ranges (`>=1.21.1 <=1.21.4` / `>=1.21.5 <=1.21.5` /
  `>=1.21.6 <=1.21.11` — the client API broke at 1.21.5 and 1.21.6)
- Per-subproject `game_versions` publish matrix on Modrinth + CurseForge — no duplicate or
  over-claimed game versions (1.20.1 bounded to <=1.20.6, 26.1.2 bounded to <=26.1.2)
- Publish dependency metadata: Modrinth now declares fabric-api and midnightlib as
  required, modmenu as optional — launchers auto-install them for players (backfilled onto
  the v3.0.0 and shipped with all 3.1.0 versions)
- Quilt builds fixed: each subproject now applies quilt-loom 1.15.1 (`-PuseQuilt=true`) instead of
  fabric-loom, which cannot remap quilted-fabric-api (no `quilt.mod.json` support); the quilt build
  was broken since it was introduced. Mixin API pulled in explicitly (quilt-loader does not bundle
  it). 26.1.2 quilt build uses fabric-api directly (no quilted-fabric-api for 26.x) and declares
  `fabric_api` in quilt.mod.json. CI now builds both loaders for all 8 subprojects.

### Infrastructure

- CI now builds each subproject separately (fail-fast disabled)
- Release workflow builds all versions, collects JARs, creates GitHub Release with changelog
- Changelog follows KeepAChangelog format for automated extraction
- CI and release workflows now build all 8 subprojects (26.x on JDK 25); CI triggers on `restructure`
- Modrinth dependency metadata backfilled retroactively on the published v3.0.0 versions
  (fabric-api + midnightlib required, modmenu optional)

### Fixed

- 26.1.2: ray-hit-point distance no longer records hits against the wrong entity when the crosshair target differs from the attacked entity
- 26.1.2: `fabric.mod.json` depended on the non-existent `fabric-api` mod id instead of `fabric`
- All subprojects: `quilt.mod.json` license metadata said GPL-3.0 while v3.0.0+ is All Rights Reserved
- 26.1.2: hit distance no longer stays on screen forever — keep-last-distance and reset-time settings apply like on other versions
- 26.1.2/26.2: `fabric.mod.json` depends on `fabric-api` (the real mod id on 26.x) instead of the
  `fabric` alias, which 26.x fabric-api jars no longer provide — the mod would not start otherwise
- Release pipeline: Modrinth publish skip-check always evaluated to "version exists" and silently
  skipped every publish (the version-list endpoint ignores the `?version_number` query parameter);
  the check now filters versions client-side
- Release pipeline: changelog extraction used a broken sed idiom that never looped and produced
  headers plus one line (v3.0.0 shipped with empty release notes); extraction now reads the first
  `## ` section body directly
- Release pipeline: CurseForge publish used `curseforge-relations`, an input name that only
  existed in mc-publish v2 — v3 ignores it, so the published files carry no dependency relations;
  renamed to `curseforge-dependencies` with the v3 `slug(type)` syntax (3.1.0 files on CurseForge
  are unaffected; the fix applies from the next release)

## 3.0.0 — Multi-Version Restructure + Entity Filter

### Major Changes

- **License change** — Switched from GPL-3.0 to **All Rights Reserved (ARR)** for v3.0.0 and later. Old versions remain under GPL-3.0. Commercial redistribution and publishing forks under a different name are now explicitly prohibited.
- **Multi-project restructure** — Split into 4 version-specific subprojects:
  - `version-1_17_1_18` (MC 1.18.2)
  - `version-1_19` (MC 1.19.4)
  - `version-1_20_1` (MC 1.20.1)
  - `version-1_21_11` (MC 1.21.11)
- **Entity filter system** — New filter/EntityFilterHelper with whitelist/blacklist mode, category toggles (Players, Hostile, Passive, Boss, Other), and custom entity ID matching
- **Unified build config** — Shared Gradle configuration via root `build.gradle` (processResources, sourcesJar, publishing)

### Entity Filter

- Config entries: `entityFilterEnable`, `entityFilterMode` (WHITELIST/BLACKLIST), per-category toggles, `entityFilterCustomIDs`
- All 3 HUD display sections filtered with `!enable || shouldTrack(entity)`
- Works across all 7 supported versions

### Build System

- Root `build.gradle` with `subprojects` block for common config
- Added Fabric maven repository to all subprojects
- `org.gradle.parallel=true` for faster builds
- `--no-daemon` required (known daemon termination issue)

### Infrastructure

- `agents.md` — Project knowledge reference for AI-assisted development
- `.gitignore` — Added `*.ipr`, `*.iws`, `out/`, `*.log`, `Thumbs.db`, `.DS_Store`; fixed `/run/` → `run/` for subproject run dirs
- Community standards: CODE_OF_CONDUCT, CONTRIBUTING, SECURITY, issue & PR templates
- CI/CD: GitHub Actions (build, CodeQL, Scorecards, Dependabot auto-merge, stale issue management)
- Gradle updated to 9.5.0
- Java target updated to 21

### Fixes

- `run/` directories now properly excluded at subproject level
- IntelliJ workspace/project files (`.iws`, `.ipr`) excluded from version control

### Available Jars

| Version | File |
|---------|------|
| 1.18.2  | `reach_display-3.0.0-1.18.2.jar` |
| 1.19.4  | `reach_display-3.0.0-1.19.4.jar` |
| 1.20.1  | `reach_display-3.0.0-1.20.1.jar` |
| 1.21.11 | `reach_display-3.0.0-1.21.11.jar` |
