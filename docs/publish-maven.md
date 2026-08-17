# Publish to Maven Central (`info.making-sense`)

Releases are done by **git tag** via GitHub Actions (`.github/workflows/release.yml`).  
There is no Maven `pom.xml`: the published version is **`SparkVersions.Spark4LibraryVersion`** in `project/SparkVersions.scala`.

On a release commit that is **`4.0.1`** (no SNAPSHOT). CI on push/PR only runs `test` + `package`. **Publish happens only on tags** `vX.Y.Z`.

Coordinates after release:

```
groupId:    info.making-sense
artifactId: spark-sas7bdat_2.13
version:    4.0.1   # tag v4.0.1 — no SNAPSHOT
```

Published files:

- **Thin** (default): `spark-sas7bdat_2.13-4.0.1.jar` + POM + sources + javadoc
- **Fat** (classifier `assembly`): `spark-sas7bdat_2.13-4.0.1-assembly.jar` (Parso bundled)

Spark format / Scala packages stay **`info.makingsense.sas.spark`** (no hyphen — invalid in Java packages). Only the Maven `groupId` uses `info.making-sense`.

User-facing thin vs fat: **[thin-vs-fat.md](thin-vs-fat.md)**.

`4.0.0` on Central is thin-only and immutable. Fat JAR on Central starts at **4.0.1**.

---

## One-time setup

### 1. Namespace

1. Account on [Central Portal](https://central.sonatype.com/).
2. Claim namespace **`info.making-sense`** (prove ownership of the domain used for that claim, typically **making-sense.info**, via DNS TXT).
3. Wait until **Verified**. Generate a **User Token**.

### 2. GPG key

```bash
gpg --full-generate-key          # (1) RSA, 4096, no expiry
gpg --list-secret-keys --keyid-format LONG
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

Export for GitHub Actions as **base64** (recommended — avoids “Misformed armored text”):

```bash
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 | pbcopy
```

Paste that into the repo secret `PGP_SECRET`.  
`PGP_PASSPHRASE` = the key passphrase.

Armored text also works if you paste the full block including newlines (`BEGIN PGP PRIVATE KEY BLOCK` … `END`).

### 3. GitHub secrets

Repo **Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|--------|
| `SONATYPE_USERNAME` | Central Portal **User Token** username (not your login email) |
| `SONATYPE_PASSWORD` | Central Portal **User Token** password |
| `PGP_SECRET` | Prefer **base64** of armored private key: `gpg --armor --export-secret-keys KEYID \| base64` |
| `PGP_PASSPHRASE` | Passphrase of that key |

Generate the token at [central.sonatype.com/account](https://central.sonatype.com/account) → **Generate User Token**.  
A `401` on upload almost always means these two secrets are wrong or still empty / old password instead of the token.

---

## Release (tag)

`git push origin v4.0.1` is correct (**keep the `v`**). `git push origin 4.0.1` would not trigger this workflow.

1. On `main`, development version is SNAPSHOT:

```scala
// project/SparkVersions.scala
val Spark4LibraryVersion = "4.0.1-SNAPSHOT"
```

2. When ready, **drop SNAPSHOT**, commit:

```scala
val Spark4LibraryVersion = "4.0.1"
```

3. Create the tag **locally**, then push **that tag**:

```bash
git tag v4.0.1
git push origin v4.0.1
```

If you only `git push origin v4.0.1` without `git tag` first, git looks for a ref that does not exist yet.

4. The **Release** workflow then:
   - fails if the sbt version still contains `SNAPSHOT`
   - fails if the tag is not `v` + `Spark4LibraryVersion`
   - runs Spark 4.1.2 tests
   - builds the fat JAR and checks that Parso is inside it
   - `publishSigned` + `sonatypeBundleRelease` (thin + assembly + sources + javadoc + signatures)
   - creates a GitHub Release and attaches thin + fat JARs

5. After Central is live, bump development to the next SNAPSHOT (`4.0.2-SNAPSHOT`) so `main` never republishes `4.0.1`.

---

## Local fallback (optional)

Only if you need to publish from a laptop. JDK **17**. Same env vars as the secrets:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export SONATYPE_USERNAME="..."
export SONATYPE_PASSWORD="..."
export PGP_PASSPHRASE="..."

sbt -Dspark.version=4.1.2 ++2.13.17 clean test
sbt -Dspark.version=4.1.2 ++2.13.17 assembly
sbt -Dspark.version=4.1.2 ++2.13.17 publishSigned
sbt sonatypeBundleRelease
```

Dry-run (no upload):

```bash
sbt -Dspark.version=4.1.2 ++2.13.17 publishM2
ls ~/.m2/repository/info/making-sense/spark-sas7bdat_2.13/4.0.1/
# expect …-4.0.1.jar and …-4.0.1-assembly.jar
```

---

## After it is live

```xml
<dependency>
    <groupId>info.making-sense</groupId>
    <artifactId>spark-sas7bdat_2.13</artifactId>
    <version>4.0.1</version>
</dependency>
```

```java
spark.read().format("info.makingsense.sas.spark").load(path);
```

Fat JAR (manual `--jars` only — not as a Maven dependency):

```
https://repo1.maven.org/maven2/info/making-sense/spark-sas7bdat_2.13/4.0.1/spark-sas7bdat_2.13-4.0.1-assembly.jar
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Tag / sbt version mismatch | Tag must be `v` + `Spark4LibraryVersion` |
| Namespace not verified | DNS TXT for the domain linked to namespace `info.making-sense` |
| `401` / `403` | Regenerate **User Token** on central.sonatype.com; set `SONATYPE_USERNAME` + `SONATYPE_PASSWORD` (token, not account password) |
| `gpg: skipped` / Misformed armored text | Re-set `PGP_SECRET` as base64 (`gpg --armor --export-secret-keys KEYID \| base64`) |
| Version already exists | Bump `Spark4LibraryVersion`, new tag |
| `NoClassDefFoundError: …SasFileReaderImpl` | Thin JAR used without Parso — see [thin-vs-fat.md](thin-vs-fat.md) |
