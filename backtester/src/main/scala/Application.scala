package ch.xavier

import Application.{executionContext, system}
import quote.QuotesActor

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Application extends App {
  implicit val system: ActorSystem[Message] = ActorSystem(Main(), "System")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
}


object Main {
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new Main(context))
}

class Main(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  private val backtesterRef: ActorRef[Message] = context.spawn(StrategiesMainActor(), "backtester-actor")
  private val quotesActorRef: ActorRef[Message] = context.spawn(QuotesActor(), "quotes-actor")
  implicit val timeout: Timeout = 3600.seconds

  context.log.info("The backtester is starting")

  //TODO: You could also try different symbols, like Solana or more volatile to test some strats
  // The impulse strat of the bot is looking for a cross of ema 21 and 50 before going in fyi

  private val MINUTES_PER_QUOTE: Int = 30 //TODO: SHOULD BE A LIST. Works with the cached quotes as you have minutes per quote as a secondary index
  private val DAYS_TO_BACKTEST_ON: Int = 365
  private val SYMBOLS: List[String] = List("BTC")

  private val backtestedStrategies: List[String] = List("CrossEMATRStrategy")
//  val backtestedStrategies: List[String] = List("CrossEMATRWithFixedTLStrategy", "CrossEMATRWithTLStrategy")
//  val backtestedStrategies: List[String] = List("CCITRStrategy")


  Source(SYMBOLS)
    .mapAsync(1)(symbol => quotesActorRef ? (replyTo => CacheQuotesMessage(symbol, DAYS_TO_BACKTEST_ON, MINUTES_PER_QUOTE, replyTo)))
    .runWith(Sink.last)
    .onComplete {
      case Success(_) =>
        quotesActorRef ! ShutdownMessage()
        backtesterRef ! StartBacktestingMessage(backtestedStrategies, SYMBOLS, MINUTES_PER_QUOTE)

      case Failure(e) => println("Exception received in Application:" + e)
    }

  override def onMessage(message: Message): Behavior[Message] =
    this
}