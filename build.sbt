import sbtcrossproject.{ CrossType, crossProject }

organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

val circeVersion = "0.13.0"
val drosteVersion = "0.8.0"
val previousCirceDrosteVersion = "0.1.0"

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val baseSettings = Seq(
  addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  scalacOptions ++= compilerOptions,
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value))
      Seq(
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-unused-import"
      )
    else
      Seq(
        "-Ywarn-unused:imports"
      )
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  coverageHighlighting := true,
  (scalastyleSources in Compile) ++= (unmanagedSourceDirectories in Compile).value
)

val allSettings = baseSettings ++ publishSettings

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

val root = project
  .in(file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-jawn" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion
    )
  )
  .aggregate(patternJVM, patternJS, drosteJVM, drosteJS)
  .dependsOn(drosteJVM)

lazy val pattern = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("pattern"))
  .settings(allSettings)
  .settings(
    moduleName := "circe-pattern",
    mimaPreviousArtifacts := Set(), // "io.circe" %% "circe-pattern" % previousCirceDrosteVersion),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "org.scalacheck" %%% "scalacheck" % "1.15.0" % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % "3.1.4.0" % Test,
      "org.typelevel" %%% "discipline-scalatest" % "1.0.1" % Test
    ),
    ghpagesNoJekyll := true,
    docMappingsApiDir := "api",
    addMappingsToSiteDir(mappings in (Compile, packageDoc), docMappingsApiDir)
  )

lazy val patternJVM = pattern.jvm
lazy val patternJS = pattern.js

lazy val droste = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("droste"))
  .settings(allSettings)
  .settings(
    moduleName := "circe-droste",
    mimaPreviousArtifacts := Set(), //"io.circe" %% "circe-droste" % previousCirceDrosteVersion),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion % Test,
      "io.circe" %%% "circe-parser" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "io.higherkindness" %%% "droste-core" % drosteVersion,
      "io.higherkindness" %%% "droste-laws" % drosteVersion % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % "3.1.4.0" % Test,
      "org.typelevel" %%% "discipline-scalatest" % "1.0.1" % Test
    ),
    ghpagesNoJekyll := true,
    docMappingsApiDir := "api",
    addMappingsToSiteDir(mappings in (Compile, packageDoc), docMappingsApiDir)
  )
  .dependsOn(pattern, pattern % "test->test")

lazy val drosteJVM = droste.jvm
lazy val drosteJS = droste.js

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/circe/circe-droste")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://circe.github.io/circe-droste/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/circe/circe-droste"),
      "scm:git:git@github.com:circe/circe-droste.git"
    )
  ),
  developers := List(
    Developer(
      "travisbrown",
      "Travis Brown",
      "travisrobertbrown@gmail.com",
      url("https://twitter.com/travisbrown")
    )
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
