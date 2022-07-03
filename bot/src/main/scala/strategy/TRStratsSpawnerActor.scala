package ch.xavier
package strategy

import quote.{Quote, QuotesFetcherActor}
import strategy.concrete.CrossEMATRStrategy
import strategy.{AdvancedStrategy, StrategiesFactory}
import trading.TradingActor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.slf4j.{Logger, LoggerFactory}
import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBarSeriesBuilder}

import java.time.{Duration, Instant}


object TRStratsSpawnerActor {
  def apply(): Behavior[BotMessage] =
    Behaviors.setup(context => new TRStratsSpawnerActor(context))
}


class TRStratsSpawnerActor(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
  private val logger: Logger = LoggerFactory.getLogger("TRStratsSpawnerActor")
  private val quotesFetcherActor: ActorRef[BotMessage] = context.spawn(QuotesFetcherActor(), "quotes-fetcher-actor")

  private val strategies: Map[String, List[AdvancedStrategy]] = Map(
    "APE" -> List(
      CrossEMATRStrategy(50, 27, 246),
      CrossEMATRStrategy(50, 1, 240),
    )
  )

  for (symbol, strats) <- strategies do quotesFetcherActor ! FetchQuotesMessage(symbol, 250, context.self)


  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    message match
      case QuotesFetchedMessage(quotes: List[Quote]) =>
        val symbol = quotes.head.symbol

        for (quote: Quote <- quotes)
          for (strategy: AdvancedStrategy <- strategies(symbol))
            strategy.addQuote(quote)

        var counter = 1
        for (strategy: AdvancedStrategy <- strategies(symbol))
          val followerActor: ActorRef[BotMessage] = context.spawn(TRStratFollowerActor(), f"follower-actor-${strategy.getName}")
          strategy.reset
          followerActor ! FollowStrategyMessage(strategy, symbol, counter)
          context.log.info(f"Assigning subAccountId:$counter to strategy:${strategy.getName}")
          counter = counter + 1

      this
}
