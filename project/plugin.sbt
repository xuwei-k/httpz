scalacOptions ++= Seq("-deprecation", "-unchecked", "-language:_")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.1")

addSbtPlugin("com.github.scalaprops" % "sbt-scalaprops" % "0.4.4")

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

fullResolvers ~= { _.filterNot(_.name == "jcenter") }
