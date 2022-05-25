package ch.xavier
package trading

import Application.{executionContext, system}
import quote.Quote

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import ch.xavier.notifications.NotificationsService
import ch.xavier.signals.Signal
import ch.xavier.trading.interfaces.BybitAPI
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
  def apply(): Behavior[Message] =
    Behaviors.setup(context => new TradingActor(context))
}

class TradingActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context) {
  val logger: Logger = LoggerFactory.getLogger("TradingActor")
  val notificationsService: NotificationsService.type = NotificationsService

  val amountInUsdtForEachTrade: Double = ConfigFactory.load().getDouble("trade.amount-per-trade")
  var activePositions: Map[String, Order] = Map()

  val bybitApi: BybitAPI.type = BybitAPI

  val tradableSymbolsWithBybit: List[String] = List("BTC", "ETH", "EOS", "XRP", "BCH", "LTC", "XTZ", "LINK", "ADA",
    "DOT", "UNI", "XEM", "SUSHI", "AAVE", "DOGE", "MATIC", "ETC", "BNB", "FIL", "SOL", "XLM", "TRX", "VET", "THETA",
    "COMP", "AXS", "SAND", "MANA", "KSM", "ATOM", "AVAX", "CHZ", "CRV", "ENJ", "GRT", "SHIB1000", "YFI", "BSV", "ICP",
    "FTM", "ALGO", "DYDX", "NEAR", "SRM", "OMG", "IOST", "DASH", "FTT", "BIT", "GALA", "CELR", "HBAR", "ONE", "C98",
    "AGLD", "MKR", "COTI", "ALICE", "EGLD", "REN", "TLM", "RUNE", "ILV", "FLOW", "WOO", "LRC", "ENS", "IOTX", "CHR",
    "BAT", "STORJ", "SNX", "SLP", "ANKR", "LPT", "QTUM", "CRO", "SXP", "YGG", "ZEC", "IMX", "SFP", "AUDIO", "ZEN",
    "SKL", "GTC", "LIT", "CVC", "RNDR", "SC", "RSR", "STX", "MASK", "CTK", "BICO", "REQ", "1INCH", "KLAY", "SPELL",
    "ANT", "DUSK", "AR", "REEF", "XMR", "PEOPLE", "IOTA", "ICX", "CELO", "WAVES", "RVN", "KNC", "KAVA", "ROSE", "DENT",
    "CREAM", "LOOKS", "JASMY", "10000NFT", "HNT", "ZIL", "NEO", "RAY", "CKB", "SUN", "JST", "BAND", "RSS3", "OCEAN",
    "1000BTT", "API3", "PAXG", "KDA", "APE", "GMT", "OGN", "BSW", "CTSI", "HOT", "ARPA", "ALPHA", "STMX", "DGB", "ZRX",
    "GLMR", "SCRT", "BAKE", "LINA", "ASTR", "FXS", "MINA", "BNX", "BOBA", "1000XEC", "ACH", "BAL", "MTL", "CVX", "DODO",
    "TOMO", "XCN", "GST", "DAR", "FLM", "GAL", "FITFI", "CTC")



  override def onMessage(message: Message): Behavior[Message] =
    message match
      case OpenPositionMessage(signal: Signal, leverage: Int, startClosePrice: Double) =>
        val quantity: Double = (amountInUsdtForEachTrade / startClosePrice) * leverage
        val symbol: String = signal.symbol

        val order: Order = Order(symbol, signal.isLong, leverage, quantity, startClosePrice)

        if !signal.followOnly && activePositions.contains(symbol) then
          context.log.error(s"A position is already opened for symbol:$symbol")
        else
          if !tradableSymbolsWithBybit.contains(symbol) then
            context.log.warn(s"Symbol:$symbol not tradable on Bybit")
          else
            if !signal.followOnly then
              context.log.info(s"Opening position for symbol:$symbol with isLong:${signal.isLong}, quantity:$quantity and leverage:$leverage")
              bybitApi.openPosition(order)
            else
              context.log.info(s"Following position for symbol:$symbol")

            activePositions = activePositions + (symbol -> order)

      case ClosePositionMessage(symbol: String, stopPrice: Double) =>
        if !activePositions.contains(symbol) then
          context.log.error(s"No position is currently active for symbol:$symbol")
        else
          if !tradableSymbolsWithBybit.contains(symbol) then
            context.log.warn(s"Symbol:$symbol not tradable on Bybit")
          else
            context.log.info(s"Closing position for symbol:$symbol on Bybit")
            val orderToClose: Order = activePositions(symbol)

            bybitApi.closePosition(activePositions(symbol))
            activePositions = activePositions - symbol

            var profits: Double = 0.0
            if orderToClose.isLongOrder then
              profits = stopPrice - orderToClose.startClosePrice
            else
              profits = orderToClose.startClosePrice - stopPrice
            val percentageGain: Double = profits * 100 / orderToClose.startClosePrice

            notificationsService.pushMessage(s"Position closed for order:${activePositions(symbol).toString}. " +
              s"Percentage gain:${percentageGain * 10}, start price:${activePositions(symbol).startClosePrice}, close price:$stopPrice",
              s"Position for $symbol closed")

    this
}
