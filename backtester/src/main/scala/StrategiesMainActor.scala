package ch.xavier

import Application.{executionContext, system}
import strategy.{BacktestersSpawnerActor, StrategiesFactory}

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object StrategiesMainActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new StrategiesMainActor(context))
}

final case class Result(strategyName: String, averageProfitsInPercent: Double, numberOfTrade: Int, profitsPerTrade: Double)

class StrategiesMainActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val logger: Logger = LoggerFactory.getLogger("StrategiesMainActor")
  implicit val timeout: Timeout = 300.seconds

  private val strategiesFactory: StrategiesFactory.type = StrategiesFactory
  private val backtestersSpawnerRef: ActorRef[Message] = context.spawn(BacktestersSpawnerActor(), "backtesters-spawner-actor")
  private var results: ListBuffer[Result] = ListBuffer()

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case StartBacktestingMessage(strategyNames: List[String], symbols: List[String], minutesPerQuote: Int) =>
        context.log.info("-----------------------------------------------------------------------------------------")
        context.log.info(s"Quotes cached for all signals, now starting to backtest the strategies:$strategyNames")
        context.log.info("-----------------------------------------------------------------------------------------")

        //TODO: Implement that for each symbol so you get results displayed per symbol. What about the minutes per quotes? You should have the results for each as well.
        Source(strategiesFactory.getAllStrategiesVariantsNames(strategyNames))
          .mapAsync(8)(strategyName => backtestersSpawnerRef ? (replyTo => BacktestStrategyMessage(strategyName, symbols, minutesPerQuote, replyTo)))
          .map(_.asInstanceOf[ResultOfBacktestingStrategyMessage])
          .filter(_.averageProfitsInPercent != 0.0)
          .map(result => results = results += Result(result.strategyName, result.averageProfitsInPercent, result.numberOfTrade, result.averageProfitsInPercent / result.numberOfTrade.toDouble))
          .runWith(Sink.last)
          .onComplete {
            case Success(result) =>
              logger.info("")
              logger.info("CONTROL:")
              results.toList.filter(result => result.strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets"))
                .foreach(result => logger.info(s"Strategy:${result.strategyName} with ${result.numberOfTrade} trades and a gain of ${result.averageProfitsInPercent}% with an profit per trade of ${result.profitsPerTrade}%"))
              logger.info("")

              logger.info("The results sorted by profits per trade are:")
              val resultsSortedByProfitsPerTrade = results
                .filter(result => !result.strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets"))
                .filter(_.profitsPerTrade > 0)
                .sortWith(_.profitsPerTrade > _.profitsPerTrade)
                .take(100)
              for result <- resultsSortedByProfitsPerTrade do
                logger.info(s"Strategy:${result.strategyName} with ${result.numberOfTrade} trades and a gain of ${result.averageProfitsInPercent}% with an profit per trade of ${result.averageProfitsInPercent / result.numberOfTrade.toDouble}%")
              logger.info("")
              logger.info("")
              logger.info("The results sorted by average gain are:")
              val resultsSortedByGain = results
                .filter(result => !result.strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets"))
                .filter(_.averageProfitsInPercent > 0)
                .sortWith(_.averageProfitsInPercent > _.averageProfitsInPercent)
                .take(100)
              for result <- resultsSortedByGain do
                logger.info(s"Strategy:${result.strategyName} with ${result.numberOfTrade} trades and a gain of ${result.averageProfitsInPercent}% with an profit per trade of ${result.averageProfitsInPercent / result.numberOfTrade.toDouble}%")

              logger.info("")
              logger.info("Backtesting done, have a great day!")
            case Failure(e) => logger.error("Exception received in StrategiesMainActor:" + e)
          }

        this

  def format_result_number(gain: Double): String =
    BigDecimal(gain).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble.toString
}
