# Changelog

## [Unreleased]

- (None.)

## [1.0.10] - 2026-06-23

- General improvements to analyzers.

## [1.0.9] - 2026-06-20

- Auto-activation: opening a solution now offers to add the analyzer so findings appear as you type. Toggle under Settings > Tools > Qualimetry C#.
- Clarified wording: the plugin provides real-time analysis once provisioned, run by Rider's bundled Roslyn host.

## [1.0.8] - 2026-06-19

- JetBrains Marketplace builds are now code-signed.

## [1.0.7] - 2026-06-18

- Five new analyzer rules plus extended readonly-array coverage (210 rules total); synced from the SonarQube plugin release.

## [1.0.6] - 2026-06-18

- Six new analyzer rules (205 rules total); synced from the SonarQube plugin release.

## [1.0.5] - 2026-06-18

- Configurable rules now expose native SonarQube rule parameters (editable in the UI and synced to the IDE in connected mode) instead of static documentation.

## [1.0.4] - 2026-06-17

- Aligned the SonarQube rule lookup to the `qualimetry-csharp` rule namespace so synced severities match the updated server plugin.

## [1.0.3] - 2026-06-17

- Settings are now namespaced under `csharpAnalyzer.*` (previously `qualimetry.*`). Update any existing settings to the new keys.

## [1.0.2] - 2026-06-17

- Version-alignment release to keep the C# plugin family on a single version.

## [1.0.1] - 2026-06-17

- Add the Qualimetry plugin icon so it displays in the Plugins list and on the marketplace page.

## [1.0.0] - 2026-06-17

First general release.

- Add the `Qualimetry.CSharp.Analyzer` package to a selected `.csproj` from the Tools and project context menus.
- Sync the active rule set and severities from a SonarQube quality profile into a solution `.globalconfig`.
- Settings panel for the SonarQube URL, token, and profile name under Settings > Tools > Qualimetry C#.
- Rule keys and severities align with the SonarQube plugin and the VS Code extension.

## [0.1.0] - 2026-06-17

First release.

- Add the `Qualimetry.CSharp.Analyzer` package to a selected `.csproj` from the Tools and project context menus.
- Sync the active rule set and severities from a SonarQube quality profile into a solution `.globalconfig`.
- Settings panel for the SonarQube URL, token, and profile name under Settings > Tools > Qualimetry C#.
- Rule keys and severities align with the SonarQube plugin and the VS Code extension.
