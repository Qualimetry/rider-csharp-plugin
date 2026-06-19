# Qualimetry C# - Rider Plugin

[![CI](https://github.com/Qualimetry/rider-csharp-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/Qualimetry/rider-csharp-plugin/actions/workflows/ci.yml)

A thin JetBrains Rider plugin for the Qualimetry C# rule set. It does not analyse code itself; it wires the Qualimetry analysis engine into your solution so Rider's bundled Roslyn host reports Qualimetry findings as you work.

Powered by the same analysis engine as the [Qualimetry C# plugin for SonarQube](https://github.com/Qualimetry/sonarqube-csharp-plugin) and the [Qualimetry C# extension for VS Code](https://github.com/Qualimetry/vscode-csharp-plugin).

## What it does

- **Add C# Analyzer To Project** - adds the `Qualimetry.CSharp.Analyzer` NuGet `PackageReference` to a selected `.csproj`, so Rider's Roslyn host runs the analyzer.
- **Sync Rules From SonarQube Profile** - reads the active rules and severities from a SonarQube quality profile and writes a `.globalconfig` at the solution root, so the locally enabled rule set matches your server profile.

Analysis is performed by the Roslyn analyzer once the NuGet is referenced; this plugin only provisions and configures. Configure the SonarQube URL, token, and profile name under **Settings > Tools > Qualimetry C#**.

## Rule set

The rule set covers **210 C# rules** across eight categories:

| Category | Rules |
| --- | ---: |
| Code Quality | 109 |
| Style | 45 |
| Metrics | 17 |
| Naming | 16 |
| Reliability | 10 |
| Unity | 8 |
| Contract | 3 |
| Interop | 2 |
| **Total** | **210** |

Rule keys and severities align with the SonarQube plugin and the VS Code extension, so findings are directly comparable across CI and both editors.

## Compatibility

- **JetBrains Rider 2024.2** or later (build range 242 to 251.*).

## Also available

- **[SonarQube plugin](https://github.com/Qualimetry/sonarqube-csharp-plugin)** - rules and quality profiles for your CI quality gate.
- **[VS Code extension](https://github.com/Qualimetry/vscode-csharp-plugin)** - real-time analysis as you type.

## Building from source

Requires JDK 17+.

```bash
./gradlew buildPlugin
```

The packaged plugin ZIP is at `rider-plugin/build/distributions/`.

## Contributing

Issues and feature requests are welcome. This project does not accept pull requests, commits, or other code contributions from third parties; the repository is maintained by the Qualimetry team only.

## License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Copyright 2026 SHAZAM Analytics Ltd
