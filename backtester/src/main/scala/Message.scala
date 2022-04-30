package ch.xavier

import akka.actor.typed.ActorRef
import ch.xavier.Quote.Quote
import ch.xavier.signals.Signal



sealed trait Message

//QUOTES
final case class CacheQuotesMessage(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) extends Message
final case class QuotesCachedMessage() extends Message
final case class ShutdownMessage() extends Message

final case class FetchQuotesMessage(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) extends Message
final case class QuotesReadyMessage(quotes: List[Quote]) extends Message

//BACKTESTING
final case class StartBacktestingMessage(strategyName: String) extends Message

final case class BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) extends Message
final case class ResultOfBacktestingStrategyMessage(strategyName: String, averageProfitsInPercent: Double) extends Message

final case class BacktestStrategyWithSignalMessage(strategyName: String, signal: Signal, replyTo: ActorRef[Message]) extends Message
final case class ResultOfBacktestingStrategyWithSignalMessage(profitsInPercent: Double) extends Message


