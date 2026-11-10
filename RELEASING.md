# Releasing `jasync-sql`

This document describes the step-by-step playbook for cutting a new `jasync-sql` release from `master`.

## Prerequisites

- Push access to `https://github.com/jasync-sql/jasync-sql`.
- Authenticated GitHub CLI (`gh auth status`) with `repo` / `contents:write` and `actions:read` permissions.
- Repository secrets configured on GitHub Actions used by [`.github/workflows/cd.yml`](.github/workflows/cd.yml):
  - Sonatype Central Portal User Token: `SONATYPE_USERNAME_2` and `SONATYPE_PASSWORD_2` (or `ORG_GRADLE_PROJECT_SONATYPEUSERNAME` / `ORG_GRADLE_PROJECT_SONATYPEPASSWORD`).
  - GPG signing credentials: `GPG_PRIVATE_KEY` (ASCII-armored private key) and `GPG_PRIVATE_PASSWORD` (or legacy `SIGNING_KEY_RING_FILE`, `SIGNING_KEY_ID`, `SIGNING_PASSWORD`).

---

## Standard Release Flow (`2.x` from `master`)

Assume `X.Y.Z` is the version being released (e.g., `2.2.5`), `PREV_TAG` is the previous release tag (e.g., `2.2.4`), and `NEXT_VERSION` is the next patch version (e.g., `2.2.6`).

### 1. Update `master` from Remote

Fetch all remote branches and tags, switch to `master`, fast-forward to `origin/master`, and ensure the working tree is clean:

```bash
git fetch origin --prune --tags
git checkout master
git merge --ff-only origin/master
git status --short
```

Verify recent CI checks on `master` passed:

```bash
gh run list --repo jasync-sql/jasync-sql --branch master --workflow ci.yml --limit 3
```

### 2. Verify `version` in `build.gradle.kts`

Inspect `version = "..."` in [`build.gradle.kts`](build.gradle.kts):

- If `version` is already `"X.Y.Z"`, proceed to Step 3.
- If `version` does not match `"X.Y.Z"`, update `build.gradle.kts`, commit, and push to `origin/master` (`chore: bump version to X.Y.Z`) **before** rebasing `github-release` so the release commit itself carries `version = "X.Y.Z"`.

### 3. Rebase `github-release` onto `master`

Check out `github-release` tracking `origin/github-release` and rebase onto `master`:

```bash
git checkout -B github-release --track origin/github-release
git rebase master
```

*(In normal mainline releases `origin/github-release` is an ancestor of `master`, so this is a clean fast-forward.)*

### 4. Push `github-release` to Remote (Triggers Maven Central CD)

> **Approval / Irreversible Action**: Pushing `github-release` immediately triggers [`.github/workflows/cd.yml`](.github/workflows/cd.yml) (`./gradlew clean build publishToSonatype closeAndReleaseSonatypeStagingRepository --no-daemon --no-parallel -x test`), publishing all 6 artifacts (`jasync-common`, `jasync-pool`, `jasync-mysql`, `jasync-postgresql`, `jasync-postgis-jts`, `jasync-r2dbc-mysql`) to Sonatype Maven Central.

```bash
git push origin github-release
```

Monitor the `CD` workflow run until completion:

```bash
gh run list --repo jasync-sql/jasync-sql --branch github-release --workflow cd.yml --limit 1
```

### 5. Generate Changelog & Publish GitHub Release

1. Preview GitHub's auto-generated release notes from `PREV_TAG` to `github-release`:

   ```bash
   gh api --method POST repos/jasync-sql/jasync-sql/releases/generate-notes \
     -f tag_name="X.Y.Z" \
     -f target_commitish="github-release" \
     -f previous_tag_name="PREV_TAG"
   ```

2. Review and curate the notes (preserve feature/fix/doc PRs and `New Contributors`, condensing routine build/test bumps if needed).
3. Publish the release as `Latest` **targeting `github-release`** (always target `github-release` rather than `master` so tag `X.Y.Z` points to the exact release commit where `version = "X.Y.Z"`, not the post-release `NEXT_VERSION` bump commit):

   ```bash
   gh release create "X.Y.Z" \
     --repo jasync-sql/jasync-sql \
     --target github-release \
     --title "X.Y.Z" \
     --latest \
     --notes-file <notes_file>
   ```

   Verify it appears at `https://github.com/jasync-sql/jasync-sql/releases`.

### 6. Post-Release Preparation on `master`

Switch back to `master`, update documentation snippets to the newly released `X.Y.Z`, bump [`build.gradle.kts`](build.gradle.kts) to `NEXT_VERSION` (`X.Y.(Z+1)`), commit, and push:

1. Switch to `master`:

   ```bash
   git checkout master
   ```

2. Update [`README.md`](README.md) Maven (`<version>X.Y.Z</version>`) and Gradle (`com.github.jasync-sql:jasync-mysql:X.Y.Z`, `com.github.jasync-sql:jasync-postgresql:X.Y.Z`) examples to `X.Y.Z`.
3. Update [`build.gradle.kts`](build.gradle.kts) `version = "NEXT_VERSION"`.
4. Commit and push directly to `origin/master`:

   ```bash
   git add build.gradle.kts README.md
   git commit -m "chore: bump version to NEXT_VERSION and update README to X.Y.Z"
   git push origin master
   ```
