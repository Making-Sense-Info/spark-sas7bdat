# Spark SAS Data Source (sas7bdat)

A library for reading SAS data (.sas7bdat) with [Spark](http://spark.apache.org/).

This is a **Making Sense** fork of [saurfang/spark-sas7bdat](https://github.com/saurfang/spark-sas7bdat), with Spark **4.1.x** support.

[![CI](https://github.com/Making-Sense-Info/spark-sas7bdat/actions/workflows/ci.yml/badge.svg)](https://github.com/Making-Sense-Info/spark-sas7bdat/actions/workflows/ci.yml)

## Requirements:

- [Spark 2.4.x, 3.0.x, 3.5.x, or 4.1.x](https://spark.apache.org/downloads.html) (see Scala/Spark version mapping below)
- Spark 4.x requires **Java 17+** and **Scala 2.13.17** (library **4.0.0-SNAPSHOT** on `main`, **4.0.0** on Maven Central after release; built/tested against **Spark 4.1.2**)
- [Parso 2.0.14](https://mvnrepository.com/artifact/com.epam/parso/2.0.14)

## Download:

Maven coordinates for this fork (`groupId` **info.making-sense**).  
Spark data source format (Java package, no hyphen): **`info.makingsense.sas.spark`**.

```scala
// For sbt — Spark 4.1.x
libraryDependencies += "info.making-sense" %% "spark-sas7bdat" % "4.0.0"
```

```xml
<!-- For Maven — Spark 4.1.x -->
<dependency>
    <groupId>info.making-sense</groupId>
    <artifactId>spark-sas7bdat_2.13</artifactId>
    <version>4.0.0</version>
</dependency>
```

| Scala Version | Spark Version | Library version | Artifact ID |
| ------------- | ------------- | --------------- | ----------- |
| 2.11.x        | 2.4.x         | 3.0.0           | spark-sas7bdat_2.11 |
| 2.12.x        | 3.0.x         | 3.0.0           | spark-sas7bdat_2.12 |
| 2.13.x        | 3.5.x         | 3.0.0           | spark-sas7bdat_2.13 |
| 2.13.17       | 4.1.2+        | **4.0.0** / 4.0.0-SNAPSHOT | spark-sas7bdat_2.13 |

Use **3.0.0** on Spark 3.5 clusters and **4.0.0** on Spark 4.1+ clusters (same artifact name, different library release). Build with `-Dspark.version=4.1.2` (or your cluster's 4.1.x patch version).

Until **4.0.0** is on Maven Central, `main` is **4.0.0-SNAPSHOT** (`publishM2`): **[docs/standalone-build.md](docs/standalone-build.md)**.

To publish: drop `-SNAPSHOT`, commit, tag **`v4.0.0`**, `git push origin v4.0.0`: **[docs/publish-maven.md](docs/publish-maven.md)**.

### Build JARs from source (thin vs fat)

Two artifacts — pick the one that matches how you run Spark:

| Artifact | Command | Output | Use when |
|----------|---------|--------|----------|
| **Thin** | `package` | `spark-sas7bdat-*-s_2.13.jar` (~50 KB) | Maven / sbt (`publishM2`), `--packages` (Parso via POM) |
| **Fat** | `assembly` | `spark-sas7bdat-*-s_2.13-assembly.jar` (~6 MB) | PySpark, `spark-submit --jars` (Parso bundled) |

Spark (`spark-core`, `spark-sql`) is not included in either JAR.

**Spark 4.1.x** (current `main`: **4.0.0-SNAPSHOT**):

```bash
# Thin JAR (Maven / publishM2)
sbt -Dspark.version=4.1.2 ++2.13.17 package

# Fat JAR (PySpark / --jars)
sbt -Dspark.version=4.1.2 ++2.13.17 assembly
# → target/scala-2.13/spark-sas7bdat-4.0.0-SNAPSHOT-s_2.13-assembly.jar
```

**Legacy line** (library **3.0.0**, e.g. Spark 3.5):

```bash
sbt ++2.13.12 package      # thin
sbt ++2.13.12 assembly     # fat
```

CI runs `package` (thin JAR) only — no `assembly`.

## Features:

- This package allows reading SAS files from local and distributed filesystems, into Spark DataFrames.
- Schema is automatically inferred from metadata embedded in the SAS file. _(Behaviour can be customised, see parameters below)_
- The SAS format is splittable when not file-system compressed, thus we are able to convert a 200GB (1.5Bn rows) .sas7bdat file to .csv files using 2000 executors in under 2 minutes.
- This library uses [parso](https://github.com/epam/parso/) for parsing as it is the only public available parser
  that handles both forms of SAS compression (CHAR and BINARY).

**NOTE:** this package does not support writing sas7bdat files

## Docs:

### Parameters:

- `extractLabel` _(Default: `false`)_
  - _Boolean:_ extract column labels as column comments for Parquet/Hive
- `forceLowercaseNames` _(Default: `false`)_
  - Boolean: force column names to lower case
- `inferDecimal` _(Default: `false`)_
  - Boolean: infer numeric columns _with format width >0 and format precision >0_, as _Decimal(Width, Precision)_
- `inferDecimalScale` _(Default: `each column's format width`)_
  - Int: scale of inferred decimals
- `inferFloat` _(Default: `false`)_
  - Boolean: infer numeric columns _with <=4 bytes_, as _Float_
- `inferInt` _(Default: `false`)_
  - Boolean: infer numeric columns _with <=4 bytes, format width >0 and format precision =0_, as _Int_
- `inferLong` _(Default: `false`)_
  - Boolean: infer numeric columns _with <=8 bytes, format width >0 and format precision =0_, as _Long_
- `inferShort` _(Default: `false`)_
  - Boolean: infer numeric columns _with <=2 bytes, format width >0 and format precision =0_, as _Short_
- `metadataTimeout` _(Default: `60`)_
  - Int: number of seconds to allow reading of file metadata _(stops corrupt files hanging)_
- `minSplitSize` _(Default: `mapred.min.split.size`)_
  - Long: minimum byte length of input splits _(splits are always at least 1MB, to ensure correct reads)_
- `maxSplitSize` _(Default: `mapred.max.split.size`)_
  - Long: maximum byte length of input splits, _(can be decreased to force higher parallelism)_

**NOTE:**

- the order of precedence for numeric type inference is: _Long_ -> _Int_ -> _Short_ -> _Decimal_ -> _Float_ -> _Double_
- sas doesn’t have a concept of Long/Int/Short, instead people typically use column formatters with 0 precision

### Scala API

```scala
val df = {
  spark.read
    .format("info.makingsense.sas.spark")
    .option("forceLowercaseNames", true)
    .option("inferLong", true)
    .load("cars.sas7bdat")
}
df.write.format("csv").option("header", "true").save("newcars.csv")
```

You can also use the implicit readers:

```scala
import info.makingsense.sas.spark._

// DataFrameReader
val df = spark.read.sas("cars.sas7bdat")
df.write.format("csv").option("header", "true").save("newcars.csv")

// SQLContext
val df2 = sqlContext.sasFile("cars.sas7bdat")
df2.write.format("csv").option("header", "true").save("newcars.csv")
```

(_Note: you cannot use parameters like `inferLong` with the implicit readers._)

### Python API

```python
df = spark.read.format("info.makingsense.sas.spark").load("cars.sas7bdat", forceLowercaseNames=True, inferLong=True)
df.write.csv("newcars.csv", header=True)
```

### R API

```r
df <- read.df("cars.sas7bdat", source = "info.makingsense.sas.spark", forceLowercaseNames = TRUE, inferLong = TRUE)
write.df(df, path = "newcars.csv", source = "csv", header = TRUE)
```

### SQL API

SAS data can be queried in pure SQL by registering the data as a (temporary) table.

```sql
CREATE TEMPORARY VIEW cars
USING info.makingsense.sas.spark
OPTIONS (path="cars.sas7bdat")
```

### SAS Export Runner

We included a simple `SasExport` Spark program that converts _.sas7bdat_ to _.csv_ or _.parquet_ files:

```bash
sbt "run input.sas7bdat output.csv"
sbt "run input.sas7bdat output.parquet"
```

To achieve more parallelism, use `spark-submit` script to run it on a Spark cluster. If you don't have a spark
cluster, you can always run it in local mode and take advantage of multi-core.

### Spark Shell

```bash
spark-shell --master local[4] --packages info.making-sense:spark-sas7bdat_2.13:4.0.0
```

## Caveats

1. `spark-csv` writes out `null` as "null" in csv text output. This means if you read it back for a string type,
   you might actually read "null" instead of `null`. The safest option is to export in parquet format where
   null is properly recorded. See https://github.com/databricks/spark-csv/pull/147 for alternative solution.

## Related Work

- [parso](https://github.com/epam/parso)
- [sas7bdat format](http://www2.uaem.mx/r-mirror/web/packages/sas7bdat/vignettes/sas7bdat.pdf)
- [ReadStat](https://github.com/WizardMac/ReadStat)

## Attribution

This project is a derivative work of [spark-sas7bdat](https://github.com/saurfang/spark-sas7bdat) by Forest Fang, licensed under the [Apache License 2.0](LICENSE). Original copyright notices are retained in the source files.


