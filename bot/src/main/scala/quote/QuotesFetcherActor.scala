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

import java.time.Instant
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object QuotesFetcherActor {
  def apply(): Behavior[BotMessage] =
    Behaviors.setup(context => new QuotesFetcherActor(context))
}

class QuotesFetcherActor(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
  private val logger: Logger = LoggerFactory.getLogger("QuotesFetcherActor")
  private val http: HttpExt = Http()
  private val urlTemplate: String = "https://api.bybit.com/public/linear/kline?symbol=$SYMBOLUSDT&interval=1&from=$START_TIMESTAMP"

  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    message match {
      case FetchQuotesMessage(symbol: String, numberOfQuotes: Int, replyTo: ActorRef[BotMessage]) =>
        val timestampInSeconds: Long = Instant.now.getEpochSecond - (numberOfQuotes + 1) * 60

        val url = urlTemplate.replace("$SYMBOL", symbol).replace("$START_TIMESTAMP", timestampInSeconds.toString)
        val response: Future[HttpResponse] = http.singleRequest(HttpRequest(uri = url))

        response.map {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            response.entity.toStrict(30.seconds)
              .map(entity => entity.getData().utf8String)
              .map(body => body.parseJson.convertTo[JsValue].asJsObject)
              .map(jsonBody => jsonBody.getFields("result").head)
              .map(_.convertTo[Seq[JsValue]])
              .onComplete {
                case Success(list) =>
                  replyTo ! QuotesFetchedMessage(
                    list.map(quote => Quote(
                      quote.asJsObject.getFields("close").head.toString.toDouble,
                      quote.asJsObject.getFields("high").head.toString.toDouble,
                      quote.asJsObject.getFields("low").head.toString.toDouble,
                      quote.asJsObject.getFields("open").head.toString.toDouble,
                      quote.asJsObject.getFields("open_time").head.toString.toLong,
                      symbol, true)).toList
                  )
                case Failure(failure) =>
                  logger.error(s"Couldn't fetch quote for symbol:$symbol with url:$url, let's wait 1 more minute for the next one")
                  replyTo ! QuotesFetchedMessage(List(Quote(0, 0, 0, 0, 0, "EMPTY", false)))
              }
          case _ => logger.error(s"Problem encountered when fetching the quotes for $symbol and timestamp $timestampInSeconds")
        }

        this
    }
}
