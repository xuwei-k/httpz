import sbt._, Keys._
import sbtbuildinfo.Plugin._

object build extends Build {

  val testSetting = TaskKey[Unit]("runTests") := (run in Test).toTask("").value

  private final val httpzName = "httpz"
  private final val asyncName = "httpz-async"
  private final val scalajName = "httpz-scalaj"
  private final val dispatchName = "httpz-dispatch"
  private final val apacheName = "httpz-apache"
  private final val nativeClientName = "httpz-native-client"
  private final val nativeName = "httpz-native"
  val modules = (
    httpzName ::
    asyncName ::
    scalajName ::
    dispatchName ::
    apacheName ::
    nativeClientName ::
    nativeName ::
    Nil
  )

  lazy val httpz = Project("httpz", file("httpz")).settings(
    Common.baseSettings : _*
  ).settings(
    name := httpzName,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz",
    buildInfoObject := "BuildInfoHttpz",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-concurrent" % Common.ScalazVersion,
      "io.argonaut" %% "argonaut" % "6.1-M4"
    )
  )

  lazy val async = Project("async", file("async")).settings(
    Common.baseSettings : _*
  ).settings(
    name := asyncName,
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.async",
    buildInfoObject := "BuildInfoHttpzAsync",
    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.9.1"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val scalaj = Project("scalaj", file("scalaj")).settings(
    Common.baseSettings : _*
  ).settings(
    name := scalajName,
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.scalajhttp",
    buildInfoObject := "BuildInfoHttpzScalaj",
    libraryDependencies ++= Seq(
      "org.scalaj" %% "scalaj-http" % "1.1.0"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val dispatch = Project("dispatch", file("dispatch")).settings(
    Common.baseSettings : _*
  ).settings(
    name := dispatchName,
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.dispatchclassic",
    buildInfoObject := "BuildInfoHttpzDispatch",
    libraryDependencies ++= Seq(
      "net.databinder" %% "dispatch-http" % "0.8.10"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val apache = Project("apache", file("apache")).settings(
    Common.baseSettings : _*
  ).settings(
    name := apacheName,
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.apachehttp",
    buildInfoObject := "BuildInfoHttpzApache",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.3.6"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val nativeClient = Project("native-client", file("native-client")).settings(
    Common.baseSettings : _*
  ).settings(
    name := nativeClientName,
    sourceGenerators in Compile <+= buildInfo,
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
    Common.baseSettings : _*
  ).settings(
    name := nativeName,
    testSetting
  ).dependsOn(httpz, nativeClient, tests % "test")

  lazy val tests = Project("tests", file("tests")).settings(
    Common.baseSettings : _*
  ).settings(
    libraryDependencies ++= ("filter" :: "jetty" :: Nil).map(m =>
      "net.databinder" %% s"unfiltered-$m" % "0.8.2"
    ),
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  ).dependsOn(httpz)

  lazy val root = {
    import sbtunidoc.Plugin._

    val sxrSettings = if(Sxr.disableSxr){
      Nil
    }else{
      Sxr.commonSettings(Compile, "unidoc.sxr") ++ Seq(
        Sxr.packageSxr in Compile <<= (Sxr.packageSxr in Compile).dependsOn(UnidocKeys.unidoc in Compile)
      ) ++ (
        httpz :: async :: scalaj :: dispatch :: apache :: nativeClient :: native :: Nil
      ).map(libraryDependencies <++= libraryDependencies in _)
    }

    Project("root", file(".")).settings(
      Common.baseSettings ++ unidocSettings ++ Seq(
        name := "httpz-all",
        artifacts := Nil,
        packagedArtifacts := Map.empty,
        artifacts <++= Classpaths.artifactDefs(Seq(packageDoc in Compile)),
        packagedArtifacts <++= Classpaths.packaged(Seq(packageDoc in Compile)),
        scalacOptions in UnidocKeys.unidoc += {
          "-P:sxr:base-directory:" + (sources in UnidocKeys.unidoc in ScalaUnidoc).value.mkString(":")
        }
      ) ++ Defaults.packageTaskSettings(
        packageDoc in Compile, (UnidocKeys.unidoc in Compile).map{_.flatMap(Path.allSubpaths)}
      ) ++ sxrSettings : _*
    ).aggregate(httpz, scalaj, async, dispatch, apache, native, nativeClient, tests)
  }

}
