package no.uio.aeroscript.compiletime;

import java.util.HashMap;
import java.util.function.Supplier;

import org.antlr.v4.runtime.tree.TerminalNode;

import no.uio.aeroscript.antlr.AeroScriptBaseVisitor;
import no.uio.aeroscript.antlr.AeroScriptParser;
import no.uio.aeroscript.antlr.AeroScriptParser.ExecutionContext;
import no.uio.aeroscript.antlr.AeroScriptParser.StatementContext;
import no.uio.aeroscript.ast.expr.Node;
import no.uio.aeroscript.ast.expr.UnaryNode;
import no.uio.aeroscript.ast.expr.BinaryNode;
import no.uio.aeroscript.ast.stmt.Action;
import no.uio.aeroscript.ast.stmt.Execution;
import no.uio.aeroscript.ast.stmt.Statement;
import no.uio.aeroscript.runtime.Interpreter;
import no.uio.aeroscript.type.ActionType;
import no.uio.aeroscript.type.ExpressionType;
import no.uio.aeroscript.type.Point;

public class ASTBuilder extends AeroScriptBaseVisitor<Object> {
    private Interpreter interpreter;

    public ASTBuilder(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Execution visitProgram(AeroScriptParser.ProgramContext ctx) {
        System.out.println("Building AST.");
        System.out.println(ctx.execution());
        // Program only takes Executions for now

        int prefixedExecutions = 0;
        for (ExecutionContext exCtx : ctx.execution()) {
            if (exCtx.prefix != null) ++prefixedExecutions;
        }
        Statement[] statements = new Statement[prefixedExecutions];
        Execution program = new Execution(interpreter, "program", statements, false, null);

        prefixedExecutions = 0;
        for (ExecutionContext exCtx : ctx.execution()) {
            Execution execution = (Execution) visitExecution(exCtx);
            program.executions.put(execution.id, execution);
            if (execution.prefix) statements[prefixedExecutions++] = execution;
            System.out.println(exCtx.getText());
        }
        

        return program;
    }

    @Override
    public Statement visitExecution(AeroScriptParser.ExecutionContext ctx) {
        String id = ctx.ID(0).getText();
        TerminalNode next = ctx.ID(1);
        Statement[] statements = new Statement[ctx.statement().size()];

        Execution execution = new Execution(interpreter, id, statements, ctx.prefix != null ? true : false, next != null ? next.getText() : null);

        int i = 0;
        for (StatementContext stmCtx : ctx.statement()) {
            if (stmCtx.action() != null) {
                statements[i++] = (Statement) visitStatement(stmCtx);
                // Remember not to add Executions if not prefixed. If we implement more Executions having Executions
            } else if (stmCtx.reaction() != null) {
                AeroScriptParser.ReactionContext rctCtx = stmCtx.reaction();
                String event = rctCtx.event().getText();
                String rctExecution = rctCtx.ID().getText();
                if (event.contains("message")) {
                    execution.messages.put(rctCtx.event().ID().getText(), rctExecution);
                }
                else {
                    execution.reactions.put(event, rctExecution);
                }
            } //else if (stmCtx.execution() != null) { ...
            // For now, Executions can't hold more Executions, except the Program Execution

        }

        return execution;
    }

    @Override
    public Statement visitAction(AeroScriptParser.ActionContext ctx) {
        Node speed = ctx.getText().contains("speed")
            ? visitExpression(ctx.expression())
            : new UnaryNode(ExpressionType.NUMBER, 0f);

        Node duration = ctx.getText().contains("seconds")
            ? visitExpression(ctx.expression())
            : new UnaryNode(ExpressionType.NUMBER, 0f);

        if (ctx.acDock() != null) {
            Supplier<Point> positionSupplier = () -> (Point) ((HashMap<String, Object>) interpreter.getVariables()).get("initial position");
            UnaryNode positionNode = new UnaryNode(ExpressionType.VARIABLE, positionSupplier);
            return new Action(interpreter, ActionType.MOVE, positionNode, speed, duration);
        }
        else if (ctx.acMove() != null) {
            Node node = visitExpression(ctx.acMove().expression());
            return new Action(interpreter, ActionType.MOVE, node, speed, duration);    
        }
        else if (ctx.acTurn() != null) {
            Node node = visitExpression(ctx.acTurn().expression());
            if (ctx.acTurn().getText().contains("left")) {
                node = new UnaryNode(ExpressionType.NEG, node);
            }
            return new Action(interpreter, ActionType.TURN, node, speed, duration);
        }
        else if (ctx.acDescend() != null) {
            if (ctx.acDescend().getText().contains("ground")) {
                Supplier<Float> altitudeSupplier = () -> interpreter.getAltitude();
                UnaryNode altitudeNode = new UnaryNode(ExpressionType.VARIABLE, altitudeSupplier);
                return new Action(interpreter, ActionType.SCEND, new UnaryNode(ExpressionType.NEG, new UnaryNode(ExpressionType.VARIABLE, altitudeNode)), speed, duration);
            }
            else {
                Node node = new UnaryNode(ExpressionType.NEG, visitExpression(ctx.acDescend().expression()));
                return new Action(interpreter, ActionType.SCEND, node, speed, duration);
            }
        }
        else if (ctx.acAscend() != null) {
            Node node = visitExpression(ctx.acAscend().expression());
            return new Action(interpreter, ActionType.SCEND, node, speed, duration);
        }
        else throw new IllegalArgumentException("Invalid action.");
    }
    
    @Override
    public Node visitExpression(AeroScriptParser.ExpressionContext ctx) {
        if (ctx.NEG() != null) {
            Node node = (Node) visit(ctx.expression(0));
            return new UnaryNode(ExpressionType.NEG, node);
        }
        else if (ctx.PLUS() != null || ctx.MINUS() != null || ctx.TIMES() != null) {
            Node lnode = (Node) visit(ctx.expression(0));
            Node rnode = (Node) visit(ctx.expression(1));

            ExpressionType operation;
            if (ctx.PLUS() != null) operation = ExpressionType.PLUS;
            else if (ctx.MINUS() != null) operation = ExpressionType.MINUS;
            else operation = ExpressionType.TIMES;
            
            return new BinaryNode(operation, lnode, rnode);
        }
        else if (ctx.RANDOM() != null) {
            Node lnode;
            Node rnode;
            if (ctx.range() == null) {
                lnode = new UnaryNode(ExpressionType.NUMBER, 0F);
                rnode = new UnaryNode(ExpressionType.NUMBER, 1F);
            } else {
                lnode = (Node) visit(ctx.range().expression(0));
                rnode = (Node) visit(ctx.range().expression(1));
            }
            return new BinaryNode(ExpressionType.RANDOM, lnode, rnode);
        }
        else if (ctx.POINT() != null) {
            return new BinaryNode(
                ExpressionType.POINT,
                (Node) visit(ctx.point().expression(0)),
                (Node) visit(ctx.point().expression(1))
            );
        }
        else if (ctx.LPAREN() != null) {
            return (Node) visit(ctx.expression(0));
        }
        else if (ctx.NUMBER() != null) {
            return new UnaryNode(ExpressionType.NUMBER, Float.parseFloat(ctx.NUMBER().getText()));
            // The parseFloat is a safe "interpretation", since NUMBER is, well, always a number
            // And so I will permit it before typechecking
        }
        else throw new IllegalArgumentException("Invalid expression.");
    }
}
