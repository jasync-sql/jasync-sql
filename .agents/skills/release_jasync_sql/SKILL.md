---
name: release-jasync-sql
description: Executes a jasync-sql release across git branches, Sonatype Maven Central CD, GitHub Releases, README snippets, and next-version preparation following RELEASING.md. Use when asked to release, cut, or publish a new version of jasync-sql.
---

# Release `jasync-sql`

Follow the canonical release runbook in [RELEASING.md](../../../RELEASING.md) step by step.

## Agent Execution Rules & Approval Gates

1. **Read [RELEASING.md](../../../RELEASING.md) first** to confirm exact branch names, file locations, and command flags.
2. **Execute local preparation autonomously (Steps 1–3)**:
   - Fetch `origin --prune --tags`, fast-forward local `master` to `origin/master`, verify recent `CI` workflow runs on `master` passed, confirm `version = "X.Y.Z"` in `build.gradle.kts`, and rebase local `github-release` onto `master`.
   - Run `./.agents/skills/release_jasync_sql/scripts/check_cves.sh` to audit all published runtime/compile dependencies in `gradle.properties` against the GitHub Advisory Database. **Halt and report to the user** before releasing if any open advisory is found.
   - Preview release notes via `gh api --method POST repos/jasync-sql/jasync-sql/releases/generate-notes` (`target_commitish=master` or `github-release`) and prepare a concise curated draft (preserving feature/fix/doc PRs and `New Contributors`).
3. **Pause for explicit user approval before irreversible remote actions** (unless the user already approved them in the prompt):
   - **Gate 1 — Push `github-release` (`Step 4`)**: Confirm before running `git push origin github-release`, since this triggers `.github/workflows/cd.yml` to publish all 6 `jasync-*` artifacts (`X.Y.Z`) to Sonatype Maven Central.
   - **Gate 2 — Publish GitHub Release (`Step 5`)**: Show the curated release notes for confirmation/edits before running `gh release create "X.Y.Z" --repo jasync-sql/jasync-sql --target github-release --title "X.Y.Z" --latest ...` (always pass `--target github-release` so tag `X.Y.Z` points to the `X.Y.Z` release commit rather than the `N+1` bump commit).
4. **Verify CD & finish `master` preparation (`Steps 4 & 6`)**:
   - Monitor the `CD` workflow run on `github-release` (`gh run list --repo jasync-sql/jasync-sql --branch github-release --workflow cd.yml --limit 1`; note Fine-Grained PATs do not support `gh run watch`, so poll `gh run view <run_id> --json status,conclusion` via `schedule` if using a Fine-Grained PAT) to confirm Maven Central publishing succeeded.
   - Switch back to `master`, update `README.md` Maven/Gradle version snippets (`jasync-mysql` and `jasync-postgresql`) to `X.Y.Z`, bump `build.gradle.kts` to `X.Y.(Z+1)`, commit, and push directly to `origin/master` (confirming before `git push` if not already approved).
