package ch.xavier

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ch.xavier.signals.SignalsRepository

object BacktesterActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new BacktesterActor(context))
}

class BacktesterActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val signalsRepository: SignalsRepository.type = SignalsRepository

  override def onMessage(message: Message): Behavior[Message] =
    message match
      case BacktestStrategyMessage(strategyName: String) =>
        context.log.info(s"Now starting to backtest the strategy:$strategyName")
        this
}
