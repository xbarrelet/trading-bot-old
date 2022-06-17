package ch.xavier
package signals

import quote.Quote
import signals.Signal
import strategy.{AdvancedStrategy, StrategiesFactory}

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Duration, Instant}


object SignalFollowerActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new SignalFollowerActor(context))
}

class SignalFollowerActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val logger: Logger = LoggerFactory.getLogger("SignalFollowerActor")
  val strategiesFactory: StrategiesFactory.type = StrategiesFactory

  var quotesFetcherRef: ActorRef[Message] = null
  var tradingActorRef: ActorRef[Message] = null
  var strategy: AdvancedStrategy = null
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
        if quote.symbol != "EMPTY" then
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
                context.log.info("")
                context.log.info(s"Strat tells us we should enter for symbol:${signal.symbol} at price:${quote.close}")
                tradingActorRef ! OpenPositionMessage(signal, strategy.leverage, quote.close)
                hasOpenedPosition = true

            else
              if strategy.shouldExitCurrentTrade then
                context.log.info("")
                context.log.info(s"Strat tells us we should exit the current position for symbol:${signal.symbol} at price:${quote.close}")
                tradingActorRef ! ClosePositionMessage(signal.symbol, quote.close)

              if strategy.shouldBuyLong then
                context.log.info("")
                context.log.info(s"Strat tells us we should buy a long position for symbol:${signal.symbol} at price:${quote.close}")
                tradingActorRef ! OpenPositionMessage(signal, strategy.leverage, quote.close)

              if strategy.shouldExit then
                context.log.info("")
                context.log.info(s"Signal tells us we should exit for symbol:${signal.symbol} at price:${quote.close}, stopping follower actor")
                tradingActorRef ! ClosePositionMessage(signal.symbol, quote.close)
                Behaviors.stopped

        this
}
