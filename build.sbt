import java.nio.file.{Files, StandardCopyOption}

val dockerGraalvmNative = taskKey[Unit]("Create a docker image containing a binary build with GraalVM's native-image.")
val dockerGraalvmNativeImageName = settingKey[String]("Name of the generated docker image, containing the native binary.")

lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.graalvm",
  scalaVersion := "2.12.8"
)

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "graalvm-tests")
  .aggregate(core)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .settings(
    name := "core",
    packageName in Docker := "docker-test",
    dockerUpdateLatest := true,
    dockerGraalvmNativeImageName := "docker-graalvm-native-test",
    dockerGraalvmNative := {
      val log = streams.value.log

      val stageDir = target.value / "native-docker" / "stage"
      stageDir.mkdirs()

      // copy all jars to the staging directory
      val cpDir = stageDir / "cp"
      cpDir.mkdirs()

      val classpathJars = Seq((packageBin in Compile).value) ++ (dependencyClasspath in Compile).value.map(_.data)
      classpathJars.foreach(cpJar => Files.copy(cpJar.toPath, (cpDir / cpJar.name).toPath, StandardCopyOption.REPLACE_EXISTING))

      val resultDir = stageDir / "result"
      resultDir.mkdirs()
      val resultName = "out"

      val className = (mainClass in Compile).value.getOrElse(sys.error("Could not find a main class."))

      val runNativeImageCommand = Seq(
        "docker",
        "run",
        "--rm",
        "-v",
        s"${cpDir.getAbsolutePath}:/opt/cp",
        "-v",
        s"${resultDir.getAbsolutePath}:/opt/graalvm",
        "graalvm-native-image",
        "-cp",
        "/opt/cp/*",
        "--static",
        // "--report-unsupported-elements-at-runtime",
        s"-H:Name=$resultName",
        className
      )

      log.info("Running native-image using the 'graalvm-native-image' docker container")
      log.info(s"Running: ${runNativeImageCommand.mkString(" ")}")

      sys.process.Process(runNativeImageCommand, resultDir) ! streams.value.log match {
        case 0 => resultDir / resultName
        case r => sys.error(s"Failed to run docker, exit status: " + r)
      }

      val buildContainerCommand = Seq(
        "docker",
        "build",
        "-t",
        dockerGraalvmNativeImageName.value,
        "-f",
        (baseDirectory.value.getParentFile / "run-native-image" / "Dockerfile").getAbsolutePath,
        resultDir.absolutePath
      )
      log.info("Building the container with the generated native image")
      log.info(s"Running: ${buildContainerCommand.mkString(" ")}")

      sys.process.Process(buildContainerCommand, resultDir) ! streams.value.log match {
        case 0 => resultDir / resultName
        case r => sys.error(s"Failed to run docker, exit status: " + r)
      }

      log.info(s"Build image ${dockerGraalvmNativeImageName.value}")
    },
    graalVMNativeImageOptions := Seq("--report-unsupported-elements-at-runtime"),
    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "3.0.0-RC2"
    )
  )
