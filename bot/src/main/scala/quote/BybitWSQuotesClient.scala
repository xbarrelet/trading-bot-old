package ch.xavier
package quote

import Application.{executionContext, system}

import akka.actor.{Cancellable, actorRef2Scala}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.settings.{ClientConnectionSettings, WebSocketSettings}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{BoundedSourceQueue, OverflowStrategy}
import akka.util.ByteString
import akka.{Done, NotUsed}
import ch.qos.logback.classic.pattern.MessageConverter
import org.slf4j.{Logger, LoggerFactory}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, SECONDS}


class BybitWSQuotesClient {
  private val logger: Logger = LoggerFactory.getLogger("WSBybitQuotesClient")

  private var followerRefs: Map[String, List[ActorRef[BotMessage]]] = Map()

  private val QUOTES_FOLLOWING_MESSAGE = "{\"op\":\"subscribe\",\"args\":[\"candle.1.SYMBOLUSDT\"]}"
  private val request: WebSocketRequest = WebSocketRequest("wss://stream.bybit.com/realtime_public")

  private val incoming: Sink[Message, Future[Done]] =
    Sink.foreach {
      case message: TextMessage.Strict =>
        if message.text.contains("data") then
          val symbol = message.text.split("candle.1.")(1).split("USDT")(0)
          val data = message.text.parseJson.convertTo[JsValue].asJsObject.getFields("data").head
          val jsonQuote = data.convertTo[Seq[JsValue]].head
          val quote: Quote = Quote(
            jsonQuote.asJsObject.getFields("close").head.toString.toDouble,
            jsonQuote.asJsObject.getFields("high").head.toString.toDouble,
            jsonQuote.asJsObject.getFields("low").head.toString.toDouble,
            jsonQuote.asJsObject.getFields("open").head.toString.toDouble,
            jsonQuote.asJsObject.getFields("start").head.toString.toLong,
            symbol,
            jsonQuote.asJsObject.getFields("confirm").head.toString.toBoolean
          )
          followerRefs(symbol).foreach(actorRef => actorRef ! QuoteFetchedMessage(quote))


      case _ =>
        logger.warn(f"Received non TextMessage")
    }

  def followSymbol(symbol: String, followerRef: ActorRef[BotMessage]): Unit = this.synchronized {
    if followerRefs.contains(symbol) then
      followerRefs = followerRefs.updated(symbol, followerRef :: followerRefs(symbol))
    else
      followerRefs = followerRefs + (symbol -> List(followerRef))
      startFollowingSymbol(symbol)
  }


  private def startFollowingSymbol(symbol: String): Unit =
    val webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = Http().webSocketClientFlow(request)
    val outgoing: Source[Message, NotUsed] = Source.single(getQuotesFollowingMessage(symbol)).concat(Source.maybe)

    val (upgradeResponse, closed) =
      outgoing
        .viaMat(webSocketFlow)(Keep.right)
        .toMat(incoming)(Keep.both)
        .run()
    
    upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed to Bybit Websocket with http status: ${upgrade.response.status}")
      }
    }

  private def getQuotesFollowingMessage(symbol: String): TextMessage =
    TextMessage(QUOTES_FOLLOWING_MESSAGE.replace("SYMBOL", symbol.toUpperCase))
}
