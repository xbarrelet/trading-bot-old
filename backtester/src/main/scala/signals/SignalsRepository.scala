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
import org.slf4j.{Logger, LoggerFactory}

object SignalsRepository {
  private val logger: Logger = LoggerFactory.getLogger("SignalsRepository")

  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )

  private var cachedSignals: List[Signal] = List()
  
  //Executed the first time in Application
  def getSignals(startTimestamp: Long = 1): List[Signal] =
    if cachedSignals.isEmpty then
      cachedSignals = cacheSignalsOlderThanOneMonth(startTimestamp)
      logger.info(s"${cachedSignals.length} signals are now cached")

    cachedSignals

  private def cacheSignalsOlderThanOneMonth(startTimestamp: Double): List[Signal] =
//    sql"select entry_price, first_target_price, second_target_price, third_target_price, is_long, stop_loss, symbol, timestamp from signals where signal_id = 4" //TEST FOR STRAT
    sql"select entry_price, first_target_price, second_target_price, third_target_price, is_long, stop_loss, symbol, timestamp from signals where signal_id = 297" //BNB 1 Year fake signal
//    sql"select entry_price, first_target_price, second_target_price, third_target_price, is_long, stop_loss, symbol, timestamp from signals where timestamp < ${(System.currentTimeMillis / 1000) - 2419200} and timestamp > $startTimestamp order by timestamp desc"
      .query[Signal]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
}
