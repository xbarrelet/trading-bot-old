package ch.xavier
package Quote

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

  val numberOfDaysOfQuotesToFetch = 7

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case CacheQuotes(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) =>

        if !quotesRepository.areQuotesCached(symbol, fromTimestamp) then
          context.log.info(s"Fetching quotes for symbol:$symbol")
          val fetcherRef: ActorRef[Message] = context.spawn(QuotesFetcherActor(), s"fetcher-actor-$symbol-$fromTimestamp")

          var startTimestamp: Long = fromTimestamp - 8640
          val endTimestamp: Long = fromTimestamp + numberOfDaysOfQuotesToFetch * 86400
          val startTimestamps: ListBuffer[Long] = ListBuffer()

          while (startTimestamp < endTimestamp) {
            startTimestamps += startTimestamp
            startTimestamp += 11940L
          }

          Source(startTimestamps.toList)
            .mapAsync(1)(startTimestamp => {
              val response: Future[Message] = fetcherRef ? (replyTo => FetchQuotes(symbol, startTimestamp, replyTo))
              response.asInstanceOf[Future[QuotesReady]]
            })
            .map(message => quotesRepository.insertQuotes(message.quotes))
            .runWith(Sink.last)
            .onComplete {
              case Success(done) => actorRef ! QuotesCached()
              case Failure(e) => println("failure:" + e)
            }
        else
          actorRef ! QuotesCached()

    this
}
