package ch.xavier

import Application.{executionContext, system}
import quote.QuotesActor
import signals.{Signal, SignalsRepository}

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
  val signalsRepository: SignalsRepository.type = SignalsRepository
  val backtesterRef: ActorRef[Message] = context.spawn(StrategiesMainActor(), "backtester-actor")
  val quotesActorRef: ActorRef[Message] = context.spawn(QuotesActor(), "quotes-actor")
  implicit val timeout: Timeout = 3600.seconds

  context.log.info("The backtester is starting, now caching or fetching the 1min quotes for each signal")

  //TODO: Kyle: Larger the timeframe the more accurate the strategy will likely become.
  // He recommended orally to use 30min or higher, backtest different of your strats with different periods and
  // see which ones makes the most money
  // You could also try different symbols, like Solana or more volatile to test some strats
  // The impulse strat of the bot is looking for a cross of ema 21 and 50 before going in fyi

  val numberOfMinutesPerQuote: Int = 30
  val backtestedStrategies: List[String] = List("CrossEMATRStrategy")
//  val backtestedStrategies: List[String] = List("CrossEMATRWithFixedTLStrategy", "CrossEMATRWithTLStrategy")
//  val backtestedStrategies: List[String] = List("CCITRStrategy")


  Source(signalsRepository.getSignals())
    .mapAsync(1)(signal => quotesActorRef ? (replyTo => CacheQuotesMessage(signal.symbol, signal.timestamp, replyTo)))
    .runWith(Sink.last)
    .onComplete {
      case Success(done) =>
        quotesActorRef ! ShutdownMessage()
        backtesterRef ! StartBacktestingMessage(backtestedStrategies)

      case Failure(e) => println("Exception received in Application:" + e)
    }

  override def onMessage(message: Message): Behavior[Message] =
    this
}