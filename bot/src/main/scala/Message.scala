package ch.xavier

import quote.Quote
import signals.Signal

import akka.actor.typed.ActorRef


sealed trait Message

// SIGNAL
final case class ProcessSignalMessage(signal: Signal) extends Message
final case class FollowSignalMessage(signal: Signal, quotesFetcherRef: ActorRef[Message], tradingActorRef: ActorRef[Message]) extends Message


//QUOTES
final case class FetchLastQuoteMessage(symbol: String, replyTo: ActorRef[Message]) extends Message
final case class QuoteFetchedMessage(quote: Quote) extends Message


//TRADING
final case class OpenPositionMessage(signal: Signal, leverage: Int, quoteClosePrice: Double) extends Message
final case class ClosePositionMessage(symbol: String, stopPrice: Double) extends Message