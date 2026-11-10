# Publishing this fork to GitHub Packages

This fork can publish its own artifacts to the GitHub Packages Maven registry, so that services can
consume the modified Micronaut RabbitMQ instead of the upstream Maven Central release.

* Registry: `https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq`
  (owner/repository **must be lowercase**)
* Artifacts:
  * `io.micronaut.rabbitmq:micronaut-rabbitmq:<version>` — jar, sources, javadoc and Gradle module metadata
  * `io.micronaut.rabbitmq:micronaut-rabbitmq-bom:<version>` — BOM (plus the generated version catalog)

Nothing is published to Maven Central from this fork; the Sonatype/GPG credentials the upstream build
expects are absent, so the signing and Sonatype publishing blocks stay disabled.

---

## 1. Publish from GitHub Actions (recommended)

The workflow [`.github/workflows/publish-github-packages.yml`](.github/workflows/publish-github-packages.yml)
is manual (`workflow_dispatch`) and uses the workflow's `GITHUB_TOKEN`, so no secrets have to be configured.

> **Use this workflow, not "Publish snapshot release"** — that one is the upstream workflow that pushes
> `-SNAPSHOT` builds to Sonatype/Maven Central and cannot work in a fork (see §8).

From the GitHub UI: **Actions → Publish to GitHub Packages → Run workflow**.

From the command line:

```bash
# default version: <projectVersion without -SNAPSHOT>-fork.<run_number>, e.g. 5.1.1-fork.17
gh workflow run publish-github-packages.yml --repo stCarolas/micronaut-rabbitmq

# explicit version
gh workflow run publish-github-packages.yml --repo stCarolas/micronaut-rabbitmq -f version=5.1.1-fork.2

# watch the run; the resolved version is printed in the job log and in the job summary
gh run watch --repo stCarolas/micronaut-rabbitmq
```

The published version is printed in the run summary ("Published the fork artifacts to GitHub Packages").

**The package appears only after this workflow has run at least once** — `Java CI` never publishes. It is
not sent to an email or to the repository's *Releases*; look here instead:

* repo page → right sidebar → **Packages**
* <https://github.com/stCarolas?tab=packages>
* direct link: <https://github.com/stCarolas/micronaut-rabbitmq/pkgs/maven/io.micronaut.rabbitmq.micronaut-rabbitmq>

A freshly published package is **private**; §5 explains how to change that.

If nothing shows up, check that the run you are looking at is *Publish to GitHub Packages* and not
`Java CI` — the workflow list shows both.

## 2. Publish from your machine

Use a personal access token (classic) with the `write:packages` scope. Keep it out of the repository:

```properties
# ~/.gradle/gradle.properties
githubPackagesRepository=stCarolas/micronaut-rabbitmq
gpr.user=stCarolas
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository \
    -PprojectVersion=5.1.1-fork.local1 \
    -PskipSigning
```

`GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables are accepted as well, and `GITHUB_REPOSITORY`
replaces `githubPackagesRepository`. `-PskipSigning` is only relevant if your machine also has
Sonatype and GPG credentials configured, in which case it keeps fork artifacts unsigned.

To try the whole publication without touching the real registry, point the URL at a local directory —
a `file:` URL does not need credentials:

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository \
    -PgithubPackagesUrl=file://$PWD/build/gpr-repo \
    -PprojectVersion=5.1.1-fork.verify
# artifacts end up under build/gpr-repo/io/micronaut/rabbitmq/...
```

Publishing only to the local Maven repository keeps working as before: `./gradlew publishToMavenLocal`.

## 3. Consume it in a service

Put the fork registry **before** `mavenCentral()` and authenticate. Groovy DSL:

```groovy
repositories {
    maven {
        name = "GitHubPackagesRabbitmq"
        url = uri("https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq")
        credentials {
            username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

dependencies {
    implementation("io.micronaut.rabbitmq:micronaut-rabbitmq:5.1.1-fork.2")
}
```

Kotlin DSL:

```kotlin
repositories {
    maven {
        name = "GitHubPackagesRabbitmq"
        url = uri("https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

dependencies {
    implementation("io.micronaut.rabbitmq:micronaut-rabbitmq:5.1.1-fork.2")
}
```

If the service declares repositories in `settings.gradle(.kts)` (`dependencyResolutionManagement`), add the
repository there instead — otherwise the declaration is ignored, which shows up as a plain
`Could not find io.micronaut.rabbitmq:micronaut-rabbitmq:<version>` error.

