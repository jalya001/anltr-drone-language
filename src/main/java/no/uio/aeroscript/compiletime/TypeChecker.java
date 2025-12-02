package no.uio.aeroscript.compiletime;

import java.util.Arrays;
import java.util.stream.Collectors;

import no.uio.aeroscript.ast.expr.BinaryNode;
import no.uio.aeroscript.ast.expr.Node;
import no.uio.aeroscript.ast.expr.UnaryNode;
import no.uio.aeroscript.ast.stmt.Action;
import no.uio.aeroscript.ast.stmt.Execution;
import no.uio.aeroscript.ast.stmt.Statement;
import no.uio.aeroscript.error.TypeError;
import no.uio.aeroscript.type.ExpressionType;

public class TypeChecker {
    private ExpressionType[][] compatibilityTable = {
        // The (n-1)th element is identity
        { ExpressionType.NUMBER, null, null },
        { ExpressionType.POINT, ExpressionType.POINT, null },
        { ExpressionType.RANGE, null, ExpressionType.RANGE } // Range is not used yet
    };

    public ExpressionType walk(Node node) {
        System.out.println("Checking type for: " + node);
        return switch (node.type) { // A visitor pattern is probably better though
            case NUMBER, VARIABLE -> ExpressionType.NUMBER;

            case NEG -> {
                yield walk((Node) ((UnaryNode) node).data);
            }

            case PLUS, MINUS, TIMES -> {
                ExpressionType leftType = walk(((BinaryNode) node).left);
                ExpressionType rightType = walk(((BinaryNode) node).right);
        
                ExpressionType result = compatibilityTable[leftType.ordinal()][rightType.ordinal()];
                if (result == null) throw new TypeError("Incompatible arithmetric on: " + leftType + " and " + rightType + ".");
                
                yield result;
            }

            case POINT -> {
                ExpressionType leftType = walk(((BinaryNode) node).left);
                ExpressionType rightType = walk(((BinaryNode) node).right);
                if (leftType == ExpressionType.POINT || rightType == ExpressionType.POINT) {
                    throw new IllegalArgumentException("Nested Points are not yet supported.");
                    // If we want to support them later, typechecking needs to be done by returning encapsulated types. E.g. Point(Number, Point(Number, Number)), which then need to be matched against the "requested type" of an Action
                }
                yield ExpressionType.POINT;
            }

            case RANDOM -> {
                ExpressionType leftType = walk(((BinaryNode) node).left);
                ExpressionType rightType = walk(((BinaryNode) node).right);
                if (leftType != ExpressionType.NUMBER || rightType != ExpressionType.NUMBER) {
                    throw new TypeError("Ranges take only Numbers.");
                }
                yield ExpressionType.NUMBER;
            }

            case RANGE -> throw new IllegalArgumentException("Ranges are not supported standalone yet.");
        };
    }

    private void checkType(Node node, ExpressionType... expectedTypes) {
        ExpressionType actualType = walk(node);
        String expectedStr = Arrays.stream(expectedTypes).map(Enum::name).collect(Collectors.joining(" or "));
        System.out.println("Expected types: " + expectedStr + " vs. Actual type: " + actualType);
        for (ExpressionType expected : expectedTypes) {
            if (actualType == expected) return;
        }
        throw new TypeError("Expression '" + node + "' is not a " + expectedStr + ".");
    }

    public ExpressionType walk(Statement statement) {
        System.out.println("Checking type for: " + statement);
        if (statement instanceof Action) {
            Action action = (Action) statement;
            checkType((Node) action.speed, ExpressionType.NUMBER);
            checkType((Node) action.duration, ExpressionType.NUMBER);
            
            switch (action.action) {
                case MOVE -> checkType((Node) action.data, ExpressionType.NUMBER, ExpressionType.POINT);
                case TURN, SCEND -> checkType((Node) action.data, ExpressionType.NUMBER);
            };
        }
        else if (statement instanceof Execution) {
            Execution execution = (Execution) statement;
            for (Statement statement2 : execution.statements) {
                walk(statement2);
            }
            for (Execution execution2 : execution.executions.values()) {
                walk(execution2);
            }
        }

        return null;
    }
}