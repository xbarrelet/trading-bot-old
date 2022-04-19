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

object SignalsRepository {
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )

  def getSignals(symbol: String, startTimestampInSeconds: Long): List[Signal] =
    val timestampTwoWeeksAfterStartTimestamp: Long = startTimestampInSeconds + 1209600

    sql"select emission_date, entry_price, stop_loss, symbol, target_price from signals"
      .query[Signal]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
}
