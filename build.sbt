name := """themis-scala"""

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

val slickVersion = "3.1.1"
libraryDependencies ++= List(

  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen" % slickVersion,
  "org.slf4j" % "slf4j-nop" % "1.7.19",
  "com.h2database" % "h2" % "1.4.191",

  "mysql" % "mysql-connector-java" % "6.0.5",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc4",
  //  "postgresql" % "postgresql" % "9.3", //-1102.jdbc41",
  //"postgresql" % "postgresql" % "9.4.1208-jdbc42-atlassian-hosted",

  "com.typesafe.play" %% "play-slick" % "2.0.0"
)

libraryDependencies ++= List(
  "org.bitbucket.b_c" % "jose4j" % "0.4.4"
)




// code generation task
lazy val slick = TaskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir = (dir / "slick").getPath // place generated files in sbt's managed sources folder
  managedSourceDirectories += (dir / "slick")

  //  val url = "jdbc:h2:mem:play;INIT=runscript from 'sql/create.sql'"
  //  val jdbcDriver = "org.h2.Driver"
  //  val slickDriver = "slick.driver.H2Driver"


  val slickDriver = "slick.driver.PostgresDriver"
  //  val url = "jdbc:postgresql://localhost:5432/batman"
  val url = "jdbc:postgresql://localhost:5432/themis?user=themis&password=themis"
  val jdbcDriver = "org.postgresql.Driver"

  val pkg = "db"
  toError(r.run("slick.codegen.SourceCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg), s.log))
  val fname = outputDir + "/db/Tables.scala"
  Seq(file(fname))
}

slick <<= slickCodeGenTask // register manual sbt command
sourceGenerators in Compile <+= slickCodeGenTask // register automatic code generation on every compile, remove for only manual use



//fork in run := true
//fork in run := true