package ch.xavier
package trading

import Application.{executionContext, system}
import quote.Quote

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.*
import spray.json.DefaultJsonProtocol.*

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
  val http: HttpExt = Http()

  //Get me from secret
  val apiKey: String = "YkdboZ6lsWJZnW7WnB"
  val secret: String = "vMbfySDliyjYnjOalcfCs4wRYzT87YI72pWj"
  val serverUrl = "https://api-testnet.bybit.com/"
  val usdtForEachTrade: Double = 1000.0

  var activePositionsWithInfo: Map[String, (Boolean, Double)] = Map()

  val tradableSymbolsWithBybit: List[String] = List("BTC", "ETH", "EOS", "XRP", "BCH", "LTC", "XTZ", "LINK", "ADA",
    "DOT", "UNI", "XEM", "SUSHI", "AAVE", "DOGE", "MATIC", "ETC", "BNB", "FIL", "SOL", "XLM", "TRX", "VET", "THETA",
    "COMP", "AXS", "LUNA", "SAND", "MANA", "KSM", "ATOM", "AVAX", "CHZ", "CRV", "ENJ", "GRT", "SHIB1000", "YFI", "BSV",
    "ICP", "FTM", "ALGO", "DYDX", "NEAR", "SRM", "OMG", "IOST", "DASH", "FTT", "BIT", "GALA", "CELR", "HBAR", "ONE",
    "C98", "AGLD", "MKR", "COTI", "ALICE", "EGLD", "REN", "TLM", "RUNE", "ILV", "FLOW", "WOO", "LRC", "ENS", "IOTX",
    "CHR", "BAT", "STORJ", "SNX", "SLP", "ANKR", "LPT", "QTUM", "CRO", "SXP", "YGG", "ZEC", "IMX", "SFP", "AUDIO",
    "ZEN", "SKL", "GTC", "LIT", "CVC", "RNDR", "SC", "RSR", "STX", "MASK", "CTK", "BICO", "REQ", "1INCH", "KLAY",
    "SPELL", "ANT", "DUSK", "AR", "REEF", "XMR", "PEOPLE", "IOTA", "ICX", "CELO", "WAVES", "RVN", "KNC", "KAVA", "ROSE",
    "DENT", "CREAM", "LOOKS", "JASMY", "10000NFT", "HNT", "ZIL", "NEO", "RAY", "CKB", "SUN", "JST", "BAND", "RSS3",
    "OCEAN", "1000BTT", "API3", "PAXG", "ANC", "KDA", "APE", "GMT", "OGN", "BSW", "CTSI", "HOT", "ARPA", "ALPHA",
    "STMX", "DGB", "ZRX", "GLMR", "SCRT", "BAKE", "LINA", "ASTR", "FXS", "MINA", "BNX", "BOBA", "1000XEC", "ACH", "BAL",
    "MTL", "CVX", "DODO", "TOMO", "XCN", "GST", "DAR", "FLM", "GAL")


  override def onMessage(message: Message): Behavior[Message] =
    message match
      case OpenPositionMessage(symbol: String, isLongOrder: Boolean, leverage: Int, priceOfToken: Double) =>
        val quantity: Double = usdtForEachTrade / priceOfToken

        if activePositionsWithInfo.contains(symbol) then
          context.log.error(s"A position is already opened for symbol:$symbol")
        else
          if !tradableSymbolsWithBybit.contains(symbol) then
            context.log.warn(s"Symbol:$symbol not tradable on Bybit")
          else
            context.log.info(s"Opening position for symbol:$symbol with isLong:$isLongOrder, quantity:$quantity and leverage:$leverage")

            val leverageResponse: Future[HttpResponse] = http.singleRequest(
                HttpRequest(method = HttpMethods.POST, uri = createSetLeverageUrl(symbol, leverage)))
            val openPositionReponse: Future[HttpResponse] = http.singleRequest(
              HttpRequest(method = HttpMethods.POST, uri = createOpenPositionUrl(symbol, isLongOrder, quantity)))

            leverageResponse.map {
              case response@HttpResponse(StatusCodes.OK, _, _, _) =>
                response.entity.toStrict(30.seconds)
                  .map(entity => entity.getData().utf8String)
                  .onComplete {
                    case Success(response) =>
                      logger.info(s"Leverage set to $leverage for symbol:$symbol")
                      runOpenPositionCommand(openPositionReponse, symbol, isLongOrder, quantity)
                  }

              case error@_ => logger.error(s"Problem encountered when setting leverage for symbol:$symbol: $error")
            }

      case ClosePositionMessage(symbol: String) =>
        if !activePositionsWithInfo.contains(symbol) then
          context.log.error(s"No position is currently active for symbol:$symbol")
        else
          context.log.info(s"Closing position for symbol:$symbol")

          val closePositionReponse: Future[HttpResponse] = http.singleRequest(
            HttpRequest(method = HttpMethods.POST, uri = createClosePositionUrl(symbol)))
          runClosePositionCommand(closePositionReponse, symbol)
    this


  private def runClosePositionCommand(closePositionReponse: Future[HttpResponse], symbol: String) = {
    closePositionReponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(30.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              println(response)
              logger.info(s"Position closed for symbol:$symbol")
              activePositionsWithInfo = activePositionsWithInfo - symbol

            case error@_ => println(s"Problem encountered when closing order for symbol:$symbol: $error")
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
              activePositionsWithInfo = activePositionsWithInfo + (symbol -> (isLongOrder, quantity))
              logger.info(s"Position created for symbol:$symbol with orderId:$orderId")

            case error@_ => logger.error(s"Problem encountered when placing order for symbol:$symbol: $error with response:$response")
          }
    }
  }

  private def createSetLeverageUrl(symbol: String, leverage: Int): String = {
    val url = serverUrl + "private/linear/position/switch-isolated?"
    val queryParams = s"api_key=$apiKey&buy_leverage=$leverage&is_isolated=True&sell_leverage=$leverage&" +
      s"symbol=${symbol}USDT&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createOpenPositionUrl(symbol: String, isLongOrder: Boolean, quantity: Double): String = {
    val side: String = if isLongOrder then "Buy" else "Sell"
    val url = serverUrl + "private/linear/order/create?"
    val formattedQuantity = String.format("%.2f", quantity)

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=False&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def createClosePositionUrl(symbol: String): String = {
    val side: String = if activePositionsWithInfo(symbol)(0) then "Sell" else "Buy"
    val formattedQuantity = String.format("%.2f", activePositionsWithInfo(symbol)(1))
    val url = serverUrl + "private/linear/order/create?"

    val queryParams = s"api_key=$apiKey&close_on_trigger=False&order_type=Market&qty=$formattedQuantity&" +
      s"reduce_only=True&side=$side&symbol=${symbol}USDT&time_in_force=GoodTillCancel&timestamp=${System.currentTimeMillis()}"

    url + addSignToQueryParams(queryParams)
  }

  private def addSignToQueryParams(queryParams: String): String =
    val sha256_HMAC: Mac = Mac.getInstance("HmacSHA256")
    val secret_key: SecretKeySpec = new SecretKeySpec(secret.getBytes, "HmacSHA256")
    sha256_HMAC.init(secret_key)

    queryParams + "&sign=" + bytesToHex(sha256_HMAC.doFinal(queryParams.getBytes))


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
