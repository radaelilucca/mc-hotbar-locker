# Hotbar Locker

Multiloader Minecraft mod for Fabric, Forge, and NeoForge, targeting Minecraft 1.21.1 and Java 21.

The functional design is in [PRD.md](PRD.md).

## Automation

- Pull requests to `main` run the full automated verification suite.
- Pushes to `main` create a semantic version tag from Conventional Commit messages.
- A `vX.Y.Z` tag builds the Fabric, Forge, and NeoForge artifacts and attaches them to a GitHub release as `hotbarlocker-X.Y.Z-fabric.jar`, `hotbarlocker-X.Y.Z-forge.jar`, and `hotbarlocker-X.Y.Z-neoforge.jar`.

## Requirements

- Java 21
- Windows: `C:\Users\lucca\.jdks\graalvm-jdk-21.0.12+7.1` is the validated local JDK.

## Validate the scaffold

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-hotbarlocker"
.\gradlew.bat '-Dorg.gradle.java.home=C:/Users/lucca/.jdks/graalvm-jdk-21.0.12+7.1' --no-daemon verifyScaffold --console=plain
```

## Development clients

```powershell
.\gradlew.bat '-Dorg.gradle.java.home=C:/Users/lucca/.jdks/graalvm-jdk-21.0.12+7.1' --no-daemon :fabric:runClient
.\gradlew.bat '-Dorg.gradle.java.home=C:/Users/lucca/.jdks/graalvm-jdk-21.0.12+7.1' --no-daemon :forge:runClient
.\gradlew.bat '-Dorg.gradle.java.home=C:/Users/lucca/.jdks/graalvm-jdk-21.0.12+7.1' --no-daemon :neoforge:runClient
```

Each command owns its run directory under the corresponding loader module. Do not launch the three clients concurrently against the same Gradle user home.

## Layout

```text
common/    Loader-neutral policy/API source and shared resources
fabric/    Fabric entry points, metadata, and client adapter
forge/     Forge entry points, metadata, and client adapter
neoforge/  NeoForge entry points, metadata, and client adapter
```

The `common` source directory is compiled into each loader artifact with Mojang mappings. This avoids a runtime abstraction dependency while keeping the implementation shared.
