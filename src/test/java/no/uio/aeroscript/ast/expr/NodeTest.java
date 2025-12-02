package no.uio.aeroscript.ast.expr;

import no.uio.aeroscript.type.ExpressionType;
import no.uio.aeroscript.type.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {
    Node left = new UnaryNode(ExpressionType.NUMBER, 2.0f);
    Node right = new UnaryNode(ExpressionType.NUMBER, 3.0f);

    @Test
    void evaluateSum() {
        Node node = new BinaryNode(ExpressionType.PLUS, left, right);
        assertEquals(5.0f, node.evaluate());
    }

    @Test
    void evaluateSub() {
        Node node = new BinaryNode(ExpressionType.MINUS, left, right);
        assertEquals(-1.0f, node.evaluate());
    }

    @Test
    void evaluateMul() {
        Node node = new BinaryNode(ExpressionType.TIMES, left, right);
        assertEquals(6.0f, node.evaluate());
    }

    @Test
    void evaluateNeg() {
        Node node = new UnaryNode(ExpressionType.NEG, left);
        assertEquals(2.0f * -1, node.evaluate());
    }

    @Test
    void evaluateRandom() {
        Node node = new BinaryNode(ExpressionType.RANDOM, left, right);
        float value = (Float) node.evaluate();
        assertTrue(value >= 2.0f && value <= 3.0f);
    }

    @Test
    void evaluatePoint() {
        Node node = new BinaryNode(ExpressionType.POINT, left, right);
        Point point = (Point) node.evaluate();
        assertEquals(2.0f, point.getX());
        assertEquals(3.0f, point.getY());
    }

    @Test
    void evaluateFancyArithmetric() {
        Node testLeft = new BinaryNode(ExpressionType.TIMES, left, right);
        Node testRight = new BinaryNode(ExpressionType.MINUS, left, right);
        Node node = new BinaryNode(ExpressionType.PLUS, testLeft, testRight);
        assertEquals(5f, node.evaluate());
    }

    @Test
    void evaluateFancyPoint() {
        Node testLeft = new BinaryNode(ExpressionType.TIMES, left, right);
        Node testRight = new BinaryNode(ExpressionType.RANDOM, left, right);
        Node node = new BinaryNode(ExpressionType.POINT, testLeft, testRight);
        Point point = (Point) node.evaluate();
        assertEquals(6.0f, point.getX());
        assertTrue(point.getY() >= 2.0f && point.getY() <= 3.0f);
    }

    @Test
    void evaluatePointArithmetric() {
        Node node1 = new BinaryNode(ExpressionType.POINT, left, right);

        Node node2 = new BinaryNode(ExpressionType.PLUS, node1, node1);
        Point point2 = (Point) node2.evaluate();
        assertEquals(4.0f, point2.getX());
        assertEquals(6.0f, point2.getY());

        Node node3 = new BinaryNode(ExpressionType.TIMES, node2, node1);
        Point point3 = (Point) node3.evaluate();
        assertEquals(8.0f, point3.getX());
        assertEquals(18.0f, point3.getY());

        Node node4 = new BinaryNode(ExpressionType.MINUS, node3, node1);
        Point point4 = (Point) node4.evaluate();
        assertEquals(6.0f, point4.getX());
        assertEquals(15.0f, point4.getY());

        Node node5 = new BinaryNode(ExpressionType.TIMES, node4, left);
        Point point5 = (Point) node5.evaluate();
        assertEquals(12.0f, point5.getX());
        assertEquals(30.0f, point5.getY());

        Node node6 = new BinaryNode(ExpressionType.PLUS, node5, left);
        Point point6 = (Point) node6.evaluate();
        assertEquals(14.0f, point6.getX());
        assertEquals(32.0f, point6.getY());

        Node node7 = new BinaryNode(ExpressionType.MINUS, node6, left);
        Point point7 = (Point) node7.evaluate();
        assertEquals(12.0f, point7.getX());
        assertEquals(30.0f, point7.getY());

        Node node8 = new UnaryNode(ExpressionType.NEG, node7);
        Point point8 = (Point) node8.evaluate();
        assertEquals(-12.0f, point8.getX());
        assertEquals(-30.0f, point8.getY());
    }

    @Test
    void evaluateFancyNeg() {
        Node testLeft = new BinaryNode(ExpressionType.TIMES, left, right);
        Node node = new UnaryNode(ExpressionType.NEG, testLeft);
        assertEquals(6.0f * -1, node.evaluate());
    }

    @Test
    void evaluateFancyRandom() {
        Node testLeft = new BinaryNode(ExpressionType.RANDOM, left, right);
        Node testLeft2 = new UnaryNode(ExpressionType.NEG, testLeft);
        Node testRight = new BinaryNode(ExpressionType.TIMES, left, right);
        Node node = new BinaryNode(ExpressionType.RANDOM, testLeft2, testRight);
        float value = (Float) node.evaluate();
        assertTrue(value >= -3.0f && value <= 6.0f);
    }
}
