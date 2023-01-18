package ch.xavier

import quote.Quote
import signals.Signal
import strategy.AdvancedStrategy

import akka.actor.typed.ActorRef


sealed trait BotMessage


//SIGNALS
final case class ProcessSignalMessage(signal: Signal) extends BotMessage
final case class FollowSignalMessage(signal: Signal, quotesFetcherRef: ActorRef[BotMessage], tradingActorRef: ActorRef[BotMessage]) extends BotMessage


//QUOTES
final case class FetchQuotesMessage(symbol: String, numberOfQuotes: Int, replyTo: ActorRef[BotMessage]) extends BotMessage
final case class FollowSymbolMessage(symbol: String, strategyFollower: ActorRef[BotMessage]) extends BotMessage

final case class QuoteFetchedMessage(quote: Quote) extends BotMessage
final case class QuotesFetchedMessage(quotes: List[Quote]) extends BotMessage


//TRADING
final case class SetAPIKeysAndLeverageMessage(apiKey: String, apiSecret: String, symbol: String, strategyName: String) extends BotMessage
final case class OpenPositionMessage(strategyName: String, openLongPosition: Boolean, startClosePrice: Double, symbol: String) extends BotMessage
final case class ClosePositionMessage(strategyName: String, exitPrice: Double) extends BotMessage
final case class UpdateAvailableAmount(newAvailableAmount: Double) extends BotMessage


//STRATEGIES
final case class FollowStrategyMessage(strategyFromMessage: AdvancedStrategy, symbol: String, subAccountId: Int) extends BotMessage