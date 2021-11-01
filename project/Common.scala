import sbt._, Keys._
import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import ReleaseStateTransformations._
import com.jsuereth.sbtpgp.PgpKeys
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

object Common {

  private def gitHash: Option[String] =
    scala.util
      .Try(
        sys.process.Process("git rev-parse HEAD").lineStream_!.head
      )
      .toOption

  def ScalazVersion = "7.3.5"

  private[this] val unusedWarnings = Def.setting(
    Seq("-Ywarn-unused:imports")
  )

  private[this] val Scala212 = "2.12.15"

  val baseSettings = Seq(
    fullResolvers ~= { _.filterNot(_.name == "jcenter") },
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      scalaVersion,
      sbtVersion,
      scalacOptions,
      licenses,
      "scalazVersion" -> ScalazVersion
    ),
    commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      UpdateReadme.updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project extract state
          extracted.runAggregated(extracted.get(thisProjectRef) / (Global / PgpKeys.publishSigned), state)
        },
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      UpdateReadme.updateReadmeProcess,
      pushChanges
    ),
    credentials ++= PartialFunction
      .condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")) { case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
      }
      .toList,
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/xuwei-k/httpz")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
    ),
    scalacOptions ++= PartialFunction
      .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
        case Some((2, v)) if v <= 12 =>
          Seq(
            "-Ypartial-unification",
            "-Xfuture",
          )
      }
      .toList
      .flatten,
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "3" =>
          Nil
        case _ =>
          Seq("-Xlint") ++ unusedWarnings.value
      }
    },
    scalaVersion := Scala212,
    crossScalaVersions := Scala212 :: "2.13.7" :: "3.1.0" :: Nil,
    (Compile / doc / scalacOptions) ++= {
      val tag = if (isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/xuwei-k/httpz/tree/${tag}€{FILE_PATH}.scala"
      )
    },
    Test / logBuffered := false,
    pomExtra := (
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:xuwei-k/httpz.git</url>
        <connection>scm:git:git@github.com:xuwei-k/httpz.git</connection>
        <tag>{if (isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }}</tag>
      </scm>
    ),
    Test / fork := true,
    description := "purely functional http client",
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) =
        new RewriteRule {
          override def transform(n: Node) =
            if (f(n)) NodeSeq.Empty else n
        }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    }
  ) ++ Seq(Compile, Test).flatMap(c => (c / console / scalacOptions) --= unusedWarnings.value)

}
