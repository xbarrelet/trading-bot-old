package ch.xavier

import Application.{executionContext, system}
import strategy.{BacktestersSpawnerActor, StrategiesFactory}

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object StrategiesMainActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new StrategiesMainActor(context))
}

class StrategiesMainActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val strategiesFactory: StrategiesFactory.type = StrategiesFactory
  val backtestersSpawnerRef: ActorRef[Message] = context.spawn(BacktestersSpawnerActor(), "backtesters-spawner-actor")
  implicit val timeout: Timeout = 300.seconds
  val logger: Logger = LoggerFactory.getLogger("StrategiesMainActor")

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case StartBacktestingMessage(strategyName: String) =>
        context.log.info("-----------------------------------------------------------------------------------------")
        context.log.info(s"Quotes cached for all signals, now starting to backtest the strategy:$strategyName")

        strategiesFactory.getStrategieVariantsName(strategyName)
          .mapAsync(1)(strategy => backtestersSpawnerRef ? (replyTo => BacktestStrategyMessage(strategy, replyTo)))
          .runWith(Sink.last)
          .onComplete {
            case Success(result) =>
              val averagePercentageGains = result.asInstanceOf[ResultOfBacktestingStrategyMessage].averageProfitsInPercent
              logger.info("")
              logger.info(s"Strategy:$strategyName has an overall result of ${BigDecimal(averagePercentageGains).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}")

            case Failure(e) => println("Exception received in StrategiesMainActor:" + e)
          }

        this
}
