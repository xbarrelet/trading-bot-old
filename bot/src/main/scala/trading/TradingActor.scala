package ch.xavier
package trading

import Application.{executionContext, system}
import notifications.NotificationsService
import quote.Quote
import signals.Signal
import trading.interfaces.BybitTradingAPI

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import ch.xavier.results.ResultsRepository
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import java.time.ZonedDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Success

object TradingActor {
  def apply(): Behavior[BotMessage] =
    Behaviors.setup(context => new TradingActor(context))
}

class TradingActor(context: ActorContext[BotMessage]) extends AbstractBehavior[BotMessage](context) {
  private val leverage: Int = DynamicConfig.leverage
  private var activePositions: Map[String, Order] = Map()

  private var apiKey: String = null
  private var apiSecret: String = null
  private var amountInUsdtForEachTrade: Double = 0.0
  private var strategyName: String = ""

  private val bybitApi: BybitTradingAPI.type = BybitTradingAPI

  override def onMessage(message: BotMessage): Behavior[BotMessage] =
    message match
      case SetAPIKeysAndLeverageMessage(apiKeyFromMessage: String, apiSecretFromMessage: String, symbolFromMessage: String, strategyNameFromMessage: String) =>
        apiKey = apiKeyFromMessage
        apiSecret = apiSecretFromMessage
        strategyName = strategyNameFromMessage

        bybitApi.getAvailableAmount(apiKeyFromMessage, apiSecretFromMessage, context.self)
        bybitApi.setLeverage(leverage, symbolFromMessage, apiKey, apiSecret)

      case OpenPositionMessage(strategyName: String, openLongPosition: Boolean, startClosePrice: Double, symbol: String) =>
        val quantity: Double = leverage * amountInUsdtForEachTrade / startClosePrice
        val order: Order = Order(symbol, openLongPosition, quantity, startClosePrice, strategyName)

        bybitApi.openPosition(order, apiKey, apiSecret)
        activePositions = activePositions + (strategyName -> order)

      case ClosePositionMessage(strategyName: String, exitPrice: Double) =>
        if !activePositions.contains(strategyName) then
          context.log.error(s"No position is currently active for strategy:$strategyName and apiKey:$apiKey")
        else
          val orderToClose: Order = activePositions(strategyName)

          bybitApi.closePosition(orderToClose, apiKey, apiSecret, exitPrice, context.self)
          activePositions = activePositions - strategyName

      case UpdateAvailableAmount(newAvailableAmount: Double) =>
        context.log.info(s"Available amount for strat:$strategyName, old:$amountInUsdtForEachTrade, new:$newAvailableAmount")
        amountInUsdtForEachTrade = newAvailableAmount

    this
}
