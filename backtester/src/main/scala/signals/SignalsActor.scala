package ch.xavier
package signals

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object SignalsActor {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new SignalsActor(context))
}

class SignalsActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  override def onMessage(message: Message): Behavior[Message] =
    message match
      case 
        
        
        
    this
}
