# Dedicated server regression checks

Run `verifyScaffold` to build all loaders, run the unit tests, and execute the
NeoForge 1.21.1 dedicated-server bootstrap check. PR and release workflows
already call this task.

- `DedicatedServerClassloadingTest` checks the compiled NeoForge entry point,
  common event subscriber, and network classes for physical-client references,
  including references inside generated lambda methods. A negative control
  verifies that it rejects the actual client HUD class.
- `:neoforge:runServerSmoke` uses the real dedicated loader and its dist cleaner.
  It passes `--world=` so Minecraft exits after mod initialization, before opening
  a world or network port. The expected `Invalid world directory specified` log
  is the termination marker, not a test failure. Both this marker and the mod
  bootstrap marker are required; startup exceptions fail the task even if
  Minecraft returns exit code zero. A three-minute timeout bounds the run.

Do not replace the smoke arguments with `--initSettings`: NeoForge skips mod
construction with that option, which would hide this regression.

Logs are in `neoforge/build/server-smoke/logs/`. This is a startup regression
check, not a multiplayer gameplay or all-modpack compatibility test.
