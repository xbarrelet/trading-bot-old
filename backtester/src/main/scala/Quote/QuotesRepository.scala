package ch.xavier
package Quote

import cats.effect.IO
import doobie.Transactor
import cats.effect.unsafe.implicits.global
import doobie.syntax.string.toSqlInterpolator
import doobie.syntax.connectionio.toConnectionIOOps
import org.postgresql.util.PSQLException

object QuotesRepository {
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )

  def getQuotes(symbol: String, startTimestampInSeconds: Long): List[Quote] =
    val timestampTwoWeeksAfterStartTimestamp: Long = startTimestampInSeconds + 1209600

    sql"select close, high, low, open, start_timestamp, symbol from quotes where symbol = $symbol and start_timestamp > $startTimestampInSeconds and start_timestamp < $timestampTwoWeeksAfterStartTimestamp order by start_timestamp asc"
      .query[Quote]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()

  def insertQuotes(quotes: List[Quote]): Unit =
    quotes.foreach(quote =>
      try {
        insertQuote(quote.symbol, quote.start_timestamp, quote.low, quote.high, quote.open, quote.close)
      }
      catch {
        case e: PSQLException =>
      }
    )


  private def insertQuote(symbol: String, startTimestamp: Long, low: Double, high: Double, open: Double,
                  close: Double): Unit =
    sql"INSERT INTO quotes (symbol, start_timestamp, low, high, open, close)  VALUES ($symbol, $startTimestamp, $low, $high, $open, $close)"
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()


}
