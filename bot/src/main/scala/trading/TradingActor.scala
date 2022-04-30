package ch.xavier
package trading

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}


object TradingActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new TradingActor(context))
}

class TradingActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case OpenPositionMessage(symbol: String, leverage: Int) =>
        context.log.info(s"Opening position for symbol:$symbol with leverage:$leverage")

      case ClosePositionMessage(symbol: String) =>
        context.log.info(s"Closing position for symbol:$symbol")
        
      this
}
