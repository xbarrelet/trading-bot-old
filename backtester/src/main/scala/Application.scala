package ch.xavier

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ch.xavier.Quote.{Quote, QuotesActor}

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
  context.log.info("Starting backtester for the trading bot")

  val testRef: ActorRef[Message] = context.spawn(QuotesActor(), "testActor")
  testRef ! FetchQuotes("BTC", 1648521540, context.self)
  testRef ! FetchQuotes("ETH", 1648521540, context.self)


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case QuotesReady(quotes: List[Quote]) =>
        context.log.info(s"Received ${quotes.length} quotes!")
        this
}