# Publish to Maven Central (`info.makingsense`)

One-time setup, then a short release command. CI does **not** publish — you do it locally (or from a protected release job later).

Coordinates after a Spark 4 release:

```
groupId:    info.makingsense
artifactId: spark-sas7bdat_2.13
version:    4.0.0
```

Publishing the **thin** JAR only (`package` / `publishSigned`). The fat JAR (`assembly`) stays a local/PySpark artifact.

---

## 1. Claim the namespace (once)

1. Create an account on the [Central Portal](https://central.sonatype.com/).
2. **View Namespaces** → **Claim namespace** → `info.makingsense`.
3. Prove you own **makingsense.info** (TXT DNS record or the verification file Sonatype shows). Wait until the namespace is **Verified**.
4. **Generate User Token** (username + password). Store them as env vars, never in git:

```bash
export SONATYPE_USERNAME="..."   # token username
export SONATYPE_PASSWORD="..."   # token password
```

`build.sbt` already uses `sonatypeProfileName := "info.makingsense"` and `sonatypeCentralHost`.

---

## 2. GPG signing key (once)

Maven Central requires signed artifacts (`sbt-pgp` is already in `project/plugins.sbt`).

```bash
gpg --full-generate-key          # RSA 4096, your name + Making Sense email
gpg --list-secret-keys --keyid-format LONG
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

Optional: export the passphrase for sbt (otherwise it will prompt):

```bash
export PGP_PASSPHRASE="..."
```

If sbt cannot find gpg: `export PGP_TTY=$(tty)` or configure `~/.sbt/gpg.sbt`.

---

## 3. sbt credentials (once)

Create `~/.sbt/1.0/sonatype.sbt` (outside the repo):

```scala
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "central.sonatype.com",
  sys.env.getOrElse("SONATYPE_USERNAME", ""),
  sys.env.getOrElse("SONATYPE_PASSWORD", "")
)
```

---

## 4. Release Spark 4.0.0

JDK **17** (not 25). On macOS Homebrew:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

From the repo root:

```bash
sbt -Dspark.version=4.1.2 ++2.13.17 clean test
sbt -Dspark.version=4.1.2 ++2.13.17 publishSigned
sbt sonatypeBundleRelease
```

`publishSigned` writes a staging bundle (JAR + sources + javadoc + `.asc` + POM).  
`sonatypeBundleRelease` uploads it to Central Portal and releases it.

Check:

- Portal: [https://central.sonatype.com/publishing](https://central.sonatype.com/publishing)
- After ~10–30 min: [Maven Central](https://search.maven.org/artifact/info.makingsense/spark-sas7bdat_2.13/4.0.0/jar)

Dry-run without uploading:

```bash
sbt -Dspark.version=4.1.2 ++2.13.17 publishM2
ls ~/.m2/repository/info/makingsense/spark-sas7bdat_2.13/4.0.0/
```

---

## 5. Checklist before `publishSigned`

Central rejects incomplete POMs. This repo already sets:

| POM field | `build.sbt` |
|-----------|-------------|
| `groupId` | `organization := "info.makingsense"` |
| `licenses` | Apache-2.0 |
| `url` / `scm` | `https://github.com/Making-Sense-Info/spark-sas7bdat` |
| `developers` | Making Sense |
| `description` | Spark SAS data source |
| no extra repos | `pomIncludeRepository := { _ => false }` |

Also required: **GPG signature**, **sources JAR**, **javadoc JAR** (sbt publishes them by default with `publishSigned`).

Do **not** publish a SNAPSHOT. Do **not** reuse a version that already exists on Central.

---

## 6. After it is live

Client apps:

```xml
<dependency>
    <groupId>info.makingsense</groupId>
    <artifactId>spark-sas7bdat_2.13</artifactId>
    <version>4.0.0</version>
</dependency>
```

Spark format (breaking vs upstream):

```java
spark.read().format("info.makingsense.sas.spark").load(path);
```

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Namespace not verified | DNS TXT for `makingsense.info` not propagated yet |
| `401` / `403` on upload | Token expired or wrong host — use `central.sonatype.com` |
| Missing javadoc / sources | `sbt publishSigned` must include `packageDoc` / `packageSrc` (default) |
| `gpg: skipped: unusable` | Key has no secret key, or `PGP_PASSPHRASE` wrong |
| Version already exists | Bump version — Central is immutable |