An explicit version on the dependency wins over a version managed by `micronaut-platform`, so the fork
version can be used in a service that already imports the Micronaut BOM. Keep `mavenCentral()` in the
repository list: everything except `micronaut-rabbitmq` itself is still resolved from Maven Central.

Using the BOM keeps the rest of the versions aligned with the fork:

```groovy
dependencies {
    implementation(platform("io.micronaut.rabbitmq:micronaut-rabbitmq-bom:5.1.1-fork.2"))
    implementation("io.micronaut.rabbitmq:micronaut-rabbitmq") // version comes from the BOM
}
```

Credentials for the service:

* locally — `gpr.user` / `gpr.key` in `~/.gradle/gradle.properties` (classic PAT with `read:packages`);
* in a GitHub Actions workflow of another repository — either a PAT secret, or the service's own
  `GITHUB_TOKEN` **after** granting that repository read access to the package
  (package page → *Package settings* → *Manage Actions access* → *Add repository*):

  ```yaml
  - run: ./gradlew build
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}   # or a PAT with read:packages
  ```

Maven consumers need the repository plus a `server` entry in `settings.xml`; enable snapshots for
`-SNAPSHOT` versions:

```xml
<repository>
  <id>github-rabbitmq</id>
  <url>https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq</url>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## 4. Versions: publish a new one instead of reusing one

GitHub Packages treats a version as immutable: uploading the same coordinates twice fails with
`409 Conflict` (`Failed to publish publication 'maven' to repository 'GitHubPackages'`). In practice
this applies to `-SNAPSHOT` versions too, because Gradle's `maven-publish` reuses the same
`…-SNAPSHOT.jar` file paths instead of creating timestamped snapshots, so the second upload hits the
files that are already there.

Consequently:

* the workflow default `5.1.1-fork.<run_number>` (from `projectVersion=5.1.1-SNAPSHOT` in
  [`gradle.properties`](gradle.properties)) is unique per run and always deployable;
* bump the version whenever you want a new one, e.g. `5.1.1-fork.3`, `5.1.1-fork.4`, or tag-like
  versions such as `5.1.1-stcarolas.1`;
* if you really want to keep a single fixed version (for example `5.1.1-fork-SNAPSHOT`), delete the
  published version first, then publish again. Maven packages are named `groupId.artifactId` in the
  REST API, and a classic PAT needs the `delete:packages` scope:

  ```bash
  # list the published versions with their ids
  gh api /users/stCarolas/packages/maven/io.micronaut.rabbitmq.micronaut-rabbitmq/versions \
    -q '.[] | "\(.id)\t\(.name)"'

  # delete a version
  gh api --method DELETE \
    /users/stCarolas/packages/maven/io.micronaut.rabbitmq.micronaut-rabbitmq/versions/<id>
  ```

  The same is available in the UI: package page → the version → *Delete*.

The version is a normal Gradle project property, so any build invocations can override it, but
micronaut-build requires it to start with a digit:

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository -PprojectVersion=5.1.1-fork.7
```

## 5. One-time GitHub setup

1. **Allow the workflow to write packages** — the workflow already requests `packages: write`
   explicitly, which is enough on a standard personal repository. If the *organization* restricts
   `GITHUB_TOKEN` to read-only, relax *Settings → Actions → General → Workflow permissions* or use a
   personal access token with `write:packages` instead. Without it the upload fails with `403`.
2. **Package visibility** — a package published from a repository starts out **private**, even when
   the repository is public. If services or people outside your account need to read it, open the
   package page and use *Danger Zone → Change visibility* (public is irreversible), or grant
   read access to individual users/repositories. Note that the Maven and Gradle registries always
   require authentication, public package or not.

## 6. How it is wired in this repository

Only three places are involved, and none of them change the upstream behaviour:

| File | Purpose |
| --- | --- |
| [`buildSrc/.../io.micronaut.build.internal.rabbitmq-github-packages.gradle`](buildSrc/src/main/groovy/io.micronaut.build.internal.rabbitmq-github-packages.gradle) | Registers a `GitHubPackages` Maven repository **only** when a registry URL and, for remote URLs, credentials are available. Properties: `githubPackagesUrl`, `githubPackagesRepository`, `gpr.user`, `gpr.key`; environment fallbacks: `GITHUB_REPOSITORY`, `GITHUB_ACTOR`, `GITHUB_TOKEN`. |
| [`buildSrc/.../io.micronaut.build.internal.rabbitmq-module.gradle`](buildSrc/src/main/groovy/io.micronaut.build.internal.rabbitmq-module.gradle), [`rabbitmq-bom/build.gradle`](rabbitmq-bom/build.gradle) | Apply that plugin to the `rabbitmq` and `rabbitmq-bom` projects. |
| [`.github/workflows/publish-github-packages.yml`](.github/workflows/publish-github-packages.yml) | Manual workflow: resolves the version, runs `publishAllPublicationsToGitHubPackagesRepository`, writes a run summary. Not part of the file set synced from `micronaut-project-template`. |

