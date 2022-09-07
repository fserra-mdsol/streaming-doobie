package com.mdsol

import cats.effect.{IO, IOApp, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import common._

object server extends IOApp.Simple {

  val server: Resource[IO, Server] = EmberServerBuilder
    .default[IO]
    .withHost(host"localhost")
    .withPort(port"8080")
    .withHttpApp(httpApp)
    .build

  override def run: IO[Unit] =
    server.useForever.void

}
