package ch.xavier
package strategy

import Application.{executionContext, system}
import quote.{Quote, QuotesRepository}
import signals.{Signal, SignalsRepository}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.{Logger, LoggerFactory}
import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar, BaseBarSeriesBuilder}

import java.time.*
import java.util.Date
import scala.concurrent.Future
import scala.util.control.Breaks.{break, breakable}
import scala.util.{Failure, Success}

object StrategyBacktesterActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new StrategyBacktesterActor(context))
}

class StrategyBacktesterActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val quotesRepository: QuotesRepository.type = QuotesRepository
  val signalsRepository: SignalsRepository.type = SignalsRepository
  val strategiesFactory: StrategiesFactory.type = StrategiesFactory
  val logger: Logger = LoggerFactory.getLogger("StrategyBacktesterActor")

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) =>
        val signals: List[Signal] = signalsRepository.getSignals
        var tradesNotEnteredCounter = 0

        Source(signals)
          .map(signal => {
            val strategy: Strategy = strategiesFactory.getStrategyFromName(strategyName, signal)
            val quotes: List[Quote] = quotesRepository.getQuotes(signal.symbol, signal.timestamp)

            var hasEntered = false
            var entryPrice = 0.0
            var exitPrice = 0.0
            logger.debug(s"Now testing strategy:$strategyName with signal with symbol:${signal.symbol}, entryPrice:${signal.entryPrice}, " +
              s"stopLoss:${signal.stopLoss} and firstTargetPrice:${signal.firstTargetPrice}")

            breakable {
              for quote: Quote <- quotes do
                strategy.addQuote(quote)
                if !hasEntered then
                  if strategy.shouldEnter then
                    hasEntered = true
                    entryPrice = quote.close
                    logger.debug(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} " +
                      s"has entered with price:$entryPrice at start_timestamp:${formatTimestamp(quote.start_timestamp)}")
                else
                  if strategy.shouldExit then
                    exitPrice = quote.close
                    logger.debug(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} " +
                      s"has exited with price:$exitPrice at start_timestamp:${formatTimestamp(quote.start_timestamp)}")
                    break
            }

            var percentageGain = 0.0
            if entryPrice == 0.0 then
              tradesNotEnteredCounter += 1
              logger.debug("The trade was never entered")
            else
              if exitPrice == 0.0 then
                exitPrice = quotes.last.close
                logger.debug(s"The strategy is still active, exiting now the trade with price:$exitPrice")

              var profits = 0.0
              if signal.isLong then
                profits = exitPrice - entryPrice
              else
                profits = entryPrice - exitPrice

              percentageGain = profits * 100 / entryPrice
              logger.debug(s"Percentage gained with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} : " +
                s"${BigDecimal(percentageGain).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}%")

            logger.debug("------------------------------------------------------------------------------------------------------------------------------------------")
            strategy.applyLeverageToPercentageGain(percentageGain)
          })
          .reduce((acc, element) => acc + element)
          .runWith(Sink.last)
          .onComplete {
            case Success(sum) => replyTo ! ResultOfBacktestingStrategyMessage(strategyName, sum / (signals.length - tradesNotEnteredCounter))
            case Failure(e) => logger.error("Exception received in BacktesterActor:" + e)
          }


        this

  private def formatTimestamp(timestamp: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.ofHours(8))
}
