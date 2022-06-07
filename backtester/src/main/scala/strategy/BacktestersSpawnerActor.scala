package ch.xavier
package strategy

import Application.{executionContext, system}
import signals.{Signal, SignalsRepository}

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import ch.xavier.strategy.advanced.AdvancedStrategyBacktesterActor
import ch.xavier.strategy.simple.SimpleStrategyBacktesterActor

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}



object BacktestersSpawnerActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new BacktestersSpawnerActor(context))
}

class BacktestersSpawnerActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  implicit val timeout: Timeout = 300.seconds

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) =>
//        val ref: ActorRef[Message] = context.spawn(SimpleStrategyBacktesterActor(), "BacktesterActor_for_" + strategyName)
        val ref: ActorRef[Message] = context.spawn(AdvancedStrategyBacktesterActor(), "BacktesterActor_for_" + strategyName)
        val response: Future[Message] =  ref ? (myRef => BacktestStrategyMessage(strategyName, myRef))

        response.onComplete {
          case Success(result: Message) => replyTo ! result
          case Failure(ex) => println(s"Problem encountered when backtesting strategy:$strategyName : ${ex.getMessage}")
        }
        
        this
}
