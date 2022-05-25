package ch.xavier
package trading

final case class Order(symbol: String, isLongOrder: Boolean, leverage: Int, quantity: Double, startClosePrice: Double)
