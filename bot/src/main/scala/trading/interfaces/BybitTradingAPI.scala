package ch.xavier
package trading.interfaces

import Application.{executionContext, system}
import trading.{Order, TradingApi}

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import ch.xavier.results.ResultsRepository
import ch.xavier.trading.interfaces.BybitTradingAPI.leverage
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsValue

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{DurationInt, pairIntToDuration}
import scala.util.Success
import spray.json.*
import spray.json.DefaultJsonProtocol.*

import java.time.LocalDateTime

object BybitTradingAPI {
  private val logger: Logger = LoggerFactory.getLogger("BybitAPI")

  private val config: Config = ConfigFactory.load()
  private val http: HttpExt = Http()
  private val apiUrl: String = config.getString("bybit.api-url")
  private val leverage: Int = ConfigFactory.load().getInt("trade.leverage")

  private val resultsRepository: ResultsRepository.type = ResultsRepository


  def getAvailableAmount(apiKey: String, apiSecret: String, replyTo: ActorRef[BotMessage]): Unit =
    val availableAmountResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.GET, uri = createAvailableAmountUrl(apiKey, apiSecret)))
    availableAmountResponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .map(body => body.parseJson.convertTo[JsValue].asJsObject)
          .map(jsonBody => jsonBody.getFields("result").head)
          .map(_.convertTo[JsValue].asJsObject)
          .map(jsonBody => jsonBody.getFields("USDT").head)
          .map(_.convertTo[JsValue].asJsObject)
          .map(jsonBody => jsonBody.getFields("available_balance").head)
          .onComplete {
            case Success(payload) =>
              val availableAmount: Double = payload.toString.replace("\"", "").toDouble
              replyTo.tell(UpdateAvailableAmount(availableAmount))
            case error@_ =>
              logger.error(s"Problem encountered when getting the available amount: $error, returning 1000")
              replyTo.tell(UpdateAvailableAmount(1000.0))
          }
    }

  def setLeverage(leverage: Int, symbol: String, apiKey: String, apiSecret: String): Unit =
    val leverageResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createSetLeverageUrl(symbol, leverage, apiKey, apiSecret)))

    leverageResponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) => logger.info(s"Leverage set to $leverage for symbol:${symbol}")
            case error@_ => logger.error(s"Problem encountered when setting leverage for symbol:${symbol}: $error")
          }
    }

  def openPosition(order: Order, apiKey: String, apiSecret: String): Unit =
    val openPositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createOpenPositionUrl(order.symbol, order.isLongOrder, order.quantity, apiKey, apiSecret)))

    openPositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          //          .map(body => body.parseJson.convertTo[JsValue].asJsObject)
          //          .map(jsonBody => jsonBody.getFields("result").head)
          //          .map(_.convertTo[JsValue].asJsObject)
          //          .map(jsonBody => jsonBody.getFields("order_id").head)
          //          .map(orderId => orderId.toString.replace("\"", ""))
          .onComplete {
            case Success(payload) =>
              logger.info(s"Position created for symbol:${order.symbol} with payload:$payload")
              resultsRepository.insertResultWithStartResult(order.symbol, order.startClosePrice,
                System.currentTimeMillis() / 1000, order.strategyName)

            case error@_ => logger.error(s"Problem encountered when placing order for symbol:${order.symbol}: $error with response:$response")
          }
    }


  def closePosition(order: Order, apiKey: String, apiSecret: String, exitPrice: Double): Unit =
    val closePositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createClosePositionUrl(order.symbol, order.isLongOrder, order.quantity, apiKey, apiSecret)))

    closePositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(s"Position closed for symbol:${order.symbol}")
              resultsRepository.updateResultWithEndValues(order.symbol, order.strategyName, exitPrice, 
                System.currentTimeMillis() / 1000)
            case error@_ => logger.error(s"Problem encountered when closing order for symbol:${order.symbol}: $error")
          }
    }

  private def createAvailableAmountUrl(apiKey: String, apiSecret: String): String = {
    val url = apiUrl + "v2/private/wallet/balance?"
    val queryParams = s"api_key=$apiKey&coin=USDT&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams, apiSecret)
  }

  private def createSetLeverageUrl(symbol: String, leverage: Int, apiKey: String, apiSecret: String): String = {
    val url = apiUrl + "private/linear/position/switch-isolated?"
    val queryParams = s"api_key=$apiKey&buy_leverage=$leverage&is_isolated=True&sell_leverage=$leverage&" +
      s"symbol=${symbol}USDT&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams, apiSecret)
  }

  private def createOpenPositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double, apiKey: String, apiSecret: String): String = {
    val side: String = if isLongOrder then "Buy" else "Sell"
    val url = apiUrl + "private/linear/order/create?"
    val formattedQuantity = String.format("%.4f", quantity)

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=False&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams, apiSecret)
  }

  private def createClosePositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double, apiKey: String, apiSecret: String): String = {
    val side: String = if isLongOrder then "Sell" else "Buy"
    val formattedQuantity = String.format("%.4f", quantity)
    val url = apiUrl + "private/linear/order/create?"

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=True&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams, apiSecret)
  }

  private def addSignToQueryParams(queryParams: String, apiSecret: String): String =
    val sha256Mac: Mac = Mac.getInstance("HmacSHA256")
    val secretKey: SecretKeySpec = new SecretKeySpec(apiSecret.getBytes, "HmacSHA256")
    sha256Mac.init(secretKey)

    queryParams + "&sign=" + bytesToHex(sha256Mac.doFinal(queryParams.getBytes))


  private def bytesToHex(hash: Array[Byte]) = {
    val hexString = new StringBuffer
    for (i <- hash.indices) {
      val hex = Integer.toHexString(0xff & hash(i))
      if (hex.length == 1) hexString.append('0')
      hexString.append(hex)
    }
    hexString.toString
  }
}