The alternative to the convention plugin is micronaut-build's built-in "external" repository, which
needs no build change at all:

```bash
./gradlew publish \
  -Dio.micronaut.publishing.uri=https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq \
  -Dio.micronaut.publishing.username=stCarolas \
  -Dio.micronaut.publishing.password="$GITHUB_TOKEN"
```

Both approaches publish the same publications; the convention plugin is preferred because credentials
travel through Gradle properties/environment variables instead of the command line.

`./gradlew publish` publishes to every configured remote repository (the local `build/repo` *Build*
repository and the GitHub Packages one); use the repository-specific task
`publishAllPublicationsToGitHubPackagesRepository` to upload only to GitHub Packages.

The POM still points at the upstream SCM (`githubSlug=micronaut-projects/micronaut-rabbitmq`). That is
cosmetic; set `githubSlug=stCarolas/micronaut-rabbitmq` in `gradle.properties` if the fork should
advertise itself.

## 7. Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `Received status code 409 from server: Conflict` | That version already exists. Publish a new version, or delete the existing package version (see §4). |
| `Received status code 401 from server: Unauthorized` | Wrong token for publishing (`write:packages` needed) or wrong username. |
| `Received status code 403 from server: Forbidden` | Workflow permissions do not allow `packages: write`, or the token lacks the `write:packages` scope. |
| `Could not find io.micronaut.rabbitmq:micronaut-rabbitmq:<version>` | Repository URL uses the wrong (non-lowercase) owner/repository, the repository is declared in `settings.gradle` instead of the build script (or vice versa), the package is private and not accessible, or the version was never published. |
| `Received status code 422 from server: Unprocessable Entity` | The artifactId must only contain lowercase letters, digits and hyphens — not an issue for this project unless you rename a module. |
| Build still resolves the upstream release | `mavenCentral()` is declared before the fork registry and the requested version exists there; request the fork version explicitly or move the registry first. |
| `Java CI` fails at *Publish to Sonatype Snapshots* with `Task 'publishToSonatype' not found` | Upstream's Maven Central snapshot deployment; the fork has no Sonatype secrets. Already guarded in `gradle.yml` (see §8). |

Verify what is in the registry from the command line:

```bash
curl -s -u stCarolas:"$GITHUB_TOKEN" \
  https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq/io/micronaut/rabbitmq/micronaut-rabbitmq/maven-metadata.xml
```

## 8. Upstream Maven Central workflows in this fork

These files come from the Micronaut project template and target Maven Central, so they cannot run in a
fork. They are left untouched (except for the guards described below) to keep the diff against
upstream small:

| Workflow | Trigger | Status in the fork |
| --- | --- | --- |
| `Java CI` (`gradle.yml`) | push / PR | Builds and tests run normally. The *Publish to Sonatype Snapshots* and *Publish to Github Pages* steps are skipped unless `SONATYPE_USERNAME`/`SONATYPE_PASSWORD`/`GH_TOKEN` secrets exist. |
| `Publish snapshot release` (`publish-snapshot.yml`) | manual | Fails by design: no Sonatype credentials. Use *Publish to GitHub Packages* instead. |
| `Release` (`release.yml`) | GitHub release published | Fails by design: it bundles and uploads to Maven Central. |
| `Maven Central Sync` (`central-sync.yml`) | manual | Fails by design: it closes/releases a Sonatype staging repository. |

`micronaut-build` only registers the `sonatype` repository and the `publishToSonatype` task when
Sonatype credentials are present, which is why a fork without those secrets fails with
`Task 'publishToSonatype' not found in root project 'rabbitmq-parent' and its subprojects`.

## 9. Related documentation

* [Working with the Gradle registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
* [Working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
* [Configuring a package's access control and visibility](https://docs.github.com/en/packages/learn-github-packages/configuring-a-packages-access-control-and-visibility)
* [`CONTRIBUTING.md`](CONTRIBUTING.md) — building and testing the fork locally
