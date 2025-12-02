package no.uio.aeroscript.type;

public enum ExpressionType { // Perhaps this is better split into UnaryType and BinaryType
    NUMBER, // The order on NUMBER, POINT, and RANGE is used in TypeChecker
    POINT,
    RANGE,
    NEG,
    PLUS,
    MINUS,
    TIMES,
    RANDOM,
    VARIABLE
}
