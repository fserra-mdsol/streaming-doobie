
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val doobie = "1.0.0-RC1"
val http4s = "0.23.13"
val circe = "0.14.1"

def doobie(artifact: String): ModuleID = "org.tpolecat" %% s"doobie-$artifact" % doobie
def http4s(artifact: String): ModuleID = "org.http4s"   %% s"http4s-$artifact" % http4s
def circe(artifact: String): ModuleID = "io.circe"      %% s"circe-$artifact"  % circe

val doobieCore = doobie("core")
val doobiePostgres = doobie("postgres")
val doobieHikari = doobie("hikari")
val http4sDsl = http4s("dsl")
val http4sServer = http4s("ember-server")
val http4sClient = http4s("ember-client")
val http4sCirce = http4s("circe")

val circeGeneric = circe("generic")
val circeGenericExtras = circe("generic-extras")
val circeParser = circe("parser")

lazy val root = (project in file("."))
  .settings(
    name := "streaming-doobie",
    libraryDependencies ++= Seq(
      doobieCore,
      doobiePostgres,
      doobieHikari,
      http4sDsl,
      http4sServer,
      http4sClient,
      http4sCirce,
      circeGeneric,
      circeGenericExtras,
      circeParser
    )
  )
