package ch.xavier
package strategy

import Quote.Quote

trait Strategy {
  def addQuote(quote: Quote): Unit
  def shouldExit: Boolean
}
