package no.uio.aeroscript.ast.expr;

import no.uio.aeroscript.type.Point;
import no.uio.aeroscript.type.ExpressionType;
import java.util.Random;

public class BinaryNode extends Node {
    public final Node left;
    public final Node right;
    private static final Random random = new Random();

    public BinaryNode(ExpressionType type, Node left, Node right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }
    
    @Override
    public Object evaluate() {
        Object le = left.evaluate();

        return switch (type) {
            case PLUS -> {
                Object re = right.evaluate();
                if (le instanceof Point && re instanceof Point) {
                    yield new Point(((Point) le).getX() + ((Point) re).getX(), ((Point) le).getY() + ((Point) re).getY());
                } else if (le instanceof Point && re instanceof Float) {
                    yield new Point(((Point) le).getX() + (Float) re, ((Point) le).getY() + (Float) re);
                } else {
                    yield (Float) le + (Float) re;
                }
            }
    
            case MINUS -> {
                Object re = right.evaluate();
                if (le instanceof Point && re instanceof Point) {
                    yield new Point(((Point) le).getX() - ((Point) re).getX(), ((Point) le).getY() - ((Point) re).getY());
                } else if (le instanceof Point && re instanceof Float) {
                    yield new Point(((Point) le).getX() - (Float) re, ((Point) le).getY() - (Float) re);
                } else {
                    yield (Float) le - (Float) re;
                }
            }
    
            case TIMES -> {
                Object re = right.evaluate();
                if (le instanceof Point && re instanceof Point) {
                    yield new Point(
                        ((Point) le).getX() * ((Point) re).getX(),
                        ((Point) le).getY() * ((Point) re).getY()
                    );
                } else if (le instanceof Point || re instanceof Point) {
                    Point p = (le instanceof Point) ? (Point) le : (Point) re;
                    Float f = (le instanceof Float) ? (Float) le : (Float) re;
                    yield new Point(p.getX() * f, p.getY() * f);
                } else {
                    yield (Float) le * (Float) re;
                }
            }
    
            case RANDOM -> {
                Object re = right.evaluate();
                yield random.nextFloat((Float) le, (Float) re);
            }
    
            case POINT -> {
                Object re = right.evaluate();
                yield new Point((Float) le, (Float) re);
            }
            
            default -> throw new IllegalArgumentException("Invalid operator: " + type);
        };
    }

    @Override
    public String toString() {
        return "BinaryNode " + type + " " + left + " " + right;
    }
}