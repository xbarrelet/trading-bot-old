package ch.xavier
package trading.interfaces

import Application.{executionContext, system}
import trading.interfaces.BybitAPI.*
import trading.{Order, TradingApi}

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol.*
import spray.json.*

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Success

//https://binance-docs.github.io/apidocs/futures/en/#new-order-trade
//https://binance-docs.github.io/apidocs/futures/en/#change-margin-type-trade
object BinanceAPI extends TradingApi {
  val logger: Logger = LoggerFactory.getLogger("BinanceAPI")
  val apiKey = "d17734d41bdbc7679c0f4787e2e9c190dc202c713c9d30a680310b634e6009c0"
  val apiSecret = "82f57ae5b9841510aec872f93c89a89b16591d680550969c977756d7c51b153f"
  val apiUrl = "https://testnet.binancefuture.com/"
  val defaultHeaders: RawHeader = RawHeader("X-MBX-APIKEY", apiKey)


  def openPosition(order: Order): Unit =
    val leverageResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createSetLeverageUrl(order.symbol, order.leverage))
        .withHeaders(defaultHeaders))
    val isolatedMarginResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createIsolatedMarginUrl(order.symbol))
        .withHeaders(defaultHeaders))
    val openPositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createOpenPositionUrl(order.symbol, order.isLongOrder, order.quantity))
        .withHeaders(defaultHeaders))

    leverageResponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(s"Leverage set to ${order.leverage} for symbol:${order.symbol}")
//              isolatedMarginResponse.map {
//                case response@HttpResponse(StatusCodes.OK, _, _, _) =>
//                  response.entity.toStrict(30.seconds)
//                    .map(entity => entity.getData().utf8String)
//                    .onComplete {
//                      case Success(response) =>
//                        logger.info(s"Margin set to isolated for symbol:${order.symbol}")
                        runOpenPositionCommand(openPositionReponse, order.symbol)
//                    }
//                case error@_ => logger.error(s"Problem encountered when setting isolated margin for symbol:$order.symbol: $error")
//              }
          }
      case error@_ => logger.error(s"Problem encountered when setting leverage for symbol:$order.symbol: $error")
    }


  def closePosition(order: Order): Unit =
    val closePositionReponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(method = HttpMethods.POST, uri = createClosePositionUrl(order.symbol, order.isLongOrder, order.quantity))
        .withHeaders(defaultHeaders))
    runClosePositionCommand(closePositionReponse, order.symbol)


  private def runClosePositionCommand(closePositionReponse: Future[HttpResponse], symbol: String) = {
    closePositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(3.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(response)
              logger.info(s"Position closed for symbol:$symbol")

            case error@_ => logger.error(s"Problem encountered when closing order for symbol:$symbol: $error")
          }
    }
  }

  private def runOpenPositionCommand(openPositionReponse: Future[HttpResponse], symbol: String) = {
    openPositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(3.seconds)
          .map(entity => entity.getData().utf8String)
//          .map(body => body.parseJson.convertTo[JsValue].asJsObject)
//          .map(jsonBody => jsonBody.getFields("orderId").head)
//          .map(_.convertTo[String])
          .onComplete {
            case Success(orderId) =>
              logger.info(s"Position created for symbol:$symbol with orderId:$orderId")

            case error@_ => logger.error(s"Problem encountered when placing order for symbol:$symbol: $error with response:$response")
          }
    }
  }

  private def createSetLeverageUrl(symbol: String, leverage: Int): String = {
    val url = apiUrl + "fapi/v1/leverage?"
    val queryParams = s"symbol=${symbol}USDT&leverage=$leverage&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createIsolatedMarginUrl(symbol: String): String = {
    val url = apiUrl + "fapi/v1/positionMargin?"
    val queryParams = s"symbol=${symbol}USDT&marginType=ISOLATED&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createOpenPositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double): String = {
    val side: String = if isLongOrder then "BUY" else "SELL"
    val url = apiUrl + "fapi/v1/order?"
    val formattedQuantity = String.format("%.2f", quantity)

    val queryParams = s"symbol=${symbol}USDT&side=$side&type=MARKET&quantity=1&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createClosePositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double): String = {
    val side: String = if isLongOrder then "SELL" else "BUY"
    val url = apiUrl + "private/linear/order/create?"

    val queryParams = s"symbol=${symbol}USDT&side=$side&type=STOP_MARKET&closePosition=true&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def addSignToQueryParams(queryParams: String): String =
    val sha256Mac: Mac = Mac.getInstance("HmacSHA256")
    val secretKey: SecretKeySpec = new SecretKeySpec(apiSecret.getBytes, "HmacSHA256")
    sha256Mac.init(secretKey)

    queryParams + "&signature=" + bytesToHex(sha256Mac.doFinal(queryParams.getBytes))


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
