package ch.xavier
package quote

import Application.{executionContext, system}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}


object BinanceQuotesFetcherActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new BinanceQuotesFetcherActor(context))
}

class BinanceQuotesFetcherActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  private val logger: Logger = LoggerFactory.getLogger("QuotesFetcherActor")
  private val http: HttpExt = Http()
  private val urlTemplate: String = "https://api1.binance.com/api/v3/klines?symbol=$SYMBOLUSDT&interval=$MINUTESm&limit=1000&startTime=$START_TIMESTAMP000"

  override def onMessage(message: Message): Behavior[Message] =
    message match {
      case FetchQuotesMessage(symbol: String, fromTimestamp: Long, minutesPerQuote: Long, actorRef: ActorRef[Message]) =>

        var quotes: Set[Quote] = Set()

        val response: Future[HttpResponse] = http.singleRequest(HttpRequest(
          uri = urlTemplate
            .replace("$SYMBOL", symbol)
            .replace("$START_TIMESTAMP", fromTimestamp.toString)
            .replace("$MINUTES", minutesPerQuote.toString)))

          response.map {
            case response@HttpResponse(StatusCodes.OK, _, _, _) =>
              response.entity.toStrict(60.seconds)
                .map(entity => entity.getData().utf8String)
                .map(body => body.parseJson.convertTo[Seq[Seq[JsValue]]])
                .onComplete {
                  case Success(list) =>
                    for quote <- list do
                      quotes = quotes +  Quote(
                        quote(4).convertTo[String].toDouble,
                        quote(2).convertTo[String].toDouble,
                        quote(3).convertTo[String].toDouble,
                        quote(1).convertTo[String].toDouble,
                        quote.head.convertTo[Long] / 1000,
                        symbol
                      )

                    actorRef ! QuotesReadyMessage(quotes)
                  case Failure(exception) =>
                    logger.error(s"Problem when fetching the quotes for symbol:$symbol and timestamp:$fromTimestamp:" + exception.getMessage)
                    actorRef ! QuotesReadyMessage(Set())
                }
            case _ => logger.error(s"Problem encountered when fetching the quotes for $symbol and timestamp $fromTimestamp")
          }

        this
    }
}
