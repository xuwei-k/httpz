import sbt._, Keys._
import sbtbuildinfo.BuildInfoPlugin
import scalaprops.ScalapropsPlugin.autoImport._

object build {

  val testSetting = TaskKey[Unit]("runTests") := (run in Test).toTask("").value

  final val httpzName = "httpz"
  final val asyncName = "httpz-async"
  final val scalajName = "httpz-scalaj"
  final val apacheName = "httpz-apache"
  final val nativeClientName = "httpz-native-client"
  final val nativeName = "httpz-native"
  val modules = (
    httpzName ::
    asyncName ::
    scalajName ::
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

}
