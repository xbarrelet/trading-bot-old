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
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object QuotesActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new QuotesActor(context))
}


class QuotesActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val quotesRepository: QuotesRepository.type = QuotesRepository
  implicit val timeout: Timeout = 30.seconds

  val numberOfDaysOfQuotesToFetch = 30

  YOU ONLY GET 43383 quotes per signal which is one day of quotes (43380), check why not 30 days as planned

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case CacheQuotesMessage(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) =>

        if !quotesRepository.areQuotesCached(symbol, fromTimestamp) then
          context.log.info(s"Fetching quotes for symbol:$symbol and timestamp:$fromTimestamp")
          val fetcherRef: ActorRef[Message] = context.spawn(QuotesFetcherActor(), s"fetcher-actor-$symbol-$fromTimestamp")

          var startTimestamp: Long = fromTimestamp
          val endTimestamp: Long = fromTimestamp + numberOfDaysOfQuotesToFetch * 86400
          val startTimestamps: ListBuffer[Long] = ListBuffer()

          while (startTimestamp < endTimestamp) {
            startTimestamps += startTimestamp
            startTimestamp += 11940L
          }

          Source(startTimestamps.toList)
            .mapAsync(1)(startTimestamp => {
              val response: Future[Message] = fetcherRef ? (replyTo => FetchQuotesMessage(symbol, startTimestamp, replyTo))
              response.asInstanceOf[Future[QuotesReadyMessage]]
            })
            .map(message => quotesRepository.insertQuotes(message.quotes))
            .runWith(Sink.last)
            .onComplete {
              case Success(done) =>
                quotesRepository.cacheQuotes(symbol, fromTimestamp)
                actorRef ! QuotesCachedMessage()
              case Failure(e) => println("Exception received in QuotesActor:" + e)
            }
        else
          actorRef ! QuotesCachedMessage()
        this

      case ShutdownMessage() =>
        Behaviors.stopped
}
