package no.uio.aeroscript.ast.expr;

import java.util.function.Supplier;

import no.uio.aeroscript.type.ExpressionType;
import no.uio.aeroscript.type.Point;
import no.uio.aeroscript.type.Range;

public class UnaryNode extends Node {
    public final Object data;

    public UnaryNode(ExpressionType type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Object evaluate() {
        return switch (type) {
            case NEG -> {
                Object evalData = ((Node) data).evaluate();
                if (evalData instanceof Point) {
                    Point p = (Point) evalData;
                    yield new Point(p.getX() * -1, p.getY() * -1);
                } else if (evalData instanceof Range) {
                    Range r = (Range) evalData;
                    yield new Point(r.getStart() * -1, r.getEnd() * -1);
                } else {
                    yield (Float) evalData * -1;
                }
            }

            case NUMBER -> {
                yield data;
            }

            case VARIABLE -> {
                yield ((Supplier<?>) data).get();
            }
            
            default -> throw new IllegalArgumentException("Invalid operator: " + type);
        };
    }

    @Override
    public String toString() {
        return "UnaryNode " + type + " " + data;
    }
}

