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


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) =>
        context.log.info("-----------------------------------------------------------------------------------------")
        println("")
        val signals: List[Signal] = signalsRepository.getSignals

        Source(signals)
          .map(signal => {
            val series: BarSeries = createSeriesForSignal(signal)
            val strategy: Strategy = strategiesFactory.getStrategyFromName(strategyName, series, signal)

            var hasEntered: Boolean = false
            var entryPrice: Double = 0.0
            var exitPrice: Double = 0.0
            println(s"Now testing signal with entryPrice:${signal.entryPrice}, stopLoss:${signal.stopLoss} and firstTargetPrice:${signal.firstTargetPrice}")
            breakable {
              for index <- List.range(0, series.getBarCount) do
                if !hasEntered then
                  if strategy.shouldEnter(index) then
                    entryPrice = series.getBar(index).getClosePrice.doubleValue()
                    hasEntered = true
                    println(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} " +
                      s"has entered with price:$entryPrice at end_timestamp:${series.getBar(index).getEndTime.toLocalDateTime}")
                else
                  if strategy.shouldExit(index) then
                    exitPrice = series.getBar(index).getClosePrice.doubleValue()
                    println(s"Strategy:$strategyName with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} " +
                      s"has exited with price:$exitPrice at end_timestamp:${series.getBar(index).getEndTime.toLocalDateTime}")
                    break
            }

            if exitPrice == 0.0 then
              exitPrice = series.getLastBar.getClosePrice.doubleValue()
              println("The strategy is still active, exiting now the trade")

            var profits = 0.0
            if signal.isLong then
              profits = exitPrice - entryPrice
            else
              profits = entryPrice - exitPrice

            val percentageGain =  profits * 100 / entryPrice
            println(s"Percentage gained with signal symbol:${signal.symbol} and timestamp:${signal.timestamp} : " +
              s"${BigDecimal(percentageGain).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble}")
            println("-----------------------------------------------------------------------------------------")
            percentageGain
          })
          .reduce((acc, element) => acc + element)
          .runWith(Sink.last)
          .onComplete {
            case Success(sum) => replyTo ! ResultOfBacktestingStrategyMessage(sum / signals.length)
            case Failure(e) => println("Exception received in BacktesterActor:" + e)
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
