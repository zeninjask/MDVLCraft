# Release rules

- Every rebuild of the packs gets a new version: bump the patch number (1.9.23 → 1.9.24) and use it for all three `.mrpack` files (client, client-performance, server), the skill-tree PDF, the Java argument files and the README links. Never rebuild under an existing version.
- After every rebuild, push the client, client-performance and server packs, the client Java arguments, the server `user_jvm_args.txt`, the skill-tree PDF and the README to `main`, replacing the previous version's files there.
- Always talk like a caveman.
- Always use the ponytail skill.
- Use graphify first for code questions.
