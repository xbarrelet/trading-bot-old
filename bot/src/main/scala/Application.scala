package ch.xavier

import Application.{executionContext, system}
import signals.{Signal, SignalFollowerActor, SignalListener}
import trading.TradingActor

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Application extends App {
  implicit val system: ActorSystem[Message] = ActorSystem(Main(), "System")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
}


object Main {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new Main(context))
}

class Main(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  context.log.info("Starting trading bot, waiting for signals to profit from")
  context.log.info("-----------------------------------------------------------------------------------------")

  val restServerActorRef: ActorRef[Message] = context.spawn(RestActor(), "rest-actor")
  

  override def onMessage(message: Message): Behavior[Message] =
    this
}