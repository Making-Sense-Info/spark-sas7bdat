addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/releases/org/scalastyle"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Maven Central publishing
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
