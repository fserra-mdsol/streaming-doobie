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
    implicit val barCodec: Codec[Bar] = deriveCodec
  }

  case class Baz(i: Int,s: String)
  object Baz {
    implicit val barCodec: Codec[Baz] = deriveCodec
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

  val createBazz =
    sql"""
          create table if not exists bazz(
            i bigint,
            s varchar
          )
       """.update.run

  def insertInto(table: String)(asMany: Int) = Update[(Int,String)](s"insert into $table (i,s) values (?,?)").updateMany(
    List.range(1,asMany).map(i => i -> s"$i")
  )

  def insertBars(asMany: Int) = Update[(Int,String)]("insert into bars (i,s) values (?,?)").updateMany(
    List.range(1,asMany).map(i => i -> s"$i")
  )

  val selectBarsStream = sql"select * from bars".query[Bar].stream

  def insertBaz(baz: List[Baz]) = Update[Baz](s"insert into bazz (i,s) values (?, ?)").updateMany(baz)



  val client = Client.fromHttpApp(httpApp)

  override def run: IO[Unit] = {
    val request = Request[IO](GET, uri"/")

    val asMany1 = 5001
    val asMany2 = 10001

    (for {
      _ <- Stream.eval(createFoos.transact(transactor))
      _ <- Stream.eval(IO.println("created table foos"))
      _ <- Stream.eval(createBarss.transact(transactor))
      _ <- Stream.eval(IO.println("created table bars"))
      _ <- Stream.eval(createBazz.transact(transactor))
      _ <- Stream.eval(IO.println("created table bazz"))
      _ <- Stream.eval(insertInto("foos")(asMany1).transact(transactor))
      _ <- Stream.eval(IO.println(s"inserted $asMany1 foos"))
      _ <- Stream.eval(insertInto("bars")(asMany2).transact(transactor))
      _ <- Stream.eval(IO.println(s"inserted $asMany2 bars"))
      _ <- client.stream(request).flatMap(_.body.chunks.parseJsonStream).flatMap { json =>
        Stream.eval(json.as[Foo].liftTo[IO].flatMap(foo => (if (foo.i % 1000 == 0) IO.println(s"received $foo") else IO.unit) *> IO.pure(foo)))
      }.flatMap( foo =>
        selectBarsStream.transact(transactor)
                        .map(bar => Baz(foo.i, s"""${foo.s + " " + bar.s}"""))
                        .evalTap(baz => if (baz.i % 1000 == 0) IO.println(s"emitting $baz") else IO.unit)
      ).chunkN(10000).flatMap{ baz =>
        val list = baz.toList
        Stream.eval(IO.println(s"************************* inserting ${list.length} Bazs") *> insertBaz(list).transact(transactor))
      }
    } yield ()).compile.drain
  }
}
