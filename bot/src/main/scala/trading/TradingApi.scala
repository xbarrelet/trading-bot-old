package ch.xavier
package trading

import Application.system

import akka.http.scaladsl.{Http, HttpExt}
import com.typesafe.config.{Config, ConfigFactory}

trait TradingApi {
  val config: Config = ConfigFactory.load()
  val http: HttpExt = Http()
  
  def openPosition(order: Order): Unit
  def closePosition(order: Order): Unit
}
