package ch.xavier
package strategy

import Quote.{Quote, QuotesRepository}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.scaladsl.{Sink, Source}
import ch.xavier.signals.{Signal, SignalsRepository}
import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar, BaseBarSeriesBuilder}

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import scala.concurrent.Future
import ch.xavier.Application.{executionContext, system}
import org.slf4j.{Logger, LoggerFactory}

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
        context.log.info("-----------------------------------------------------------------------------------------")
        context.log.info("")
        val signals: List[Signal] = signalsRepository.getSignals
        var tradesNotEnteredCounter = 0

        Source(signals)
          .map(signal => {
            val series: BarSeries = createSeriesForSignal(signal)
            val strategy: Strategy = strategiesFactory.getStrategyFromName(strategyName, series, signal)

            var hasEntered = false
            var entryPrice = 0.0
            var exitPrice = 0.0
            logger.info(s"Now testing signal with symbol:${signal.symbol}, entryPrice:${signal.entryPrice}, " +
              s"stopLoss:${signal.stopLoss} and firstTargetPrice:${signal.firstTargetPrice}")

            breakable {
              for index <- List.range(0, series.getBarCount) do
                if !hasEntered then
                  if strategy.shouldEnter(index) then
                    entryPrice = series.getBar(index).getClosePrice.doubleValue()
                    hasEntered = true
                    logger.info(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} has entered with price:$entryPrice at end_timestamp:${series.getBar(index).getEndTime.toLocalDateTime}")
                else
                  if strategy.shouldExit(index) then
                    exitPrice = series.getBar(index).getClosePrice.doubleValue()
                    logger.info(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} has exited with price:$exitPrice at end_timestamp:${series.getBar(index).getEndTime.toLocalDateTime}")
                    break
            }

            var percentageGain = 0.0
            if entryPrice == 0.0 then
              tradesNotEnteredCounter += 1
              logger.info("The trade was never entered")
            else
              if exitPrice == 0.0 then
                exitPrice = series.getLastBar.getClosePrice.doubleValue()
                logger.info(s"The strategy is still active, exiting now the trade with price:$exitPrice")

              var profits = 0.0
              if signal.isLong then
                profits = exitPrice - entryPrice
              else
                profits = entryPrice - exitPrice

              percentageGain = profits * 100 / entryPrice
              logger.info(s"Percentage gained with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} : " +
                s"${BigDecimal(percentageGain).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}")

            logger.info("------------------------------------------------------------------------------------------------------------------------------------------")
            percentageGain
          })
          .reduce((acc, element) => acc + element)
          .runWith(Sink.last)
          .onComplete {
            case Success(sum) => replyTo ! ResultOfBacktestingStrategyMessage(sum / (signals.length - tradesNotEnteredCounter))
            case Failure(e) => logger.error("Exception received in BacktesterActor:" + e)
          }


        this

  def createSeriesForSignal(signal: Signal): BarSeries =
    val series: BarSeries = BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.valueOf(_)).build
    val quotes: List[Quote] = quotesRepository.getQuotes(signal.symbol, signal.timestamp)

    quotes.foreach(quote => {
      series.addBar(BaseBar.builder()
        .closePrice(DoubleNum.valueOf(quote.close))
        .highPrice(DoubleNum.valueOf(quote.high))
        .openPrice(DoubleNum.valueOf(quote.open))
        .lowPrice(DoubleNum.valueOf(quote.low))
        .timePeriod(Duration.ofMinutes(1))
        .endTime(ZonedDateTime.ofInstant(Instant.ofEpochSecond(quote.start_timestamp + 60), ZoneId.of("UTC")))
        .build())
    })

    series
}
