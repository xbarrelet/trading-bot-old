package ch.xavier

import quote.Quote

import akka.actor.typed.ActorRef
import ch.xavier.strategy.SimpleStrategy


sealed trait Message

//QUOTES
final case class CacheQuotesMessage(symbol: String, daysToBacktestOn: Long, quoteMinutes: Int, actorRef: ActorRef[Message]) extends Message
final case class QuotesCachedMessage() extends Message
final case class ShutdownMessage() extends Message

final case class FetchQuotesMessage(symbol: String, fromTimestamp: Long, minutesPerQuote: Long, actorRef: ActorRef[Message]) extends Message
final case class QuotesReadyMessage(quotes: Set[Quote]) extends Message

//BACKTESTING
final case class StartBacktestingMessage(strategyNames: List[String], symbols: List[String], minutesPerQuote: Int) extends Message

final case class BacktestStrategyMessage(strategyName: String, symbol: String, minutesPerQuote: Int, replyTo: ActorRef[Message]) extends Message
final case class ResultOfBacktestingStrategyMessage(strategyName: String, averageProfitsInPercent: Double, numberOfTrade: Int) extends Message



