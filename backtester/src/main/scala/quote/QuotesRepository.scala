package ch.xavier
package quote

import cats.effect.IO
import doobie.Transactor
import cats.effect.unsafe.implicits.global
import doobie.syntax.string.toSqlInterpolator
import doobie.syntax.connectionio.toConnectionIOOps
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Instant, LocalDateTime, ZoneOffset}

object QuotesRepository {
  val logger: Logger = LoggerFactory.getLogger("QuotesRepository")
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5430/data",
    "root",
    "toor"
  )

  private var cachedQuotes: Map[String, Map[Long, List[Quote]]] = Map()


  def getQuotes(symbol: String, startTimestampInSeconds: Long): List[Quote] =
    if !cachedQuotes.contains(symbol) then
      return List()
    if !cachedQuotes(symbol).contains(startTimestampInSeconds) then
      return List()

    cachedQuotes(symbol)(startTimestampInSeconds)

  def areQuotesAvailable(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long): Boolean =
    if cachedQuotes.contains(symbol) then
      if cachedQuotes(symbol).contains(startTimestampInSeconds) then
        return true

    val areQuotesAvailable: Boolean = areQuotesPresent(symbol, startTimestampInSeconds, endTimestampInSeconds)
    if areQuotesAvailable then
      var quotesPerTimestamp: Map[Long, List[Quote]] = Map(startTimestampInSeconds -> getQuotesFromRepository(symbol, startTimestampInSeconds, endTimestampInSeconds))

      if cachedQuotes.contains(symbol) then
        val currentQuotes = cachedQuotes(symbol)
        quotesPerTimestamp = quotesPerTimestamp.++(currentQuotes)

      cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)
      logger.info(s"Quotes already present for symbol:$symbol and date:" +
        s"${LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimestampInSeconds), ZoneOffset.ofHours(8))} and are now cached")
      true
    else
      false

  private def areQuotesPresent(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long): Boolean =
    val first_timestamp_present: Boolean =  sql"select count(*) from quotes where start_timestamp > ${startTimestampInSeconds - 1000} and start_timestamp < ${startTimestampInSeconds + 1000}  and symbol = $symbol"
      .query[Int]
      .unique
      .transact(transactor)
      .unsafeRunSync() > 0
    
    val second_timestamp_present: Boolean =  sql"select count(*) from quotes where start_timestamp > ${endTimestampInSeconds - 1000} and start_timestamp < ${endTimestampInSeconds + 1000}  and symbol = $symbol"
      .query[Int]
      .unique
      .transact(transactor)
      .unsafeRunSync() > 0

    first_timestamp_present && second_timestamp_present

  def cacheQuotes(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long): Unit =
    val quotesPerTimestamp: Map[Long, List[Quote]] = Map(startTimestampInSeconds -> getQuotesFromRepository(symbol, startTimestampInSeconds, endTimestampInSeconds))
    cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)

  private def getQuotesFromRepository(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long): List[Quote] =
    sql"select close, high, low, open, start_timestamp, symbol from quotes where symbol = $symbol and start_timestamp > $startTimestampInSeconds and start_timestamp <= $endTimestampInSeconds order by start_timestamp asc"
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
