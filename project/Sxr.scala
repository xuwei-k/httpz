import sbt._, Keys._

object Sxr {

  val packageSxr = TaskKey[File]("packageSxr")
  val disableSxr = sys.props.isDefinedAt("disable_sxr")

  println("disable_sxr = " + disableSxr)

  def commonSettings(c: Configuration, out: String): Seq[Def.Setting[_]] = {
    Defaults.packageTaskSettings(
      packageSxr in c, (crossTarget in c).map{ dir =>
        Path.allSubpaths(dir / out).toSeq
      }
    ) ++ Seq[Def.Setting[_]](
      resolvers += "bintray/paulp" at "https://dl.bintray.com/paulp/maven",
      addCompilerPlugin("org.improving" %% "sxr" % "1.0.1"),
      packageSxr in c <<= (packageSxr in c).dependsOn(compile in c),
      packagedArtifacts <++= Classpaths.packaged(Seq(packageSxr in c)),
      artifacts <++= Classpaths.artifactDefs(Seq(packageSxr in c)),
      artifactClassifier in packageSxr := Some("sxr")
    )
  }

  def subProjectSxr(c: Configuration, out: String): Seq[Def.Setting[_]] = {
    if(disableSxr){
      Nil
    }else{
      commonSettings(c, out) ++ Seq(
        scalacOptions in c <+= scalaSource in c map {
          "-P:sxr:base-directory:" + _.getAbsolutePath
        }
      )
    }
  }

}
