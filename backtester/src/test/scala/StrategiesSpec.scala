import ch.xavier.signals.Signal
import ch.xavier.strategy.Strategy
import org.scalatest.flatspec.AnyFlatSpec
import ch.xavier.strategy.strats.{LeveragedSimpleStrategyWithThreeTargets, LeveragedTrailingLossStrategy}

class StrategiesSpec extends AnyFlatSpec{

  val testSignal: Signal = Signal(
    100, // entry
    150, 200, 250, //3 targets
    true, //Long
    50, // SL
    "TEST_SYMBOL", 1640995200)


  behavior of "LeveragedSimpleStrategyWithThreeTargets"
  val testedStrategy: Strategy = LeveragedSimpleStrategyWithThreeTargets(testSignal, 1)


  it should "set the stopLoss at the first target price if it goes above it" in {
    

  }

  it should "set the stopLoss at the second target price if it goes above it" in {}

  it should "set the stopLoss at the third target price if it goes above it" in {}
}
