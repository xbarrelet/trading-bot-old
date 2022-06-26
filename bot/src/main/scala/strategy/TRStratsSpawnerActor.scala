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
  val logger: Logger = LoggerFactory.getLogger("TRStratsSpawnerActor")

  val quotesFetcherActor: ActorRef[BotMessage] = context.spawn(QuotesFetcherActor(), "quotes-fetcher-actor")

  val strategies: List[AdvancedStrategy] = List(
    // 1 YEAR BACKTESTED
    CrossEMATRStrategy(50, 50, 235),
    CrossEMATRStrategy(50, 1, 222),

    // 1 MONTH BACKTESTED
    CrossEMATRStrategy(50, 49, 201),
    CrossEMATRStrategy(50, 2, 181),
  )

  quotesFetcherActor ! FetchQuotesMessage("BTC", 240, context.self)


  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    message match
      case QuotesFetchedMessage(quotes: List[Quote]) =>
        for (quote: Quote <- quotes)
          for (strategy: AdvancedStrategy <- strategies)
            strategy.addQuote(quote)

        var counter = 1
        for (strategy: AdvancedStrategy <- strategies)
          val followerActor: ActorRef[BotMessage] = context.spawn(TRStratFollowerActor(), f"follower-actor-${strategy.getName}")
          strategy.reset
          followerActor ! FollowStrategyMessage(strategy, "BTC", counter)
          context.log.info(f"Assigning subAccountId:$counter to strategy:${strategy.getName}")
          counter = counter + 1

      this
}
