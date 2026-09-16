# Releasing

Publishing is manual and runs from a machine with the signing key. This file exists because the
process is infrequent enough to forget between releases, and a half-remembered release is how a
wrong artifact reaches Maven Central under a name nobody can take back.

## What has to be in place

| | Where | Checked by |
|---|---|---|
| Central Portal token | `mavenCentralUsername` / `mavenCentralPassword` in `~/.gradle/gradle.properties` | the upload fails with 401 |
| GPG signing key | your local gpg agent — the build uses `useGpgCmd()`, so there is no key in any properties file | `sign*Publication` tasks fail with "no signatory" |
| The version | `version` in `shared/build.gradle.kts`, overridable with `-Pheimui.version=` | nothing — check it yourself |

Confirm the key is the one previous releases used, because changing it silently looks exactly like
a compromise to anyone verifying signatures:

```bash
gpg --list-secret-keys --keyid-format=long
```

It should be `36EDAD48467E5E90`.

## The release

**1. Everything CI checks, before pushing anything.**

```bash
./gradlew verify
```

**2. Set the version.** Edit `version` in `shared/build.gradle.kts`, or pass `-Pheimui.version=` for
a one-off. Publishing over a version that already exists on Central is not possible — Central
refuses, which is the behaviour you want.

**3. Upload to the staging repository.**

```bash
./gradlew publishToMavenCentral
```

This signs every artifact and uploads them. It does **not** make them public: they sit in a staging
repository in the [Central Portal](https://central.sonatype.com/publishing) until you release them
by hand. Look at what is there before you do — it is the last moment anything can be taken back.

Use `publishAndReleaseToMavenCentral` instead only when you are confident; it skips that pause.

**4. Tag the commit that was published**, not whatever is on your branch now:

```bash
git tag -a v<version> -m "<version>" && git push origin v<version>
```

**5. Create the GitHub release.** Attach the artifacts a consumer actually links, with their
signatures — the `.aar`, both `.klib`s, the sources jar, and one `.asc` each. Download them from
Central rather than from `build/`, so what is attached is provably what was published:

```bash
gh release create v<version> --prerelease --title "<version>" --notes-file notes.md <files...>
```

Write what changed, and say plainly what is broken or missing. `v0.0.1-alpha-1` names a bug it
shipped with; that is the standard to hold.

**6. Point the showcase at it.** `heimuiCore` in `heimui-demo/gradle/libs.versions.toml`, so its CI
builds a minified release against the version you just published. Until that moves, the R8 step in
that pipeline is testing the previous release.

## Afterwards

Central takes a few minutes to appear and a few hours to index. Verify from outside your machine:

```bash
curl -sI https://repo1.maven.org/maven2/io/heimui/heimui-core/<version>/heimui-core-<version>.pom
```

Then check the artifact carries what it should — the AAR must contain `proguard.txt`:

```bash
unzip -l ~/.m2/.../heimui-core-android-<version>.aar | grep proguard
```

That one is not paranoia. `0.0.1-alpha-1` shipped without it.
