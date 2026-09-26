import Base
import ./risk_engine.bend as Risk

# These claims cover the pure business rules, not the JVM, OS or C runtime.
law verified_is_zero:
  for amount: U32
  {Risk.calculate_risk(amount, True{}) == 0 : U32}

law high_is_95:
  {Risk.score(True{}, False{}) == 95 : U32}

law low_is_10:
  {Risk.score(False{}, False{}) == 10 : U32}

law bounded:
  for +over: Bool
  for +verified: Bool
  {U32.is_le(Risk.score(over, verified), 100) == True{} : Bool}

law threshold_is_inclusive:
  {Risk.calculate_risk(1000000, False{}) == 10 : U32}

law first_cent_above:
  {Risk.calculate_risk(1000001, False{}) == 95 : U32}
