package ch.xavier
package trading.interfaces

import trading.{Order, TradingApi}

import org.slf4j.{Logger, LoggerFactory}

object BinanceAPI extends TradingApi {
  val logger: Logger = LoggerFactory.getLogger("BinanceAPI")

  def openPosition(order: Order): Unit =
    logger.info(s"Opening position on Binance for order:$order")

  def closePosition(order: Order): Unit =
    logger.info(s"Closing position on Binance for order:$order")
}
