package ch.xavier

import akka.actor.typed.ActorRef
import ch.xavier.Quote.Quote



sealed trait Message

//QUOTES
final case class CacheQuotes(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) extends Message
final case class QuotesCached() extends Message

final case class FetchQuotes(symbol: String, fromTimestamp: Long, actorRef: ActorRef[Message]) extends Message
final case class QuotesReady(quotes: List[Quote]) extends Message

//BACKTESTING
final case class BacktestStrategyMessage(strategyName: String) extends Message