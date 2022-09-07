package com.mdsol

import cats.effect.IO
import com.mdsol.streamings.Foo
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.http4s.circe.CirceEntityCodec._

object common {

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/postgres",
    "postgres",
    "postgres"
  )

  val route = HttpRoutes.of[IO] {
    case GET -> Root => Ok(sql"select * from foos".query[Foo].stream.transact(transactor))
  }

  val httpApp = route.orNotFound
}
