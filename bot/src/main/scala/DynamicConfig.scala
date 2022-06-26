package ch.xavier

import com.typesafe.config.{Config, ConfigFactory}

object DynamicConfig {
  private val config: Config = ConfigFactory.load()

  var leverage: Int = config.getInt("trade.leverage")
  var amountPerTrade: Int = config.getInt("trade.amount-per-trade")
}