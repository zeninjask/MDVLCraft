# Release rules

- Every rebuild of the packs gets a new version: bump the patch number (1.9.23 → 1.9.24) and use it for all three `.mrpack` files (client, client-performance, server), the skill-tree PDF, the Java argument files and the README links. Never rebuild under an existing version.
- After every rebuild, push the client, client-performance and server packs, the client Java arguments, the server `user_jvm_args.txt`, the skill-tree PDF and the README to `main`, replacing the previous version's files there.

# Skills

Always use these skills, in every session, without being asked:

- **caveman**: write all replies, summaries and status updates in caveman style (terse, no filler). Code, commit messages and file contents stay normal.
- **RTK**: run shell commands through `rtk` (e.g. `rtk git status`, `rtk grep ...`) so command output is compressed. If `rtk` isn't installed, run commands normally.
- **ponytail**: take the smallest change that fully solves the task, keep replies short and plain, and end with what was skipped and any risk.
- **graphify**: for any question about the codebase, its structure or how files relate, use graphify first (build or refresh `graphify-out/` if it's missing or stale) before reading files by hand.

# Working rules

- While a long task runs (tests, builds), check its progress every 5 minutes and report it.

# Terms

- **Execute / execution / assassinate / assassination** always means Combat Evolution's execution: right click while behind a target that hasn't noticed you, for a big-damage attack (its damage carries the `combat_evolution` EXECUTION damage type tag). It never means Epic Fight Nightfall's Execution skill unless the user names Nightfall.

# Testing without running out of memory

The sandbox has about 14 GB in total; a Forge server plus a client plus Gradle goes over it and the kernel kills one of
them (it was the client, and once the shell). Step by step:

1. Only test in game when the user asks for it. Otherwise build (`./gradlew -q build --offline`) and stop.
2. Before a test, stop Gradle: `cd binder && ./gradlew --stop`, and check `free -g` shows at least 10 GB available.
3. Server alone first, with `-Xmx4G` written into the test server's `user_jvm_args.txt` *after* the pack is copied in
   (the pack's own file sets 8 GB and overwrites it).
4. Client with `-Xmx3G`, and only one client. Never a second server or client at the same time. The test client's
   real size is about 9 GB whatever its -Xmx (software rendering keeps textures in RAM), so with the server at 4 GB the
   machine is full: in the test client only, use no resource packs, `mipmapLevels:0`, render distance 4, and move
   heavy client-only mods (shaders, particles, ambient sound) out of its mods folder. Keep Distant Horizons: the binder
   requires it.
5. Watch memory every 5 minutes (`free -g`, `ps -o rss,cmd -p <pid>`); if available memory drops under 1.5 GB, stop
   the client first.
6. Stop processes by PID (`kill <pid>`), never with `pkill -f` / `pgrep -f` on a pattern that appears in your own
   command line (e.g. a script name): it matches your own shell, kills it, or makes a wait loop never end.
7. After the test, kill both and delete their worlds and logs you no longer need.
8. Build the binder with `./gradlew clean build` before a release and check the jar contains `mdvlcraft.refmap.json`;
   an incremental build once dropped it, and every binder mixin then failed at startup.
9. A release must boot a server before it is pushed: run the server alone on the new mods once.
