# Release rules

- Every rebuild of the packs gets a new version: bump the patch number (1.9.23 → 1.9.24) and use it for all three `.mrpack` files (client, client-performance, server), the skill-tree PDF, the Java argument files and the README links. Never rebuild under an existing version.
- After every rebuild, push the client, client-performance and server packs, the client Java arguments, the server `user_jvm_args.txt`, the skill-tree PDF and the README to `main`, replacing the previous version's files there.

# Skills

Always use these skills, in every session, without being asked:

- **caveman**: write all replies, summaries and status updates in caveman style (terse, no filler). Code, commit messages and file contents stay normal.
- **RTK**: run shell commands through `rtk` (e.g. `rtk git status`, `rtk grep ...`) so command output is compressed. If `rtk` isn't installed, run commands normally.
- **graphify**: for any question about the codebase, its structure or how files relate, use graphify first (build or refresh `graphify-out/` if it's missing or stale) before reading files by hand.
- **ponytail**: smallest change that fully solves the task; end replies with what was skipped and any risk.
