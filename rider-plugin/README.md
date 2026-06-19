# Qualimetry C# (Rider plugin)

A thin Rider (IntelliJ-platform) plugin for the Qualimetry C# rule set. It does not analyze code
itself. Rider's bundled Roslyn host runs the `Qualimetry.CSharp.Analyzer` once the NuGet is
referenced; this plugin only provisions that package and configures rule severities.

## What it does

1. Provisions the `Qualimetry.CSharp.Analyzer` NuGet into a selected C# project.
2. Syncs the active rule set from a SonarQube quality profile into a local `.globalconfig` at the
   solution root.

The Roslyn diagnostic id is the rule key in every channel: `QCS####`. There is no id-translation
layer. The SonarQube repository key is `roslyn.qualimetry-csharp`.

## Actions

Both actions are under Tools, and the provision action is also on the Project View context menu.

- **Qualimetry: Add C# Analyzer To Project** - select a `.csproj`, then run the action. If the
  project does not already reference `Qualimetry.CSharp.Analyzer`, it inserts:

  ```xml
  <ItemGroup>
    <PackageReference Include="Qualimetry.CSharp.Analyzer" Version="0.1.0" PrivateAssets="all" />
  </ItemGroup>
  ```

  If the reference is already present, the action reports that and makes no change.

- **Qualimetry: Sync Rules From SonarQube Profile** - calls the SonarQube web API using the
  configured URL and token, resolves the profile key, reads every active `qualimetry-csharp` rule
  for language `cs` (paged), and writes a `.globalconfig` at the solution root. Each active rule is
  mapped to a `dotnet_diagnostic.QCS####.severity` entry:

  | SonarQube severity        | .globalconfig severity |
  | ------------------------- | ---------------------- |
  | BLOCKER, CRITICAL, MAJOR  | warning                |
  | MINOR                     | suggestion             |
  | INFO                      | silent                 |

  The SonarQube calls used are:

  - `GET /api/qualityprofiles/search?language=cs` to resolve the profile key by name.
  - `GET /api/rules/search?activation=true&qprofile=<key>&languages=cs&repositories=qualimetry-csharp&ps=500&p=<n>`
    paged until all rules are read.

  Authentication is HTTP Basic: the token is the username and the password is empty.

## Settings

Settings > Tools > Qualimetry C# (persisted via a `PersistentStateComponent`):

- `sonarQubeUrl` - base URL of the SonarQube server, for example `https://sonar.example.com`.
- `token` - SonarQube user token, sent as the Basic auth username with an empty password.
- `profileName` - name of the quality profile to sync. Default `Qualimetry C#`.
- `analyzerVersion` - version of the analyzer NuGet to reference. Default `0.1.0`.

## Build

```
./gradlew buildPlugin
```

The IntelliJ-platform Gradle plugin downloads the Rider SDK (`type = RD`) from the JetBrains
download servers on first build, so the build requires network access to those servers. JDK 17 and
Kotlin JVM are used. The built distribution is in `build/distributions`.

## Boundary

This is a thin client. It never embeds analysis logic. All analysis is performed by the Roslyn
analyzer NuGet once it is referenced in a project.
