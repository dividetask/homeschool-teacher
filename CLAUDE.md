# CLAUDE.md

Guidance for Claude Code when working in this repository.

## After pushing — always check the build

- A background job builds this branch automatically and commits the
  result to `build-logs/build.log` on the branch within ~15 minutes of a
  push. This environment has **no Android SDK**, so you cannot compile
  locally — the build log is how you verify your changes compile.
- **Every time** you finish work that pushed commits, schedule a one-shot
  check ~15 minutes later with `CronCreate` (`recurring: false`) whose
  prompt fetches the branch, reads `build-logs/build.log`, and acts on the
  result:
  - build **FAILED** for the newest code commit → diagnose from the Gradle
    output, fix, commit, push, then schedule another check ~15 min out.
  - build **SUCCEEDED** → nothing to do.
  - newest code commit **not built yet** (log's `from <sha>` is behind the
    tip) → schedule another check ~15 min out.
- Cron jobs are session-only, so re-arm this after each push rather than
  relying on a single standing job.

## Asking questions — IMPORTANT

- **Never use the interactive question/multiple-choice prompt tool**
  (`AskUserQuestion`) — the user dislikes it.
- When you need to ask the user something, **write the question(s) as
  plain text in the chat**, then **stop and wait** for the user's reply.
- Do not bundle a question with a pile of options to click. Just ask
  clearly in prose, then end your turn.
