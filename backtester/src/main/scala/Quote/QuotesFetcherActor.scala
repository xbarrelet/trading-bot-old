package ch.xavier
package Quote

import Application.{executionContext, system}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

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
      case FetchQuotes(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) =>
        val quotes: ListBuffer[Quote] = ListBuffer()

        val response: Future[HttpResponse] = http.singleRequest(HttpRequest(uri =
          urlTemplate.replace("$SYMBOL", symbol).replace("$START_TIMESTAMP", fromTimestamp.toString)))

        response.map {
          case response@HttpResponse(StatusCodes.OK, _, _, _) =>
            response.entity.toStrict(3.seconds)
              .map(entity => entity.getData().utf8String)
              .map(body => body.parseJson.convertTo[JsValue].asJsObject)
              .map(jsonBody => jsonBody.getFields("result").head)
              .map(_.convertTo[Seq[JsValue]])
              .onComplete {
                case Success(list) =>
                  for quote <- list do
                    quotes += Quote(
                      quote.asJsObject.getFields("close").head.toString.toDouble,
                      quote.asJsObject.getFields("high").head.toString.toDouble,
                      quote.asJsObject.getFields("low").head.toString.toDouble,
                      quote.asJsObject.getFields("open").head.toString.toDouble,
                      quote.asJsObject.getFields("open_time").head.toString.toLong,
                      symbol
                    )

                  actorRef ! QuotesReady(quotes.toList)
              }
          case _ => println(s"Problem encountered when fetching the quotes for $symbol and timestamp $fromTimestamp")
        }

        this
    }
}
