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

  //TODO: Strat when trailing loss detected you sell or buy short and when trailing gain detected you long again until you reach the targets
  // Or sell after x number of negative quotes? or amount-based? Or also check volume?
  // When you reach a target do you go back down until a resistance or level or whatever? How can you detect that?
  // also to consider only second or third target

  val backtestedStrategies: List[String] = List("LeveragedTrailingLossStrategy")
//  val backtestedStrategies: List[String] = List("SimpleStrategyWithThreeTargetsAndTrailingLoss")
//  val backtestedStrategies: List[String] = List("SimpleStrategyWithThreeTargetsAndStoppingLoss")
//  val backtestedStrategies: List[String] = List("LeveragedSS3TSL", "LeveragedSimpleStrategyWithThreeTargets")
  //LeveragedSS3TTL2 -> no benefit, check stoploss instead

  //  val backtestedStrategies: List[String] = List("test")

//  Source(signalsRepository.getSignals(startTimestamp = 1641011872))
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