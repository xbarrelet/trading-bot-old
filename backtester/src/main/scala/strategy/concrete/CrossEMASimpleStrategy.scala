package ch.xavier
package strategy.concrete

import quote.Quote
import strategy.SimpleStrategy

import org.ta4j.core.indicators.EMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class CrossEMASimpleStrategy(val shortEma: Int, val longEma: Int) extends SimpleStrategy {
  //TODO: Implement when shorting
  private val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  private val shortEMAIndicator = new EMAIndicator(closePriceIndicator, shortEma)
  private val longEMAIndicator = new EMAIndicator(closePriceIndicator, longEma)

  //ENTRY RULE
  private val entryRule: Rule = new OverIndicatorRule(shortEMAIndicator, longEMAIndicator)

  //EXIT RULES
  private val exitRule: Rule = new UnderIndicatorRule(shortEMAIndicator, longEMAIndicator)

  def shouldEnter: Boolean = entryRule.isSatisfied(series.getEndIndex)

  def shouldExit: Boolean = exitRule.isSatisfied(series.getEndIndex)

  override def getName: String = s"CrossEMAStrat_shortEma_${shortEma}_longEma_${longEma}"
}
