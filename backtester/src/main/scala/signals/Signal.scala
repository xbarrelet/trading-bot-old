package ch.xavier
package signals

import akka.http.scaladsl.model.DateTime

final case class Signal(
                         emissionDate: DateTime,
                         entryPrice: Double,
                         stopLoss: Double,
                         symbol: String,
                         targetPrice: Double
                       )
