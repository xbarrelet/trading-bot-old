package ch.xavier
package signals

import akka.http.scaladsl.model.DateTime
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.time.LocalDateTime

final case class Signal(
                         entryPrice: Double,
                         firstTargetPrice: Double,
                         secondTargetPrice: Double,
                         thirdTargetPrice: Double,
                         isLong: Boolean,
                         stopLoss: Double,
                         symbol: String,
                         followOnly: Boolean
                       )
