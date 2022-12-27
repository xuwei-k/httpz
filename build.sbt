import build._

lazy val httpz = module("httpz").settings(
  scalapropsWithScalaz,
  name := httpzName,
  scalapropsVersion := "0.9.1",
  buildInfoPackage := "httpz",
  buildInfoObject := "BuildInfoHttpz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-core" % Common.ScalazVersion,
    "io.argonaut" %% "argonaut" % "6.3.8"
  )
)

lazy val async = module("async")
  .settings(
    name := asyncName,
    testSetting,
    buildInfoPackage := "httpz.async",
    buildInfoObject := "BuildInfoHttpzAsync",
    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % "2.12.3"
    )
  )
  .dependsOn(httpz, tests % "test")

lazy val scalaj = module("scalaj")
  .settings(
    name := scalajName,
    testSetting,
    buildInfoPackage := "httpz.scalajhttp",
    buildInfoObject := "BuildInfoHttpzScalaj",
    libraryDependencies ++= Seq(
      "org.scalaj" %% "scalaj-http" % "2.4.2" cross CrossVersion.for3Use2_13
    )
  )
  .dependsOn(httpz, tests % "test")

lazy val apache = module("apache")
  .settings(
    name := apacheName,
    testSetting,
    buildInfoPackage := "httpz.apachehttp",
    buildInfoObject := "BuildInfoHttpzApache",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.5.14"
    )
  )
  .dependsOn(httpz, tests % "test")

lazy val nativeClient = module("native-client").settings(
  name := nativeClientName,
  buildInfoPackage := "httpz.native",
  buildInfoObject := "BuildInfoHttpzNative",
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  libraryDependencies ++= (
    ("junit" % "junit" % "4.13.2" % "test") ::
      ("com.github.sbt" % "junit-interface" % "0.13.3" % "test") ::
      ("com.github.kristofa" % "mock-http-server" % "4.1" % "test") ::
      Nil
  )
)

lazy val native = Project("native", file("native"))
  .settings(
    Common.baseSettings,
    name := nativeName,
    testSetting
  )
  .dependsOn(httpz, nativeClient, tests % "test")

lazy val tests = Project("tests", file("tests"))
  .settings(
    Common.baseSettings,
    libraryDependencies ++= {
      ("filter" :: "jetty" :: Nil).map(m => "ws.unfiltered" %% s"unfiltered-$m" % "0.12.0")
    },
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
  .dependsOn(httpz)

lazy val root = {
  Project("root", file("."))
    .settings(
      Common.baseSettings,
      name := "httpz-all",
      artifacts := Nil,
      packagedArtifacts := Map.empty,
      artifacts ++= Classpaths.artifactDefs(Seq(Compile / packageDoc)).value,
      packagedArtifacts ++= Classpaths.packaged(Seq(Compile / packageDoc)).value,
      Defaults.packageTaskSettings(
        Compile / packageDoc,
        (Compile / unidoc).map { _.flatMap(Path.allSubpaths) }
      ),
    )
    .enablePlugins(ScalaUnidocPlugin)
    .aggregate(httpz, scalaj, async, apache, native, nativeClient, tests)
}
