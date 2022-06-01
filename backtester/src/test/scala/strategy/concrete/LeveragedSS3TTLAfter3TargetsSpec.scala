package strategy.concrete

import ch.xavier.quote.Quote
import ch.xavier.signals.Signal
import ch.xavier.strategy.Strategy
import ch.xavier.strategy.concrete.LeveragedSS3TTL
import org.scalatest.flatspec.AnyFlatSpec
import strategy.StrategySpec

class LeveragedSS3TTLAfter3TargetsSpec extends StrategySpec {


  val testLongSignal: Signal = Signal(
    100, // entry
    150, 200, 250, //3 targets
    true, //Long
    50, // SL
    "TEST_SYMBOL", 1640995200)

  val testShortSignal: Signal = Signal(
    200, // entry
    150, 100, 50, //3 targets
    false, //Long
    250, // SL
    "TEST_SYMBOL", 1640995200)


  behavior of "LeveragedSimpleStrategyWithThreeTargetsAndTrailingLossAfterThirdTarget"


  it should "LONG: set the stopLoss at the first target price if it goes above it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testLongSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val pastFirstTargetQuote = createQuoteWithPrice(151)
    val goesUnderFirstTargetQuote = createQuoteWithPrice(149)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(goesUnderFirstTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: set the stopLoss at the first target price if it goes below it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testShortSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val pastFirstTargetQuote = createQuoteWithPrice(149)
    val goesAboveFirstTargetQuote = createQuoteWithPrice(151)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(goesAboveFirstTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "LONG: set the stopLoss at the second target price if it goes above it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testLongSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val pastFirstTargetQuote = createQuoteWithPrice(151)
    val pastSecondTargetQuote = createQuoteWithPrice(201)
    val goesUnderSecondTargetQuote = createQuoteWithPrice(199)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(goesUnderSecondTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: set the stopLoss at the second target price if it goes below it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testShortSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val pastFirstTargetQuote = createQuoteWithPrice(149)
    val pastSecondTargetQuote = createQuoteWithPrice(99)
    val goesAboveSecondTargetQuote = createQuoteWithPrice(101)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(goesAboveSecondTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "LONG: set the stopLoss at the third target price if it goes above it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testLongSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val pastFirstTargetQuote = createQuoteWithPrice(151)
    val pastSecondTargetQuote = createQuoteWithPrice(201)
    val pastThirdTargetQuote = createQuoteWithPrice(251)
    val goesUnderThirdTargetQuote = createQuoteWithPrice(249)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(pastThirdTargetQuote)
    testedStrategy.addQuote(goesUnderThirdTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: set the stopLoss at the third target price if it goes below it" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testShortSignal, 1, 10)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val pastFirstTargetQuote = createQuoteWithPrice(149)
    val pastSecondTargetQuote = createQuoteWithPrice(99)
    val pastThirdTargetQuote = createQuoteWithPrice(49)
    val goesAboveThirdTargetQuote = createQuoteWithPrice(51)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(pastThirdTargetQuote)
    testedStrategy.addQuote(goesAboveThirdTargetQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "LONG: The trailing loss works after the third target is reached" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testLongSignal, 1, 100)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val pastFirstTargetQuote = createQuoteWithPrice(151)
    val pastSecondTargetQuote = createQuoteWithPrice(201)
    val pastThirdTargetQuote = createQuoteWithPrice(251)
    val newPeakPriceQuote = createQuoteWithPrice(300)
    val stayAboveTrailingLoss = createQuoteWithPrice(270)
    val goesBelowTrailingLoss = createQuoteWithPrice(269)

    testedStrategy.addQuote(entryQuote)
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(pastThirdTargetQuote)
    testedStrategy.addQuote(newPeakPriceQuote)

    // WHEN1
    testedStrategy.addQuote(stayAboveTrailingLoss)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesBelowTrailingLoss)
    // THEN2
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: The trailing loss works after the third target is reached" in {
    // GIVEN
    val testedStrategy: Strategy = LeveragedSS3TTL(testShortSignal, 1, 100)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val pastFirstTargetQuote = createQuoteWithPrice(149)
    val pastSecondTargetQuote = createQuoteWithPrice(99)
    val pastThirdTargetQuote = createQuoteWithPrice(49)
    val newPeakPriceQuote = createQuoteWithPrice(10)
    val stayBelowTrailingLoss = createQuoteWithPrice(11)
    val goesAboveTrailingLoss = createQuoteWithPrice(12)

    testedStrategy.addQuote(entryQuote)
    testedStrategy.addQuote(pastFirstTargetQuote)
    testedStrategy.addQuote(pastSecondTargetQuote)
    testedStrategy.addQuote(pastThirdTargetQuote)
    testedStrategy.addQuote(newPeakPriceQuote)

    // WHEN1
    testedStrategy.addQuote(stayBelowTrailingLoss)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesAboveTrailingLoss)
    // THEN2
    assert(testedStrategy.shouldExit)
  }
}
