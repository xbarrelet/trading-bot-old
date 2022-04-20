package ch.xavier

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ch.xavier.Quote.QuotesActor
import ch.xavier.signals.{Signal, SignalsRepository}

import scala.concurrent.ExecutionContextExecutor

object Application extends App {
  implicit val system: ActorSystem[Message] = ActorSystem(Main(), "System")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
}


object Main {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new Main(context))
}


class Main(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val signalsRepository: SignalsRepository.type = SignalsRepository
  val backtesterRef: ActorRef[Message] = context.spawn(BacktesterActor(), "backtester-actor")
  val quotesActorRef: ActorRef[Message] = context.spawn(QuotesActor(), "quotes-actor")

  context.log.info("Starting backtester for the trading bot, now caching the quotes of to backtest each signal")

  val backtestedStrategy: String = "SimpleStrategy"

  val signals: List[Signal] = signalsRepository.getSignals
  signals.foreach(signal => quotesActorRef ! CacheQuotes(signal.symbol, signal.timestamp, context.self))
  var quotesCachedCounter: Int = 0

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case QuotesCached() =>
        quotesCachedCounter += 1

        if quotesCachedCounter == signals.length then
          context.log.info("-----------------------------------------------------------------------------------------")
          context.log.info("Quotes cached for each signal, now starting the backtesting")
          backtesterRef ! BacktestStrategyMessage(backtestedStrategy)

        this
}