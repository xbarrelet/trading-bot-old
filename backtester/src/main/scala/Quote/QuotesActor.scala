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
  val numberOfDaysOfQuotesToFetch = 1
  implicit val timeout: Timeout = 30.seconds

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case FetchQuotes(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) =>
        var quotesFromDb: List[Quote] = quotesRepository.getQuotes(symbol, fromTimestamp)
        context.log.info(s"length of quotes in db for symbol:$symbol : ${quotesFromDb.length}")

        if quotesFromDb.length < 100 then
          context.log.info(s"Fetching quotes for symbol:$symbol")
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
              val response: Future[Message] = fetcherRef ? (replyTo => FetchQuotes(symbol, startTimestamp, replyTo))
              response.asInstanceOf[Future[QuotesReady]]
            })
            .map(message => quotesRepository.insertQuotes(message.quotes))
            .runWith(Sink.last)
            .onComplete {
              case Success(done) =>
                if quotesFromDb.length < 100 then
                  quotesFromDb = quotesRepository.getQuotes(symbol, fromTimestamp)
                actorRef ! QuotesReady(quotesFromDb)
              case Failure(e) => println("failure:" + e)
            }
          
    this
}
