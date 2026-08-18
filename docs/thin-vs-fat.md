# Thin vs fat JAR

Maven Central publishes **both** artifacts under the same coordinates
`info.making-sense:spark-sas7bdat_2.13:4.0.1`. Spark (`spark-core`, `spark-sql`)
is never bundled. **4.0.0** on Central is thin-only; use **4.0.1+** for the fat JAR.

| | **Thin** (default) | **Fat** (`classifier=assembly`) |
|---|--------------------|----------------------------------|
| File on Central | `spark-sas7bdat_2.13-4.0.1.jar` (~50 KB) | `spark-sas7bdat_2.13-4.0.1-assembly.jar` |
| Parso | resolved from the POM | bundled inside the JAR |
| Use | sbt / Maven / Gradle, `spark.jars.packages`, `spark-submit --packages` | `spark.jars`, `spark-submit --jars`, air-gapped PySpark |

Wget of the **thin** JAR alone on `spark.jars` fails with:

`NoClassDefFoundError: com/epam/parso/impl/SasFileReaderImpl`

That is expected: the thin artifact does not contain Parso.

Do **not** add `classifier=assembly` as a Maven/sbt dependency. The POM still
declares Parso, so you would get the fat JAR **and** a transitive Parso JAR.
Download the assembly file only for `--jars` / `spark.jars`.

## With network (recommended)

Spark/Maven pulls the thin JAR and Parso from the POM:

```python
spark = SparkSession.builder \
    .config("spark.jars.packages", "info.making-sense:spark-sas7bdat_2.13:4.0.1") \
    .getOrCreate()
```

```bash
spark-shell --packages info.making-sense:spark-sas7bdat_2.13:4.0.1
```

## Air-gapped / one JAR

Download the **assembly** artifact (not the file without `-assembly`):

```bash
wget https://repo1.maven.org/maven2/info/making-sense/spark-sas7bdat_2.13/4.0.1/spark-sas7bdat_2.13-4.0.1-assembly.jar
```

```python
from pathlib import Path

jar = Path("C:/jars/spark-sas7bdat_2.13-4.0.1-assembly.jar")
spark = SparkSession.builder \
    .config("spark.jars", jar.as_posix()) \
    .getOrCreate()
```

On Windows, use `Path.as_posix()` or `C:/...` in `spark.jars`. Backslashes can
break comma-separated JAR lists.

[GitHub Releases](https://github.com/Making-Sense-Info/spark-sas7bdat/releases)
attach the same fat JAR (local name `spark-sas7bdat-4.0.1-s_2.13-assembly.jar`).
You can also build it with `sbt assembly` — see [standalone-build.md](standalone-build.md).

## Air-gapped / two JARs

Equivalent to the fat JAR if you already have the thin artifact:

```bash
wget https://repo1.maven.org/maven2/info/making-sense/spark-sas7bdat_2.13/4.0.1/spark-sas7bdat_2.13-4.0.1.jar
wget https://repo1.maven.org/maven2/com/epam/parso/2.0.14/parso-2.0.14.jar
```

Pass **both** on `spark.jars` (comma-separated).
