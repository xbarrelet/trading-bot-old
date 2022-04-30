package ch.xavier
package signals

import akka.http.scaladsl.model.DateTime
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.time.LocalDateTime

final case class Signal(
                         entryPrice: Double,
                         firstTargetPrice: Double,
                         isLong: Boolean,
                         stopLoss: Double,
                         symbol: String
                       )
