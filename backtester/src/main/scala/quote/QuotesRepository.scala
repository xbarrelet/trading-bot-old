package ch.xavier
package quote

import cats.effect.IO
import doobie.Transactor
import cats.effect.unsafe.implicits.global
import doobie.syntax.string.toSqlInterpolator
import doobie.syntax.connectionio.toConnectionIOOps
import org.postgresql.util.PSQLException
import org.slf4j.{Logger, LoggerFactory}

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

  def areQuotesCached(symbol: String, startTimestampInSeconds: Long): Boolean =
    if cachedQuotes.contains(symbol) then
      if cachedQuotes(symbol).contains(startTimestampInSeconds) then
        return true

    val quotes: List[Quote] = getQuotesFromRepository(symbol, startTimestampInSeconds)
    if quotes.length > 1000 then
      var quotesPerTimestamp: Map[Long, List[Quote]] = Map(startTimestampInSeconds -> getQuotesFromRepository(symbol, startTimestampInSeconds))

      if cachedQuotes.contains(symbol) then
        val currentQuotes = cachedQuotes(symbol)
        quotesPerTimestamp = quotesPerTimestamp.++(currentQuotes)

      cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)
      logger.info(s"Quotes already present for symbol:$symbol and timestamp:$startTimestampInSeconds and are now cached")
      true
    else
      false

  def cacheQuotes(symbol: String, startTimestampInSeconds: Long): Unit =
    val quotesPerTimestamp: Map[Long, List[Quote]] = Map(startTimestampInSeconds -> getQuotesFromRepository(symbol, startTimestampInSeconds))
    cachedQuotes = cachedQuotes + (symbol -> quotesPerTimestamp)

  private def getQuotesFromRepository(symbol: String, startTimestampInSeconds: Long): List[Quote] =
    val timestampOneDayBeforeStartTimestamp: Long = startTimestampInSeconds - 86400
    val timestampTwoWeeksAfterStartTimestamp: Long = startTimestampInSeconds + 1209600

    sql"select close, high, low, open, start_timestamp, symbol from quotes where symbol = $symbol and start_timestamp > $timestampOneDayBeforeStartTimestamp and start_timestamp < $timestampTwoWeeksAfterStartTimestamp order by start_timestamp asc"
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
