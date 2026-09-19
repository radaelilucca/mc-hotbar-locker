# Hotbar Locker

Multiloader Minecraft mod scaffold for Fabric, Forge, and NeoForge, targeting Minecraft 1.21.1 and Java 21.

The functional design is in [PRD.md](PRD.md). This initial base intentionally contains no inventory interception: it proves the build, metadata, loader entry points, shared source set, resource layout, and Mixin configuration before Phase 1 changes gameplay behavior.

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
