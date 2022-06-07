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
  implicit val timeout: Timeout = 300.seconds

  context.log.info("The backtester is starting, now caching or fetching the quotes for each signal")

  val backtestedStrategies: List[String] = List("AdvancedTrailingLossReversalStrat")
//  val backtestedStrategies: List[String] = List("SimpleStrategyWithThreeTargets")
//  val backtestedStrategies: List[String] = List("LeveragedSS3TSL", "LeveragedSimpleStrategyWithThreeTargets")
  //LeveragedSS3TTL2 -> no benefit, check stoploss instead

  Source(signalsRepository.getSignals(startTimestamp = 1641011872))
    .mapAsync(4)(signal => quotesActorRef ? (replyTo => CacheQuotesMessage(signal.symbol, signal.timestamp, replyTo)))
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