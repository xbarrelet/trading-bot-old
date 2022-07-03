package ch.xavier
package results

final case class Result(
                         symbol: String,
                         startPrice: Double,
                         startTimestamp: Long,
                         strategyName: String,
                         exitPrice: Double,
                         exitTimestamp: Long
                       )
