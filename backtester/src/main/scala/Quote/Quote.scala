package ch.xavier
package Quote

final case class Quote(
                        close: Double,
                        high: Double,
                        low: Double,
                        open: Double,
                        start_timestamp: Long,
                        symbol: String
                      )
