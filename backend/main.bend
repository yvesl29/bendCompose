import Base
import ./risk_engine.bend as Risk

# Protocol: risk-engine -- <amount in cents> <0|1>
# Success: one line BEND_RISK_V1:<score>. Errors: nonzero exit code.
def emit(amount: U32, verified: Bool) -> IO(Unit):
  IO.print("BEND_RISK_V1:" ++ U32.show(Risk.calculate_risk(amount, verified)))

def with_flag(amount: U32, flag: Maybe<&2, U32>) -> IO(Unit):
  match flag:
    case None{}:
      IO.die(Unit, 2, "Invalid verified flag")
    case Some{value}:
      match value:
        case 0:
          emit(amount, False{})
        case 1:
          emit(amount, True{})
        case _:
          IO.die(Unit, 2, "Expected verified flag 0 or 1")

def with_amount(amount: Maybe<&2, U32>, flag: String) -> IO(Unit):
  match amount:
    case None{}:
      IO.die(Unit, 2, "Invalid amount in cents")
    case Some{value}:
      with_flag(value, U32.read(flag))

def parse(args: List<String>) -> IO(Unit):
  match args:
    case Con{amount, Con{flag, Nil{}}}:
      with_amount(U32.read(amount), flag)
    case _:
      IO.die(Unit, 2, "Usage: risk-engine -- <cents> <0|1>")

def main() -> IO(Unit):
  do IO<Unit>:
    args : List<String> <- IO.args()
    parse(args)
