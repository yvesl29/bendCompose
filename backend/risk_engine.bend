import Base

# Amounts are exact integer cents. 10 000 EUR = 1 000 000 cents.
# Pure business logic; the UI never calculates the score.
def unverified_score(over_threshold: Bool) -> U32:
  match over_threshold:
    case True{}:
      95
    case False{}:
      10

def score(over_threshold: Bool, verified: Bool) -> U32:
  match verified:
    case True{}:
      0
    case False{}:
      unverified_score(over_threshold)

def calculate_risk(amount_cents: U32, verified: Bool) -> U32:
  score(U32.is_gt(amount_cents, 1000000), verified)
