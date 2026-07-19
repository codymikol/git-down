# Packaging & distribution

GitDown is a JVM (Kotlin/Compose Desktop) application built with Gradle's
`org.jetbrains.compose` plugin, which already knows how to produce native
installers via `compose.desktop.application.nativeDistributions`
(`build.gradle.kts`): `.deb` (Linux), `.dmg` (macOS), `.msi` (Windows).

Versioning is driven by [release-please](https://github.com/googleapis/release-please)
(`release-please-config.json` / `.release-please-manifest.json`): it opens/
updates a release PR from Conventional Commit history, and merging it tags a
semver release and bumps the single `version` in `gradle.properties`. The
GitHub Actions workflow that runs release-please and, on each cut release,
builds the three installers below and attaches them as Release assets could
not be added in this change — see "Deploy workflow" below.

## Deploy workflow — pending, needs `workflow` scope

A `.github/workflows/release.yml` was drafted (release-please job, then a
Deb/Dmg/Msi build-and-attach matrix job) but this bot's PAT intentionally
lacks the `workflow` scope needed to push files under `.github/workflows/`.
A maintainer needs to add that workflow file by hand; see the issue comment
on #182 for the full YAML.

Status of each distribution channel:

## apt (Debian/Ubuntu) — asset available once the workflow lands, repo hosting not set up

`packageDeb` already produces a valid `.deb`; once the deploy workflow above
is added it will be attached to every GitHub Release. What's still needed to
make `apt install gitdown` work:

- A hosted apt repository (e.g. a signed PPA on Launchpad, or a self-hosted
  repo built with `reprepro`/`aptly`) that ingests the `.deb` asset from each
  release and republishes it under `dists/`.
- A GPG signing key for the repo, and a documented `add-apt-repository`/
  `curl ... | gpg --dearmor` step for users.

This is infrastructure/hosting the release workflow can push to once such a
repo exists, but provisioning that repo (and its signing key/secrets) is a
one-time setup decision outside this change's scope.

## Homebrew (macOS/Linux) — formula not written, needs a tagged release first

Homebrew formulas fetch a specific, hash-pinned source or binary URL, so a
formula can only be written against a release that already exists. Once
release-please cuts the first tag:

- Publish a formula (in `homebrew-core` or a project tap, e.g.
  `codymikol/homebrew-gitdown`) that either:
  - downloads the `.dmg`/`.deb` release asset directly (`url`/`sha256`
    pointing at the GitHub Release), or
  - builds from the `shadowJar` output for a source-based formula.
- Tap formulas can be updated automatically from the release workflow (e.g.
  a step that bumps the formula's `url`/`sha256` and pushes to the tap repo)
  once the tap repository exists.

## Nix — flake package output not added yet, blocked on an offline dependency lock

Making GitDown consumable as a nix flake input (e.g. `packages.default`)
means wrapping the Gradle build in a nix derivation. Nix builds are
network-sandboxed, so the derivation can't just run `./gradlew build` — every
Maven dependency has to be pre-fetched and hash-pinned as a fixed-output
derivation first, typically via a generator such as
[`gradle2nix`](https://github.com/tadfisher/gradle2nix) that inspects the
Gradle dependency graph and emits a lockfile nix can fetch from.

Generating that lockfile requires running the generator against this
project with full network access (to resolve every dependency in
`gradle/libs.versions.toml` plus their transitive graph, including the
JetBrains Compose dev repo and the JGit Maven repo declared in
`build.gradle.kts`), which isn't available in this sandboxed environment.
Follow-up work: run `gradle2nix` (or equivalent) with network access, commit
the resulting lockfile, and add a `packages.default` output to `flake.nix`
that consumes it.
