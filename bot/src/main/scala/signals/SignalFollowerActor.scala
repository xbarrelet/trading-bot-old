package ch.xavier
package signals

import quote.Quote
import signals.Signal
import strategy.{StrategiesFactory, Strategy}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import java.time.{Duration, Instant}


object SignalFollowerActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new SignalFollowerActor(context))
}

class SignalFollowerActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val strategiesFactory: StrategiesFactory.type = StrategiesFactory

  var quotesFetcherRef: ActorRef[Message] = null
  var tradingActorRef: ActorRef[Message] = null
  var strategy: Strategy = null
  var signal: Signal = null

  var hasStartedFollowingPrice = false
  var hasOpenedPosition = false


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case FollowSignalMessage(receivedSignal: Signal, fetcherRef: ActorRef[Message], tradingRef: ActorRef[Message]) =>
        context.log.info(s"Starting to follow signal:$receivedSignal")

        strategy = strategiesFactory.getCurrentStrategy(receivedSignal)
        quotesFetcherRef = fetcherRef
        tradingActorRef = tradingRef
        signal = receivedSignal

        quotesFetcherRef ! FetchLastQuoteMessage(receivedSignal.symbol, context.self)
        this

      case QuoteFetchedMessage(quote: Quote) =>
        strategy.addQuote(quote)

        if !hasStartedFollowingPrice then
          val secondsUntilNextQuote: Long = 60L - (Instant.now.getEpochSecond - quote.start_timestamp)
          context.log.info(s"Schedule starts in $secondsUntilNextQuote seconds")
          hasStartedFollowingPrice = true

          context.system.scheduler.scheduleWithFixedDelay(Duration.ofSeconds(secondsUntilNextQuote), Duration.ofMinutes(1),
            () => {
              quotesFetcherRef ! FetchLastQuoteMessage(signal.symbol, context.self)
            }, context.system.executionContext)

        else
          if !hasOpenedPosition then
            if strategy.shouldEnter then
              context.log.info(s"Signal tells us we should open position for symbol:$signal.symbol")
              tradingActorRef ! OpenPositionMessage(signal.symbol, signal.isLong, strategy.leverage, quote.close)
              hasOpenedPosition = true
              //TODO: Add expiration date

          else
            if strategy.shouldExit then
              context.log.info(s"Signal tells us we should close position for symbol:$signal.symbol")
              tradingActorRef ! ClosePositionMessage(signal.symbol)
              Behaviors.stopped

        this
}
