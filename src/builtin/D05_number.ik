Number zero? = method(
  "Returns true if this number is zero.",
  @ == 0
)

Number negation = method(
  "Returns the negation of this number",

  0 - @)

Number abs = method(
  "Returns the absolute value of this number",
  if(self < 0, negation, self)
)

Number          do(=== = generateMatchMethod(==))
Number Real     do(=== = generateMatchMethod(==))
Number Rational do(=== = generateMatchMethod(==))
Number Decimal  do(=== = generateMatchMethod(==))

Number Integer odd? = method(
  "Returns true if this number is odd, false otherwise",
  (@ % 2) != 0
)

Number Integer even? = method(
  "Returns true if this number is even, false otherwise",
  (@ % 2) == 0
)

Number Infinity mimic = method(
  error!(Condition Error CantMimicOddball mimic)
)
