package ch.xavier
package quote

import Application.{executionContext, system}

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Framing, Sink, Source}
import akka.util.Timeout
import org.postgresql.util.PSQLException

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, blocking}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import java.text.SimpleDateFormat
import java.time.{Instant, LocalDateTime, ZoneOffset}

object QuotesActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new QuotesActor(context))
}


class QuotesActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  private val quotesRepository: QuotesRepository.type = QuotesRepository
  implicit val timeout: Timeout = 30.seconds

  private val SECONDS_PER_DAY = 86400

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case CacheQuotesMessage(symbol: String, daysToBacktestOn: Long, minutesPerQuote: Int, actorRef: ActorRef[Message]) =>

        val startTimestamp: Long = System.currentTimeMillis() / 1000 - daysToBacktestOn * SECONDS_PER_DAY
        val endTimestamp: Long = System.currentTimeMillis() / 1000
        val startTimestamps: ListBuffer[Long] = ListBuffer()

        var startLoopTimestamp = startTimestamp
        while (startLoopTimestamp < endTimestamp) {
          startTimestamps += startLoopTimestamp
          startLoopTimestamp += minutesPerQuote * 60 * 1000
        }

        if !quotesRepository.areQuotesAvailable(symbol, startTimestamps.head, startTimestamps.last, minutesPerQuote) then
          context.log.info(s"Fetching ${minutesPerQuote}m quotes for symbol:$symbol for the last $daysToBacktestOn days")
          val fetcherRef: ActorRef[Message] = context.spawn(BinanceQuotesFetcherActor(), s"fetcher-actor-$symbol-$minutesPerQuote")
          
          Source(startTimestamps.toList)
            .mapAsync(1)(startTimestamp => {
              val response: Future[Message] = fetcherRef ? (replyTo => FetchQuotesMessage(symbol, startTimestamp, minutesPerQuote, replyTo))
              response.asInstanceOf[Future[QuotesReadyMessage]]
            })
            .map(message => quotesRepository.insertQuotes(message.quotes, minutesPerQuote))
            .runWith(Sink.last)
            .onComplete {
              case Success(_) =>
                quotesRepository.cacheQuotes(symbol, startTimestamps.head, startTimestamps.last, minutesPerQuote)
                println("All quotes have been downloaded and cached")
                actorRef ! QuotesCachedMessage()
              case Failure(e) =>
                println("Exception received in QuotesActor:" + e)
                actorRef ! QuotesCachedMessage()
            }
        else
          context.log.info(s"${minutesPerQuote}m quotes already present for symbol:$symbol from $startTimestamp to $endTimestamp")
          actorRef ! QuotesCachedMessage()

        this

      case ShutdownMessage() =>
        Behaviors.stopped


  private def formatTimestamp(timestamp: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.ofHours(8))
}
