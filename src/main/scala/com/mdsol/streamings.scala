package com.mdsol

import cats.syntax.all._
import cats.effect.{IO, IOApp}
import doobie._
import org.http4s.Request
import org.http4s.dsl.io._
import org.http4s.implicits._
import doobie.implicits._
import io.circe._
import io.circe.jawn.CirceSupportParser._
import io.circe.generic.semiauto._
import org.http4s.client.Client
import fs2._
import org.typelevel.jawn.fs2._
import common._

object streamings extends IOApp.Simple {


  case class Foo(i: Int,s: String)
  object Foo {
    implicit val fooCodec: Codec[Foo] = deriveCodec
  }

  case class Bar(i: Int,s: String)
  object Bar {
    implicit val fooCodec: Codec[Bar] = deriveCodec
  }

  val createFoos =
    sql"""
          create table if not exists foos(
            i bigint,
            s varchar
          )
       """.update.run
  val createBarss =
    sql"""
          create table if not exists bars(
            i bigint,
            s varchar
          )
       """.update.run

  def insertFoos(asMany: Int) = Update[(Int,String)]("insert into foos (i,s) values (?,?)").updateMany(
    List.range(1,asMany).map(i => i -> s"$i")
  )

  val selectFoosStream = sql"select * from foos".query[Foo].stream

  def insertBar(bar: Bar) = Update[Bar](s"insert into bars (i,s) values (?, ?)").run(bar)



  val client = Client.fromHttpApp(httpApp)

  override def run: IO[Unit] = {
    val request = Request[IO](GET, uri"/")

    val asMany = 10000
    
    (for {
      _ <- Stream.eval(createFoos.transact(transactor))
      _ <- Stream.eval(IO.println("created table foos"))
      _ <- Stream.eval(createBarss.transact(transactor))
      _ <- Stream.eval(IO.println("created table bars"))
      _ <- Stream.eval(insertFoos(asMany).transact(transactor))
      _ <- Stream.eval(IO.println(s"inserted $asMany foos"))
      _ <- client.stream(request).flatMap(_.body.chunks.parseJsonStream).flatMap { json =>
        Stream.eval(json.as[Foo].liftTo[IO].flatMap(foo => IO.println(s"received $foo") *> IO.pure(foo)))
      }.parZipWith(
        selectFoosStream.transact(transactor)
      ) { case (foo1, foo2) =>
        Bar(foo1.i + foo2.i, s"rec ${foo1.s} ${foo2.s}")
      }.flatMap(bar => Stream.eval(IO.println(s"inserting $bar") *> insertBar(bar).transact(transactor)))
    } yield ()).compile.drain
  }
}
