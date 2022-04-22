package ch.xavier
package strategy

import Quote.QuotesRepository

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ch.xavier.signals.SignalsRepository




object StrategyBacktesterActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new StrategyBacktesterActor(context))
}

class StrategyBacktesterActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val quotesRepository: QuotesRepository.type = QuotesRepository
  val signalsRepository: SignalsRepository.type = SignalsRepository


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String, replyTo: ActorRef[Message]) =>
        context.log.info("-----------------------------------------------------------------------------------------")
        context.log.info(s"Now we backtest the strategy:$strategyName for real")

        this
}
