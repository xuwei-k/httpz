import sbt._, Keys._
import sbtrelease._
import xerial.sbt.Sonatype._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys
import sbtbuildinfo.Plugin._

object build extends Build {

  def gitHash: Option[String] = scala.util.Try(
    sys.process.Process("git rev-parse HEAD").lines_!.head
  ).toOption

  val showDoc = TaskKey[Unit]("showDoc")

  val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

  val testSetting = TaskKey[Unit]("runTests") := (run in Test).toTask("").value

  val updateReadme = { state: State =>
    val extracted = Project.extract(state)
    val scalaV = extracted get scalaBinaryVersion
    val v = extracted get version
    val org =  extracted get organization
    val modules = ("native" :: "scalaj" :: "apache" :: "dispatch" :: "async" :: Nil).map("httpz-" + _)
    val snapshotOrRelease = if(extracted get isSnapshot) "snapshots" else "releases"
    val readme = "README.md"
    val readmeFile = file(readme)
    val newReadme = Predef.augmentString(IO.read(readmeFile)).lines.map{ line =>
      val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == v.contains("SNAPSHOT")
      if(line.startsWith("libraryDependencies") && matchReleaseOrSnapshot){
        val i = modules.indexWhere(line.contains)
        s"""libraryDependencies += "${org}" %% "${modules(i)}" % "$v""""
      }else if(line.contains(sonatypeURL) && matchReleaseOrSnapshot){
        val n = "httpz-all"
        s"- [API Documentation](${sonatypeURL}${snapshotOrRelease}/archive/${org.replace('.','/')}/${n}_${scalaV}/${v}/${n}_${scalaV}-${v}-javadoc.jar/!/index.html)"
      }else line
    }.mkString("", "\n", "\n")
    IO.write(readmeFile, newReadme)
    val git = new Git(extracted get baseDirectory)
    git.add(readme) ! state.log
    git.commit("update " + readme) ! state.log
    "git diff HEAD^" ! state.log
    state
  }

  val updateReadmeProcess: ReleaseStep = updateReadme

  final val ScalazVersion = "7.1.0"

  val baseSettings = ReleasePlugin.releaseSettings ++ sonatypeSettings ++ buildInfoSettings ++ Seq(
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
    commands += Command.command("updateReadme")(updateReadme),
    ReleasePlugin.ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      updateReadmeProcess,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project extract state
          extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
        },
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      ReleaseStep{ state =>
        val extracted = Project extract state
        extracted.runAggregated(SonatypeKeys.sonatypeReleaseAll in Global in extracted.get(thisProjectRef), state)
      },
      updateReadmeProcess,
      pushChanges
    ),
    credentials ++= PartialFunction.condOpt(sys.env.get("SONATYPE_USER") -> sys.env.get("SONATYPE_PASS")){
      case (Some(user), Some(pass)) =>
        Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass)
    }.toList,
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/xuwei-k/httpz")),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    scalacOptions ++= (
      "-deprecation" ::
      "-unchecked" ::
      "-Xlint" ::
      "-language:existentials" ::
      "-language:higherKinds" ::
      "-language:implicitConversions" ::
      Nil
    ),
    scalacOptions ++= {
      if(scalaVersion.value.startsWith("2.11"))
        Seq("-Ywarn-unused", "-Ywarn-unused-import")
      else
        Nil
    },
    scalaVersion := "2.11.4",
    crossScalaVersions := scalaVersion.value :: "2.10.4" :: Nil,
    scalacOptions in (Compile, doc) ++= {
      val tag = if(isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }
      Seq(
        "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
        "-doc-source-url", s"https://github.com/xuwei-k/httpz/tree/${tag}€{FILE_PATH}.scala"
      )
    },
    logBuffered in Test := false,
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
        <tag>{if(isSnapshot.value) gitHash.getOrElse("master") else { "v" + version.value }}</tag>
      </scm>
    ),
    fork in Test := true,
    incOptions := incOptions.value.withNameHashing(true),
    description := "purely functional http client",
    pomPostProcess := { node =>
      import scala.xml._
      import scala.xml.transform._
      def stripIf(f: Node => Boolean) = new RewriteRule {
        override def transform(n: Node) =
          if (f(n)) NodeSeq.Empty else n
      }
      val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
      new RuleTransformer(stripTestScope).transform(node)(0)
    },
    showDoc in Compile <<= (doc in Compile, target in doc in Compile) map { (_, out) =>
      java.awt.Desktop.getDesktop.open(out / "index.html")
    }
  )

  lazy val httpz = Project("httpz", file("httpz")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz",
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz",
    buildInfoObject := "BuildInfoHttpz",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-concurrent" % ScalazVersion,
      "io.argonaut" %% "argonaut" % "6.1-M4"
    )
  )

  lazy val async = Project("async", file("async")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-async",
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.async",
    buildInfoObject := "BuildInfoHttpzAsync",
    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.8.14"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val scalaj = Project("scalaj", file("scalaj")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-scalaj",
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.scalajhttp",
    buildInfoObject := "BuildInfoHttpzScalaj",
    libraryDependencies ++= Seq(
      "org.scalaj" %% "scalaj-http" % "0.3.16"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val dispatch = Project("dispatch", file("dispatch")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-dispatch",
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.dispatchclassic",
    buildInfoObject := "BuildInfoHttpzDispatch",
    libraryDependencies ++= Seq(
      "net.databinder" %% "dispatch-http" % "0.8.10"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val apache = Project("apache", file("apache")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-apache",
    testSetting,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.apachehttp",
    buildInfoObject := "BuildInfoHttpzApache",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.3.5"
    )
  ).dependsOn(httpz, tests % "test")

  lazy val nativeClient = Project("native-client", file("native-client")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-native-client",
    sourceGenerators in Compile <+= buildInfo,
    buildInfoPackage := "httpz.native",
    buildInfoObject := "BuildInfoHttpzNative",
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    libraryDependencies ++= (
      ("junit"                % "junit"              % "4.11"   % "test") ::
      ("com.novocode"         % "junit-interface"    % "0.11"   % "test") ::
      ("com.github.kristofa"  % "mock-http-server"   % "4.0"    % "test") ::
      Nil
    )
  )

  lazy val native = Project("native", file("native")).settings(
    baseSettings : _*
  ).settings(
    name := "httpz-native",
    testSetting
  ).dependsOn(httpz, nativeClient, tests % "test")

  lazy val tests = Project("tests", file("tests")).settings(
    baseSettings : _*
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

    Project("root", file(".")).settings(
      baseSettings ++ unidocSettings ++ Seq(
        name := "httpz-all",
        artifacts <<= Classpaths.artifactDefs(Seq(packageDoc in Compile)),
        packagedArtifacts <<= Classpaths.packaged(Seq(packageDoc in Compile))
      ) ++ Defaults.packageTaskSettings(
        packageDoc in Compile, (UnidocKeys.unidoc in Compile).map{_.flatMap(Path.allSubpaths)}
      ): _*
    ).aggregate(httpz, scalaj, async, dispatch, apache, native, nativeClient, tests)
  }


}

