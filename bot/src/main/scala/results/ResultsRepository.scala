package ch.xavier
package results

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.Transactor
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.syntax.string.toSqlInterpolator
import doobie.util.update.Update
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Instant, LocalDateTime, ZoneOffset}

object ResultsRepository {
  private val logger: Logger = LoggerFactory.getLogger("ResultsRepository")
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://results-bot-db.cfmszstktgv7.ap-southeast-1.rds.amazonaws.com:5432/results",
    "postgres",
    "postgrespostgrespostgres"
  )


  def insertResultWithStartResult(symbol: String, entryPrice: Double, startTimestamp: Long, strategyName: String, isLong: Boolean): Unit =
    sql"INSERT INTO results (symbol, entry_price, start_timestamp, strategy_name, is_long)  VALUES ($symbol, $entryPrice, $startTimestamp, $strategyName, $isLong)"
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()

  def updateResultWithEndValues(symbol: String, strategyName: String, exitPrice: Double, exitTimestamp: Long): Unit =
    sql"update results set exit_price=$exitPrice, exit_timestamp=$exitTimestamp where symbol = $symbol and strategy_name = $strategyName and start_timestamp = (select max(start_timestamp) from results where symbol = $symbol and strategy_name = $strategyName)"
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()
}
