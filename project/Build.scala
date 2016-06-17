import sbt._, Keys._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._
import scalaprops.ScalapropsPlugin.autoImport._

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

  def module(id: String) =
    Project(id, file(id)).settings(
      Common.baseSettings,
      BuildInfoPlugin.projectSettings
    )

  lazy val httpz = module("httpz").settings(
    scalapropsWithScalazlaws,
    name := httpzName,
    scalapropsVersion := "0.1.17",
    buildInfoPackage := "httpz",
    buildInfoObject := "BuildInfoHttpz",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-concurrent" % Common.ScalazVersion,
      "io.argonaut" %% "argonaut" % "6.1"
    )
  )

  lazy val async = module("async").settings(
    name := asyncName,
    testSetting,
    buildInfoPackage := "httpz.async",
    buildInfoObject := "BuildInfoHttpzAsync",
    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.9.38"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val scalaj = module("scalaj").settings(
    name := scalajName,
    testSetting,
    buildInfoPackage := "httpz.scalajhttp",
    buildInfoObject := "BuildInfoHttpzScalaj",
    libraryDependencies ++= Seq(
      "org.scalaj" %% "scalaj-http" % "2.2.1"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val dispatch = module("dispatch").settings(
    name := dispatchName,
    testSetting,
    buildInfoPackage := "httpz.dispatchclassic",
    buildInfoObject := "BuildInfoHttpzDispatch",
    libraryDependencies ++= Seq(
      "net.databinder" %% "dispatch-http" % "0.8.10"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val apache = module("apache").settings(
    name := apacheName,
    testSetting,
    buildInfoPackage := "httpz.apachehttp",
    buildInfoObject := "BuildInfoHttpzApache",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.5.1"
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
