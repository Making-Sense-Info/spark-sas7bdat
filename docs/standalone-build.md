# Standalone build and local Maven install

Build **spark-sas7bdat 4.0.0** (Spark **4.1.x**) from source and install into `~/.m2`.

Maven coordinates: `info.making-sense:spark-sas7bdat_2.13:4.0.0`  
Spark format: `info.makingsense.sas.spark`

## Prerequisites

JDK **17** (21 OK; not 25). sbt 1.9+.

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

On Windows, put `-Dspark.version=…` **before** `++2.13.17`.

## Thin vs fat JAR

| Artifact | Command | Output | Use when |
|----------|---------|--------|----------|
| **Thin** | `package` / `publishM2` | `spark-sas7bdat-4.0.0-s_2.13.jar` (~50 KB) | Maven / sbt (Parso via POM) |
| **Fat** | `assembly` | `spark-sas7bdat-4.0.0-s_2.13-assembly.jar` (~6 MB) | PySpark / `--jars` |

```bash
sbt -Dspark.version=4.1.2 ++2.13.17 clean test package publishM2
sbt -Dspark.version=4.1.2 ++2.13.17 assembly
```

Installed at `~/.m2/repository/info/making-sense/spark-sas7bdat_2.13/4.0.0/`.

Maven Central: tag `v4.0.0`. See [publish-maven.md](publish-maven.md).
