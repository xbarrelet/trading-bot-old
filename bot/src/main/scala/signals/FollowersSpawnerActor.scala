package ch.xavier
package signals

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ch.xavier.quote.QuotesFetcherActor
import ch.xavier.trading.TradingActor



object FollowersSpawnerActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new FollowersSpawnerActor(context))
}

class FollowersSpawnerActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val quotesFetcherRef: ActorRef[Message] = context.spawn(QuotesFetcherActor(), "quotes-fetcher-actor")
  val tradingActorRef: ActorRef[Message] = context.spawn(TradingActor(), "trading-actor")

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case ProcessSignalMessage(signal: Signal) =>
        val followerActorRef: ActorRef[Message] = context.spawn(SignalFollowerActor(), 
          s"signal-follower-actor-${signal.symbol}-${signal.entryPrice.toString}")
        
        followerActorRef ! FollowSignalMessage(signal, quotesFetcherRef, tradingActorRef)

        this
}