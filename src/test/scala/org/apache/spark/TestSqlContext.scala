package org.apache.spark

import org.apache.spark.sql.{SQLContext, SparkSession}

/** Spark 4 made SQLContext abstract; wrap an existing SparkContext via SparkSession. */
object TestSqlContext {
  def apply(sc: SparkContext): SQLContext =
    SparkSession.builder().sparkContext(sc).getOrCreate().sqlContext
}
