package ch.xavier

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import strategy.TRStratsSpawnerActor

import ch.xavier.trading.interfaces.BybitTradingAPI

object Application extends App {
  implicit val system: ActorSystem[BotMessage] = ActorSystem(Main(), "System")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
}


object Main {
  def apply(): Behavior[BotMessage] =
    Behaviors.setup(context => new Main(context))
}

class Main(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
  context.log.info("Starting trading bot and waiting for signals to profit from")
  context.log.info("-----------------------------------------------------------------------------------------")

//  val mainActor: ActorRef[BotMessage] = context.spawn(RestActor(), "rest-actor")

  val mainActor: ActorRef[BotMessage] = context.spawn(TRStratsSpawnerActor(), "strats-spawner-actor")

  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    this
}