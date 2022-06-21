package ch.xavier
package strategy.advanced

import Application.{executionContext, system}
import quote.{Quote, QuotesRepository}
import signals.{Signal, SignalsRepository}
import strategy.simple.SimpleStrategy
import strategy.advanced.AdvancedStrategyBacktesterActor
import strategy.StrategiesFactory

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


object AdvancedStrategyBacktesterActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new AdvancedStrategyBacktesterActor(context))
}

class AdvancedStrategyBacktesterActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val quotesRepository: QuotesRepository.type = QuotesRepository
  val signalsRepository: SignalsRepository.type = SignalsRepository
  val strategiesFactory: StrategiesFactory.type = StrategiesFactory
  val logger: Logger = LoggerFactory.getLogger("AdvancedStrategyBacktesterActor")

  var WITH_EXTENSIVE_LOGGING = false

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) =>
        var tradesNotEnteredCounter = 0
        var numberOfTrade = 0

        val signals: List[Signal] = signalsRepository.getSignals()

        Source(signals)
          .map(signal => {
            val strategy: AdvancedStrategy = strategiesFactory.getAdvancedStrategyFromName(strategyName, signal)
//            if strategyName != "AdvancedMultiplePositionsLeveragedSS3T_with_leverage_10" then
//              WITH_EXTENSIVE_LOGGING = false
            val quotes: List[Quote] = quotesRepository.getQuotes(signal.symbol, signal.timestamp)

            var entryPrice = 0.0
            var exitPrice = 0.0

            var currentProfit: Double = 0.0
            var currentEntryPrice: Double = 0.0
            var currentExitPrice: Double = 0.0
            var hasLongPositionOpened = false
            var hasShortPositionOpened = false

            var hasEntered = false
            if WITH_EXTENSIVE_LOGGING then
              logger.info(s"Now testing strategy:$strategyName with signal with ${quotes.length} quotes and symbol:${signal.symbol}, isLong:${signal.isLong}, entryPrice:${signal.entryPrice}, " +
                s"stopLoss:${signal.stopLoss}, firstTargetPrice:${signal.firstTargetPrice}, secondTargetPrice:${signal.secondTargetPrice}, thirdTargetPrice:${signal.thirdTargetPrice}")

            breakable {
              for quote: Quote <- quotes do
//                if WITH_EXTENSIVE_LOGGING then
//                  logger.info(s"Adding close price:${quote.close} at ${formatTimestamp(quote.start_timestamp)}")
                strategy.addQuote(quote)

                if !hasEntered then
                  if strategy.shouldEnter then
                    hasEntered = true
                    entryPrice = quote.close
                    currentEntryPrice = quote.close
                    if signal.isLong then
                      hasLongPositionOpened = true
                    else
                      hasShortPositionOpened = true

                    if WITH_EXTENSIVE_LOGGING then
                      logger.info(s"Strategy:$strategyName with signal symbol:${signal.symbol} " +
                        s"has entered with price:$currentEntryPrice at start_timestamp:${formatTimestamp(quote.start_timestamp)}")
                else

                  if strategy.shouldExitCurrentTrade then
                    numberOfTrade += 1
                    if hasShortPositionOpened then
                      currentExitPrice = quote.close
                      currentProfit += currentEntryPrice - currentExitPrice
                      currentExitPrice = 0.0

                      hasShortPositionOpened = false
                      if WITH_EXTENSIVE_LOGGING then
                        logger.info(s"Closing short position for strat:$strategyName with signal symbol:${signal.symbol} " +
                          s"has exited with price:${quote.close} at start_timestamp:${formatTimestamp(quote.start_timestamp)}, current profit:$currentProfit")

                    if hasLongPositionOpened then
                      currentExitPrice = quote.close
                      currentProfit += currentExitPrice - currentEntryPrice
                      currentExitPrice = 0.0

                      hasLongPositionOpened = false
                      if WITH_EXTENSIVE_LOGGING then
                        logger.info(s"Closing long position for strat:$strategyName with signal symbol:${signal.symbol} " +
                          s"has exited with price:${quote.close} at start_timestamp:${formatTimestamp(quote.start_timestamp)}, current profit:$currentProfit")

                  if strategy.shouldBuyLong && !hasLongPositionOpened then
                    hasLongPositionOpened = true
                    currentEntryPrice = quote.close
                    if WITH_EXTENSIVE_LOGGING then
                      logger.info(s"Buying long position for strat:$strategyName with signal symbol:${signal.symbol} " +
                        s"at price $currentEntryPrice at start_timestamp:${formatTimestamp(quote.start_timestamp)}")

                  if strategy.shouldBuyShort && !hasShortPositionOpened then
                    hasShortPositionOpened = true
                    currentEntryPrice = quote.close
                    if WITH_EXTENSIVE_LOGGING then
                      logger.info(s"Buying short position for strat:$strategyName with signal symbol:${signal.symbol} " +
                        s"at price $currentEntryPrice at start_timestamp:${formatTimestamp(quote.start_timestamp)}")

                  if strategy.shouldExit then
                    exitPrice = quote.close
                    currentExitPrice = exitPrice
                    numberOfTrade += 1

                    if hasShortPositionOpened then
                      currentProfit += currentEntryPrice - currentExitPrice

                    if hasLongPositionOpened then
                      currentProfit += currentExitPrice - currentEntryPrice

                    hasLongPositionOpened = false
                    hasShortPositionOpened = false

                    if WITH_EXTENSIVE_LOGGING then
                      logger.info(s"Strategy:$strategyName with signal symbol:${signal.symbol} " +
                        s"has exited with price:${quote.close} at start_timestamp:${formatTimestamp(quote.start_timestamp)}")
                    break
            }

            var percentageGain = 0.0
            if entryPrice == 0.0 then
              tradesNotEnteredCounter += 1
              if WITH_EXTENSIVE_LOGGING then
               logger.info("The trade was never entered")
            else
              if hasShortPositionOpened then
                numberOfTrade += 1
                currentProfit += currentEntryPrice - quotes.last.close
                if WITH_EXTENSIVE_LOGGING then
                  logger.info(s"No more quote, closing the short position with currentProfit at $currentProfit")

              if hasLongPositionOpened then
                currentProfit += quotes.last.close - currentEntryPrice
                if WITH_EXTENSIVE_LOGGING then
                  logger.info(s"No more quote, closing the long position with currentProfit at $currentProfit")

//              if exitPrice == 0.0 then
//                exitPrice = quotes.last.close
//                if signal.isLong then
//                  currentProfit = exitPrice - entryPrice
//                else
//                  currentProfit = entryPrice - exitPrice
//
//                if WITH_EXTENSIVE_LOGGING then
//                  logger.info(s"The strategy $strategyName is still active, exiting now the trade with price:$exitPrice")

            percentageGain = currentProfit * 100 / entryPrice
//            if WITH_EXTENSIVE_LOGGING then
//              logger.info(s"Percentage gained with signal symbol:${signal.symbol} and timestamp:${formatTimestamp(signal.timestamp)} : " +
//                s"${BigDecimal(strategy.applyLeverageToPercentageGain(percentageGain)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}%")
//              logger.info("------------------------------------------------------------------------------------------------------------------------------------------")

            strategy.applyLeverageToPercentageGain(percentageGain)
          })
          .reduce((acc, element) => acc + element)
          .runWith(Sink.last)
          .onComplete {
            case Success(sum) =>
              replyTo ! ResultOfBacktestingStrategyMessage(strategyName, sum / (signals.length - tradesNotEnteredCounter), numberOfTrade)
              logger.info(s"Done backtesting strat:$strategyName with $numberOfTrade trades and profits:${sum / (signals.length - tradesNotEnteredCounter)}")

            case Failure(e) =>
              logger.error("Exception received in BacktesterActor:" + e)
              e.printStackTrace()
              replyTo ! ResultOfBacktestingStrategyMessage(strategyName, 0, 0)
          }


        Behaviors.stopped

  private def formatTimestamp(timestamp: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.ofHours(8))
}
