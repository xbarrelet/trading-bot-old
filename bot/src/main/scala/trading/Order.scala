package ch.xavier
package trading

final case class Order(symbol: String, isLongOrder: Boolean, quantity: Double, startClosePrice: Double, strategyName: String)
