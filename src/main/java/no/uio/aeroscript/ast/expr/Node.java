package no.uio.aeroscript.ast.expr;

import no.uio.aeroscript.type.ExpressionType;

public abstract class Node {
    public ExpressionType type;
    public abstract Object evaluate();

    @Override
    public String toString() {
        return "Node";
    }
}
