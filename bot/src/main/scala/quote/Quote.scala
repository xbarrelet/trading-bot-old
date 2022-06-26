package ch.xavier
package quote

final case class Quote(
                        close: Double,
                        high: Double,
                        low: Double,
                        open: Double,
                        start_timestamp: Long,
                        symbol: String,
                        isFinalQuote: Boolean
                      )
