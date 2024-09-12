package ch.xavier
package quote

import cats.effect.IO
import doobie.{Fragment, Transactor}
import cats.effect.unsafe.implicits.global
import doobie.syntax.string.toSqlInterpolator
import doobie.syntax.connectionio.toConnectionIOOps
import doobie.util.update.Update
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Instant, LocalDateTime, ZoneOffset}

object QuotesRepository {
  private val logger: Logger = LoggerFactory.getLogger("QuotesRepository")
  private val transactor: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5429/data",
    "root",
    "toor"
  )
  private var cachedQuotes: Map[String, Map[Long, List[Quote]]] = Map()


  def getQuotes(symbol: String, minutesPerQuote: Long): List[Quote] =
    if !cachedQuotes.contains(symbol) then
      return List()
    if !cachedQuotes(symbol).contains(minutesPerQuote) then
      return List()

    cachedQuotes(symbol)(minutesPerQuote)

  def areQuotesAvailable(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long, quoteMinutes: Long): Boolean =
    if cachedQuotes.contains(symbol) then
      if cachedQuotes(symbol).contains(quoteMinutes) then
        return true

    val areQuotesAvailable: Boolean = areQuotesPresent(symbol, startTimestampInSeconds, endTimestampInSeconds, quoteMinutes)
    if areQuotesAvailable then
      var quotesPerTimestamp: Map[Long, List[Quote]] = Map(quoteMinutes -> getQuotesFromRepository(symbol, startTimestampInSeconds, endTimestampInSeconds, quoteMinutes))

      if cachedQuotes.contains(symbol) then
        val currentQuotes = cachedQuotes(symbol)
        quotesPerTimestamp = quotesPerTimestamp.++(currentQuotes)

      cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)
      logger.debug(s"Quotes already present for symbol:$symbol and date:" +
        s"${LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimestampInSeconds), ZoneOffset.ofHours(8))} and are now cached")
      true
    else
      false

  private def areQuotesPresent(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long, quoteMinutes: Long): Boolean =
    val first_timestamp_present: Boolean =  Fragment.const(s"select count(*) from quotes_${quoteMinutes}m where start_timestamp > ${startTimestampInSeconds - 1000} and start_timestamp < ${startTimestampInSeconds + 1000}  and symbol = '$symbol'")
      .query[Int]
      .unique
      .transact(transactor)
      .unsafeRunSync() > 0

    val second_timestamp_present: Boolean =  Fragment.const(s"select count(*) from quotes_${quoteMinutes}m where start_timestamp > ${endTimestampInSeconds - 1000} and start_timestamp < ${endTimestampInSeconds + 1000}  and symbol = '$symbol'")
      .query[Int]
      .unique
      .transact(transactor)
      .unsafeRunSync() > 0

    first_timestamp_present && second_timestamp_present

  def cacheQuotes(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long, quoteMinutes: Long): Unit =
    val quotesPerTimestamp: Map[Long, List[Quote]] = Map(startTimestampInSeconds -> getQuotesFromRepository(symbol, startTimestampInSeconds, endTimestampInSeconds, quoteMinutes))
    cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)

  private def getQuotesFromRepository(symbol: String, startTimestampInSeconds: Long, endTimestampInSeconds: Long, quoteMinutes: Long): List[Quote] =
    Fragment.const(s"select close, high, low, open, start_timestamp, symbol from quotes_${quoteMinutes}m where symbol = '$symbol' and start_timestamp > $startTimestampInSeconds and start_timestamp <= $endTimestampInSeconds order by start_timestamp asc")
      .query[Quote]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
  

  def insertQuotes(quotes: Set[Quote], quoteMinutes: Long): Unit =
    val query = s"INSERT INTO quotes_${quoteMinutes}m (close, high, low, open, start_timestamp, symbol)  VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING"
    Update[Quote](query)
      .updateMany(quotes.toList)
      .transact(transactor)
      .unsafeRunSync()
}
