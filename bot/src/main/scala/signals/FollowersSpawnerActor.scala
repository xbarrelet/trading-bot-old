//package ch.xavier
//package signals
//
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
//import ch.xavier.quote.QuotesFetcherActor
//import ch.xavier.trading.TradingActor
//
//
//
//object FollowersSpawnerActor {
//  def apply(): Behavior[BotMessage] =
//    Behaviors.setup(context => new FollowersSpawnerActor(context))
//}
//
//class FollowersSpawnerActor(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
//  val quotesFetcherRef: ActorRef[BotMessage] = context.spawn(QuotesFetcherActor(), "quotes-fetcher-actor")
//  val tradingActorRef: ActorRef[BotMessage] = context.spawn(TradingActor(), "trading-actor")
//
//  override def onMessage(message: BotMessage): Behavior[BotMessage] =
//    message match
//      case ProcessSignalMessage(signal: Signal) =>
//        val followerActorRef: ActorRef[BotMessage] = context.spawn(SignalFollowerActor(),
//          s"signal-follower-actor-${signal.symbol}-${signal.entryPrice.toString}")
//
//        followerActorRef ! FollowSignalMessage(signal, quotesFetcherRef, tradingActorRef)
//
//        this
//}