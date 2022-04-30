package ch.xavier
package quote

import Application.{executionContext, system}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
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
  val http: HttpExt = Http()
  val urlTemplate: String = "https://api.bybit.com/public/linear/kline?symbol=$SYMBOLUSDT&interval=1&from=$START_TIMESTAMP"

  override def onMessage(message: Message): Behavior[Message] =
    message match {
      case FetchLastQuoteMessage(symbol: String, replyTo: ActorRef[Message]) =>
        val lastMinuteTimestampInSecond: Long = Instant.now.getEpochSecond - 60

        val response: Future[HttpResponse] = http.singleRequest(HttpRequest(uri =
          urlTemplate.replace("$SYMBOL", symbol).replace("$START_TIMESTAMP", lastMinuteTimestampInSecond.toString)))

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
              }
          case _ => println(s"Problem encountered when fetching the quotes for $symbol and timestamp $lastMinuteTimestampInSecond")
        }

        this
    }
}
