package ch.xavier
package strategy

import quote.{BybitWSQuotesClient, Quote}
import strategy.{AdvancedStrategy, StrategiesFactory}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import ch.xavier.BotMessage
import ch.xavier.trading.TradingActor
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Duration, Instant}


object TRStratFollowerActor {
  def apply(): Behavior[BotMessage] =
    Behaviors.setup(context => new TRStratFollowerActor(context))
}

class TRStratFollowerActor(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
  private val quotesClient: BybitWSQuotesClient.type = BybitWSQuotesClient

  private var tradingActorRef: ActorRef[BotMessage] = null
  private var strategy: AdvancedStrategy = null

  private val config: Config = ConfigFactory.load()

  private var apiKey: String = null
  private var apiSecret: String = null

  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    message match
      case FollowStrategyMessage(strategyFromMessage: AdvancedStrategy, symbol: String, subAccountId: Int) =>
        context.log.info(s"Starting to follow strategy:${strategyFromMessage.getName} using subAccountId:$subAccountId")

        strategy = strategyFromMessage
        apiKey = config.getString(f"bybit.api-key$subAccountId")
        apiSecret = config.getString(f"bybit.api-secret$subAccountId")
        tradingActorRef = context.spawn(TradingActor(), f"trading-actor-$subAccountId")
        tradingActorRef ! SetAPIKeysMessage(apiKey, apiSecret)

        quotesClient.followSymbol(symbol, context.self)

      case QuoteFetchedMessage(quote: Quote) =>
        strategy.addQuote(quote)

        if strategy.shouldExitCurrentTrade then
          context.log.info("")
          context.log.info(s"Strategy ${strategy.getName} is closing its current position at price:${quote.close} at time:${quote.start_timestamp}")

//          tradingActorRef ! ClosePositionMessage(strategy.getName)

        if strategy.shouldBuyLong then
          context.log.info("")
//          context.log.info(s"Strategy ${strategy.getName} is opening a long position at price:${quote.close} at time:${quote.start_timestamp}")

          tradingActorRef ! OpenPositionMessage(strategy.getName, true, quote.close)

        if strategy.shouldBuyShort then
          context.log.info("")
//          context.log.info(s"Strategy ${strategy.getName} is opening a short position at price:${quote.close} at time:${quote.start_timestamp}")

          tradingActorRef ! OpenPositionMessage(strategy.getName, false, quote.close)

      this
}
