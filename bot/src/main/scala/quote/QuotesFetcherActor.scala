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
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new QuotesFetcherActor(context))
}

class QuotesFetcherActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val logger: Logger = LoggerFactory.getLogger("QuotesFetcherActor")
  val http: HttpExt = Http()
  val urlTemplate: String = "https://api.bybit.com/public/linear/kline?symbol=$SYMBOLUSDT&interval=1&from=$START_TIMESTAMP"

  override def onMessage(message: Message): Behavior[Message] =
    message match {
      case FetchLastQuoteMessage(symbol: String, replyTo: ActorRef[Message]) =>
        var lastMinuteTimestampInSecond: Long = Instant.now.getEpochSecond - 60
        if symbol.equals("LTC") || symbol.equals("LINK") then
          lastMinuteTimestampInSecond = Instant.now.getEpochSecond - 90

        val url = urlTemplate.replace("$SYMBOL", symbol).replace("$START_TIMESTAMP", lastMinuteTimestampInSecond.toString)
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
                  if list != null then
                    replyTo ! QuoteFetchedMessage(Quote(
                      list.last.asJsObject.getFields("close").head.toString.toDouble,
                      list.last.asJsObject.getFields("high").head.toString.toDouble,
                      list.last.asJsObject.getFields("low").head.toString.toDouble,
                      list.last.asJsObject.getFields("open").head.toString.toDouble,
                      list.last.asJsObject.getFields("open_time").head.toString.toLong,
                      symbol
                    ))
                case Failure(failure) =>
                  logger.error(s"Couldn't fetch quote for symbol:$symbol with url:$url, let's wait 1 more minute for the next one")
                  replyTo ! QuoteFetchedMessage(Quote(0, 0, 0, 0, 0, "EMPTY"))
                  //TODO: Or schedule ici un nouveau message dans 5 seconds avec meme params pour self send
              }
          case _ => logger.error(s"Problem encountered when fetching the quotes for $symbol and timestamp $lastMinuteTimestampInSecond")
        }

        this
    }
}
