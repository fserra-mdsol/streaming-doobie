package com.mdsol

import cats.effect.{IO, IOApp, Resource}
import doobie.{ExecutionContexts, Meta}
import doobie.hikari.HikariTransactor
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import doobie.syntax._
import doobie.implicits._
import doobie.postgres.implicits._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._
import doobie.util.transactor.Transactor
import org.http4s.client.Client
import org.http4s.server.Server


object streamings extends IOApp.Simple {

  val transactor = Transactor.fromDriverManager[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/streamings",
        "postgres",
        "")

  case class Foo(i: Int,s: String)
  object Foo {
    implicit val fooCodec: Codec[Foo] = deriveCodec
  }

  case class Bar(i: Int,s: String)
  object Bar {
    implicit val fooCodec: Codec[Bar] = deriveCodec
  }

  val selectBars = sql"select * from bars".query[Bar].stream.transact(transactor)


  val route = HttpRoutes.of[IO] {
    case GET -> Root => Ok(sql"select * from foos".query[Foo].stream.transact(transactor))
  }

  val httpApp = route.orNotFound

  val client = Client.fromHttpApp(httpApp)

  override def run: IO[Unit] = {
    val server: Resource[IO, Server] = EmberServerBuilder
      .default[IO]
      .withHost(host"localhost")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build



  }
}
