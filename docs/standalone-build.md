# Standalone build and local Maven install

Build **spark-sas7bdat** (Spark **4.1.x**) from source and install into `~/.m2`.

On `master`, the Spark 4 line is **`4.1.0-SNAPSHOT`**. Latest Central release is **`4.0.1`**.

Maven coordinates (Central): `info.making-sense:spark-sas7bdat_2.13:4.0.1`  
Spark format: `info.makingsense.sas.spark`

How to *use* the JARs (PySpark, `--jars`, Windows paths): **[thin-vs-fat.md](thin-vs-fat.md)**.

## Prerequisites

JDK **17** (21 OK; not 25). sbt 1.9+.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

On Windows, put `-Dspark.version=…` **before** `++2.13.17`.

## Commands

| Artifact | Command | Local output (from `master`) |
|----------|---------|------------------------------|
| **Thin** | `package` / `publishM2` | `spark-sas7bdat-4.1.0-SNAPSHOT-s_2.13.jar` |
| **Fat** | `assembly` | `spark-sas7bdat-4.1.0-SNAPSHOT-s_2.13-assembly.jar` |

`publishM2` also installs the fat JAR as classifier `assembly`.

**Spark 4.1.x** (library **4.1.0-SNAPSHOT** on `master`):

```bash
sbt -Dspark.version=4.1.2 ++2.13.17 clean test package publishM2
sbt -Dspark.version=4.1.2 ++2.13.17 assembly
```

**Legacy line** (library **3.0.0**, e.g. Spark 3.5):

```bash
sbt ++2.13.12 package
sbt ++2.13.12 assembly
```

Installed at `~/.m2/repository/info/making-sense/spark-sas7bdat_2.13/4.1.0-SNAPSHOT/`.

Maven Central: last tag `v4.0.1`. Next release: drop SNAPSHOT, tag `v4.1.0`. See [publish-maven.md](publish-maven.md).
