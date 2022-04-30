package ch.xavier

import Application.{executionContext, system}
import quote.QuotesFetcherActor
import signals.{FollowersSpawnerActor, Signal, SignalFollowerActor, SignalsRepository}
import trading.TradingActor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

object RestActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new RestActor(context))
}

class RestActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val signalsRepository: SignalsRepository.type = SignalsRepository
  val followersSpawnerRef: ActorRef[Message] = context.spawn(FollowersSpawnerActor(), s"followers-spawner-actor")

  val route: Route =
    concat(
      get {
        pathSingleSlash {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Trading bot is up!"))
        }
      },
      post {
        path("signal") {
          entity(as[String]) { receivedSignal =>
            val jsonSignal = receivedSignal.parseJson.convertTo[JsValue].asJsObject
            val signal: Signal = Signal(
              jsonSignal.getFields("entryPrice").head.toString.toDouble,
              jsonSignal.getFields("firstTargetPrice").head.toString.toDouble,
              jsonSignal.getFields("isLong").head.toString.toBoolean,
              jsonSignal.getFields("stopLoss").head.toString.toDouble,
              jsonSignal.getFields("symbol").head.toString.replace("\"", "")
            )

//            signalsRepository.insertSignal(signal)
            followersSpawnerRef ! ProcessSignalMessage(signal)

            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Signal received"))
          }
        }
      }
    )

  Http().newServerAt("localhost", 8080).bind(route)

  override def onMessage(message: Message): Behavior[Message] =
    this
}