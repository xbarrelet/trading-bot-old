package ch.xavier
package signals

import signals.Signal

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.Transactor
import doobie.implicits.javasql.*
import doobie.implicits.javatime.*
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.syntax.string.toSqlInterpolator

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime}

object SignalsRepository {
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )


  def insertSignal(signal: Signal): Unit =
    sql"INSERT INTO public.signals (entry_price, first_target_price, stop_loss, symbol, timestamp) VALUES (${signal.entryPrice}, ${signal.firstTargetPrice}, ${signal.stopLoss}, ${signal.symbol}, ${Instant.now.getEpochSecond})"
    .update
    .run
    .transact(transactor)
    .unsafeRunSync()
}
