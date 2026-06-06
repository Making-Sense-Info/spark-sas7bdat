/**
 * Spark / Scala version matrix for cross-builds.
 *
 * Legacy line (library 3.0.0): Spark 2.4 / 3.0 / 3.5 — Java 8 bytecode.
 * Spark 4 line (library 4.0.0): Spark 4.1+ on Scala 2.13.17 — Java 17 bytecode.
 *
 * Build Spark 4: sbt ++2.13.17 -Dspark.version=4.1.2 compile
 */
object SparkVersions {

  val LegacyLibraryVersion = "3.0.0"
  val Spark4LibraryVersion = "4.0.0"

  val Spark24 = "2.4.6"
  val Spark30 = "3.0.0"
  val Spark35 = "3.5.0"
  val Spark412 = "4.1.2"

  val Scala211 = "2.11.12"
  val Scala212 = "2.12.11"
  val Scala213Legacy = "2.13.12"
  val Scala213Spark4 = "2.13.17"

  def resolveSparkVersion(scalaBinary: String, explicit: Option[String]): String =
    explicit.getOrElse(scalaBinary match {
      case "2.11" => Spark24
      case "2.12" => Spark30
      case "2.13" => Spark35
      case other  => sys.error(s"Unsupported Scala binary version: $other")
    })

  def isSpark4(sparkVersion: String): Boolean =
    sparkVersion.startsWith("4.")

  def libraryVersion(sparkVersion: String): String =
    if (isSpark4(sparkVersion)) Spark4LibraryVersion else LegacyLibraryVersion

  def recommendedScala(sparkVersion: String, scalaBinary: String): Option[String] =
    if (isSpark4(sparkVersion) && scalaBinary == "2.13") Some(Scala213Spark4)
    else None

}
