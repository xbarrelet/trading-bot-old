package ch.xavier

import Application.{executionContext, system}
import quote.QuotesFetcherActor
import signals.{FollowersSpawnerActor, Signal, SignalFollowerActor}
import trading.TradingActor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

object RestActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new RestActor(context))
}

class RestActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val logger: Logger = LoggerFactory.getLogger("RestActor")
  val followersSpawnerRef: ActorRef[Message] = context.spawn(FollowersSpawnerActor(), s"followers-spawner-actor")

  val route: Route =
    concat(
      get {
        pathSingleSlash {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Trading bot is up!\n"))
        }
      },
      post {
        path("signal") {
          entity(as[String]) { receivedSignal =>
            val jsonSignal = receivedSignal.parseJson.convertTo[JsValue].asJsObject
            val signal: Signal = Signal(
              jsonSignal.getFields("entryPrice").head.toString.toDouble,
              jsonSignal.getFields("firstTargetPrice").head.toString.toDouble,
              jsonSignal.getFields("secondTargetPrice").head.toString.toDouble,
              jsonSignal.getFields("thirdTargetPrice").head.toString.toDouble,
              jsonSignal.getFields("isLong").head.toString.toBoolean,
              jsonSignal.getFields("stopLoss").head.toString.toDouble,
              jsonSignal.getFields("symbol").head.toString.replace("\"", ""),
              false
            )

            followersSpawnerRef ! ProcessSignalMessage(signal)

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Signal received\n"))
          }
        }
      },
      post {
        path("signals" / "follow") {
          entity(as[String]) { receivedSignals =>
            val jsonSignals = receivedSignals.parseJson.convertTo[Seq[JsValue]]
            for jsonSignal <- jsonSignals do
              val signal: Signal = Signal(
                jsonSignal.asJsObject.getFields("entryPrice").head.toString.toDouble,
                jsonSignal.asJsObject.getFields("firstTargetPrice").head.toString.toDouble,
                jsonSignal.asJsObject.getFields("secondTargetPrice").head.toString.toDouble,
                jsonSignal.asJsObject.getFields("thirdTargetPrice").head.toString.toDouble,
                jsonSignal.asJsObject.getFields("isLong").head.toString.toBoolean,
                jsonSignal.asJsObject.getFields("stopLoss").head.toString.toDouble,
                jsonSignal.asJsObject.getFields("symbol").head.toString.replace("\"", ""),
                true
              )
              followersSpawnerRef ! ProcessSignalMessage(signal)

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Signals followed\n"))
          }
        }
      },
      put {
        path("leverage" / IntNumber) { leverage =>
          logger.info(s"Now using new leverage of $leverage")
          DynamicConfig.leverage = leverage
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Leverage changed to $leverage\n"))
        }
      },
      put {
        path("amount-per-trade" / IntNumber) { amountPerTrade =>
          logger.info(s"Now using new amount per trade of $amountPerTrade")
          DynamicConfig.amountPerTrade = amountPerTrade
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"Amount per trade changed to $amountPerTrade\n"))
        }
      }
    )

  Http().newServerAt("0.0.0.0", 8090).bind(route)

  override def onMessage(message: Message): Behavior[Message] =
    this
}