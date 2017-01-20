import build._

lazy val httpz = module("httpz").settings(
  scalapropsWithScalazlaws,
  name := httpzName,
  scalapropsVersion := "0.3.6",
  buildInfoPackage := "httpz",
  buildInfoObject := "BuildInfoHttpz",
  libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-concurrent" % Common.ScalazVersion,
    "io.argonaut" %% "argonaut" % "6.2-RC2"
  )
)

lazy val async = module("async").settings(
  name := asyncName,
  testSetting,
  buildInfoPackage := "httpz.async",
  buildInfoObject := "BuildInfoHttpzAsync",
  libraryDependencies ++= Seq(
    "com.ning" % "async-http-client" % "1.9.40"
  )
).dependsOn(httpz, tests % "test")

lazy val scalaj = module("scalaj").settings(
  name := scalajName,
  testSetting,
  buildInfoPackage := "httpz.scalajhttp",
  buildInfoObject := "BuildInfoHttpzScalaj",
  libraryDependencies ++= Seq(
    "org.scalaj" %% "scalaj-http" % "2.3.0"
  )
).dependsOn(httpz, tests % "test")

lazy val apache = module("apache").settings(
  name := apacheName,
  testSetting,
  buildInfoPackage := "httpz.apachehttp",
  buildInfoObject := "BuildInfoHttpzApache",
  libraryDependencies ++= Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.5.2"
  )
).dependsOn(httpz, tests % "test")

lazy val nativeClient = module("native-client").settings(
  name := nativeClientName,
  buildInfoPackage := "httpz.native",
  buildInfoObject := "BuildInfoHttpzNative",
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  libraryDependencies ++= (
    ("junit"                % "junit"              % "4.12"   % "test") ::
    ("com.novocode"         % "junit-interface"    % "0.11"   % "test") ::
    ("com.github.kristofa"  % "mock-http-server"   % "4.1"    % "test") ::
    Nil
  )
)

lazy val native = Project("native", file("native")).settings(
  Common.baseSettings,
  name := nativeName,
  testSetting
).dependsOn(httpz, nativeClient, tests % "test")

lazy val tests = Project("tests", file("tests")).settings(
  Common.baseSettings,
  libraryDependencies ++= {
    ("filter" :: "jetty" :: Nil).map(m =>
      "ws.unfiltered" %% s"unfiltered-$m" % "0.9.0-beta2"
    )
  },
  publishArtifact := false,
  publish := {},
  publishLocal := {}
).dependsOn(httpz)

lazy val root = {
  import sbtunidoc.Plugin._

  Project("root", file(".")).settings(
    Common.baseSettings,
    unidocSettings,
    name := "httpz-all",
    artifacts := Nil,
    packagedArtifacts := Map.empty,
    artifacts ++= Classpaths.artifactDefs(Seq(packageDoc in Compile)).value,
    packagedArtifacts ++= Classpaths.packaged(Seq(packageDoc in Compile)).value,
    Sxr.settings1,
    Defaults.packageTaskSettings(
      packageDoc in Compile, (UnidocKeys.unidoc in Compile).map{_.flatMap(Path.allSubpaths)}
    ),
    Sxr.settings2
  ).aggregate(httpz, scalaj, async, apache, native, nativeClient, tests)
}
