package ch.xavier
package signals

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.Transactor
import doobie.implicits.javasql.*
import doobie.implicits.javatime.*
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.syntax.string.toSqlInterpolator
import org.postgresql.util.PSQLException

import java.sql.Timestamp
import java.time.LocalDateTime
import ch.xavier.signals.Signal

object SignalsRepository {
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )

  private var cachedSignals: List[Signal] = List()
  
  //Executed the first time in Application
  def getSignals: List[Signal] =
    if cachedSignals.isEmpty then
      cachedSignals = cacheSignals

    cachedSignals

  private def cacheSignals: List[Signal] =
    sql"select entry_price, first_target_price, entry_price < first_target_price, stop_loss, symbol, timestamp from signals"
      .query[Signal]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
}
