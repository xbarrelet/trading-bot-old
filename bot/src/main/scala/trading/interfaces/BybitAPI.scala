package ch.xavier
package trading.interfaces

import Application.{executionContext, system}
import trading.{Order, TradingApi}

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsValue

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Success
import spray.json.*
import spray.json.DefaultJsonProtocol.*

object BybitAPI extends TradingApi {
  val logger: Logger = LoggerFactory.getLogger("BybitAPI")

  val apiKey: String = config.getString("bybit.api-key")
  val apiSecret: String = config.getString("bybit.api-secret")
  val apiUrl: String = config.getString("bybit.api-url")


  def openPosition(order: Order): Unit =
    val leverageResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createSetLeverageUrl(order.symbol, order.leverage)))
    val openPositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createOpenPositionUrl(order.symbol, order.isLongOrder, order.quantity)))

    leverageResponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(s"Leverage set to $order.leverage for symbol:$order.symbol")
              runOpenPositionCommand(openPositionReponse, order.symbol, order.isLongOrder, order.quantity)
          }

      case error@_ => logger.error(s"Problem encountered when setting leverage for symbol:$order.symbol: $error")
    }


  def closePosition(order: Order): Unit =
    val closePositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createClosePositionUrl(order.symbol, order.isLongOrder, order.quantity)))
    runClosePositionCommand(closePositionReponse, order.symbol)


  private def runClosePositionCommand(closePositionReponse: Future[HttpResponse], symbol: String) = {
    closePositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(s"Position closed for symbol:$symbol")

            case error@_ => logger.error(s"Problem encountered when closing order for symbol:$symbol: $error")
          }
    }
  }

  private def runOpenPositionCommand(openPositionReponse: Future[HttpResponse], symbol: String, isLongOrder: Boolean, quantity: Double) = {
    openPositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .map(body => body.parseJson.convertTo[JsValue].asJsObject)
          .map(jsonBody => jsonBody.getFields("result").head)
          .map(_.convertTo[JsValue].asJsObject)
          .map(jsonBody => jsonBody.getFields("order_id").head)
          .map(orderId => orderId.toString.replace("\"", ""))
          .onComplete {
            case Success(orderId) =>
              logger.info(s"Position created for symbol:$symbol with orderId:$orderId")

            case error@_ => logger.error(s"Problem encountered when placing order for symbol:$symbol: $error with response:$response")
          }
    }
  }

  private def createSetLeverageUrl(symbol: String, leverage: Int): String = {
    val url = apiUrl + "private/linear/position/switch-isolated?"
    val queryParams = s"api_key=$apiKey&buy_leverage=$leverage&is_isolated=True&sell_leverage=$leverage&" +
      s"symbol=${symbol}USDT&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createOpenPositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double): String = {
    val side: String = if isLongOrder then "Buy" else "Sell"
    val url = apiUrl + "private/linear/order/create?"
    val formattedQuantity = String.format("%.2f", quantity)

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=False&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createClosePositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double): String = {
    val side: String = if isLongOrder then "Sell" else "Buy"
    val formattedQuantity = String.format("%.2f", quantity)
    val url = apiUrl + "private/linear/order/create?"

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=True&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def addSignToQueryParams(queryParams: String): String =
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
