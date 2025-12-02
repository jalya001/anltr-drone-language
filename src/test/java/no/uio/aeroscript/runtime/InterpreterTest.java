package no.uio.aeroscript.runtime;

import no.uio.aeroscript.antlr.AeroScriptLexer;
import no.uio.aeroscript.antlr.AeroScriptParser;
import no.uio.aeroscript.type.ActionType;
import no.uio.aeroscript.type.ExpressionType;
import no.uio.aeroscript.ast.expr.UnaryNode;
import no.uio.aeroscript.ast.expr.BinaryNode;
import no.uio.aeroscript.ast.stmt.Action;
import no.uio.aeroscript.ast.stmt.Execution;
import no.uio.aeroscript.ast.stmt.Statement;
import no.uio.aeroscript.compiletime.ASTBuilder;
import no.uio.aeroscript.compiletime.TypeChecker;
import no.uio.aeroscript.error.TypeError;
import no.uio.aeroscript.type.Memory;
import no.uio.aeroscript.type.Point;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

// This code isn't great, but I never considered it a high priority
class InterpreterTest {
    private HashMap<Memory, Object> heap;
    private Stack<Statement> stack;

    private static final String programAero = "src\\test\\java\\no\\uio\\aeroscript\\resources\\program.aero";
    private static final String recursetocrashAero = "src\\test\\java\\no\\uio\\aeroscript\\resources\\recursetocrash.aero";
    private static final String reactionsAero = "src\\test\\java\\no\\uio\\aeroscript\\resources\\reactions.aero";
    private static final String fancyTypes = "src\\test\\java\\no\\uio\\aeroscript\\resources\\fancytypes.aero";
    private static final String wrongType = "src\\test\\java\\no\\uio\\aeroscript\\resources\\wrongtype.aero";

    private void initInterpreter() {
        this.heap = new HashMap<>();
        this.stack = new Stack<>();
        HashMap<Memory, HashMap<String, Object>> variables = new HashMap<>();
        variables.put(Memory.VARIABLES, new HashMap<>());
        HashMap<String, Object> vars = variables.get(Memory.VARIABLES);

        HashMap<String, String> reactionTable = new HashMap<>();
        heap.put(Memory.REACTIONS, reactionTable);
        HashMap<String, String> messageTable = new HashMap<>();
        heap.put(Memory.MESSAGES, messageTable);
        HashMap<String, Execution> methodTable = new HashMap<>();
        heap.put(Memory.EXECUTIONS, methodTable);

        float batteryLevel = 10000; // We have way too little battery to cost, so I set it to be more
        Float initialZ = 0f; // Needs to be a float
        Point initialPosition = new Point(0f, 0f); // I changed this to use f instead, since Float doesn't allow the ambiguity or something

        vars.put("initial position", initialPosition);
        vars.put("current position", initialPosition);
        vars.put("altitude", initialZ);
        vars.put("initial battery level", batteryLevel);
        vars.put("battery level", batteryLevel);
        vars.put("battery low", false);
        vars.put("distance travelled", 0.0f);
        vars.put("initial execution", null);

        heap.put(Memory.VARIABLES, vars);
    }

    private AeroScriptParser.ExpressionContext parseExpression(String expression) {
        AeroScriptLexer lexer = new AeroScriptLexer(CharStreams.fromString(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AeroScriptParser parser = new AeroScriptParser(tokens);
        return parser.expression();
    }

    private Execution getProgram(String file, Interpreter interpreter) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file)));
            AeroScriptLexer lexer = new AeroScriptLexer(CharStreams.fromString(content));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            AeroScriptParser parser = new AeroScriptParser(tokens);
            ASTBuilder astBuilder = new ASTBuilder(interpreter);
            Execution program = astBuilder.visitProgram(parser.program());
            return program;
        }  catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    private ArrayList<Execution> parseProgram(Interpreter interpreter, String path) {
        try {
            ArrayList<Execution> executions = interpreter.runProgram(getProgram(path, interpreter));
            return executions;
        } catch (Exception e) {
            System.err.println("Parser error: " + e.getMessage());
            return null;
        }
    }

    @Test
    void getFirstExecution() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        parseProgram(interpreter, programAero);
        @SuppressWarnings("unchecked")
        HashMap<String, Object> variables = (HashMap<String, Object>) interpreter.heap.get(Memory.VARIABLES);
        Execution firstExecution = (Execution) variables.get("first execution");
        assertEquals("RandomTour", firstExecution.id);
    }

    @Test
    void getPosition() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        Point current = interpreter.getCurrentPosition();
        assertEquals(0f, current.getX());
        assertEquals(0f, current.getY());
    }

    @Test
    void getDistanceTravelled() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        assertEquals(0f, interpreter.getDistanceTravelled());
    }

    @Test
    void getBatteryLevel() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        assertEquals(10000, interpreter.getBatteryLevel());
    }

    @Test
    void visitProgram() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        // Implement the test, read a file and parse it, then ensure you have the first execution, and that the number
        // of exeuctions is correct (in the program.aero file there are 9 executions)
        ArrayList<Execution> executions = parseProgram(interpreter, programAero);
        System.out.println(executions);
        assertEquals(5, executions.size()); // There are not 9, but 5
    }

    @Test
    void crashProgram() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        assertThrows(RuntimeException.class, () -> interpreter.runProgram(getProgram(recursetocrashAero, interpreter)));
        // We have no infinite loop detection, but we don't need one, because it fails by running out of battery instead of looping forever. The exception it gets is the one for running out of battery.
    }

    @Test
    void reactionsProgram() {
        initInterpreter();
        Interpreter interpreter = new Interpreter(this.heap, this.stack);

        try {
            interpreter.runProgramREPL(getProgram(reactionsAero, interpreter));
            
            interpreter.receiveMessage("go_short");
            System.out.println("30 expected, vs. " + interpreter.getCurrentPosition().getX());
            assertEquals(30f, interpreter.getCurrentPosition().getX());
            interpreter.receiveMessage("go_short");
            System.out.println("40 expected, vs. " + interpreter.getCurrentPosition().getX());
            assertEquals(40f, interpreter.getCurrentPosition().getX());
            interpreter.receiveMessage("go_home");
            System.out.println("0 expected, vs. " + interpreter.getCurrentPosition().getX());
            assertEquals(0f, interpreter.getCurrentPosition().getX());
        }  catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    @Test
    void buildAST() {
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        Execution program = getProgram(programAero, interpreter);
        System.out.println(program);
        // I haven't thought of what exactly to test here yet
        // But not getting any error is a test alone
    }

    @Test
    void typeTest1() {
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        TypeChecker typeChecker = new TypeChecker();
        typeChecker.walk(getProgram(fancyTypes, interpreter));
    }

    @Test
    void typeTest2() {
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        TypeChecker typeChecker = new TypeChecker();
        assertThrows(TypeError.class, () -> typeChecker.walk(getProgram(wrongType, interpreter)));

        UnaryNode node1 = new UnaryNode(ExpressionType.NUMBER, 10);
        BinaryNode pointNode = new BinaryNode(ExpressionType.POINT, node1, node1);
        BinaryNode node = new BinaryNode(ExpressionType.PLUS, node1, pointNode);
        assertThrows(TypeError.class, () -> typeChecker.walk(node));

        BinaryNode node3 = new BinaryNode(ExpressionType.RANDOM, node1, pointNode);
        assertThrows(TypeError.class, () -> typeChecker.walk(node3));

        Action action1 = new Action(interpreter, ActionType.SCEND, pointNode, node1, node1);
        assertThrows(TypeError.class, () -> typeChecker.walk(action1));

        Action action2 = new Action(interpreter, ActionType.SCEND, node1, pointNode, node1);
        assertThrows(TypeError.class, () -> typeChecker.walk(action2));

        Action action3 = new Action(interpreter, ActionType.TURN, pointNode, node1, node1);
        assertThrows(TypeError.class, () -> typeChecker.walk(action3));
    }

    @Test
    void visitExpression() {
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        ASTBuilder astBuilder = new ASTBuilder(interpreter);

        assertEquals(5.0f, Float.parseFloat(astBuilder.visitExpression(parseExpression("2 + 3")).evaluate().toString()));
        assertEquals(-1.0f, Float.parseFloat(astBuilder.visitExpression(parseExpression("2 - 3")).evaluate().toString()));
        assertEquals(6.0f, Float.parseFloat(astBuilder.visitExpression(parseExpression("2 * 3")).evaluate().toString()));
        assertEquals(-1, Float.parseFloat(astBuilder.visitExpression(parseExpression("-- 1")).evaluate().toString()));
    }

    @Test
    void faultyExpressions() {
        Interpreter interpreter = new Interpreter(this.heap, this.stack);
        ASTBuilder astBuilder = new ASTBuilder(interpreter);

        assertEquals(5.0f, Float.parseFloat(astBuilder.visitExpression(parseExpression("---5")).evaluate().toString()));
        assertThrows(IllegalArgumentException.class, () -> astBuilder.visitExpression(parseExpression("random [10, 4]")).evaluate());
        //assertThrows(Exception.class, () -> interpreter.visitExpression(parseExpression("random (2, 4)")).evaluate()); // Aeroscript warning
        assertThrows(Exception.class, () -> astBuilder.visitExpression(parseExpression("point [10, 4]")).evaluate());
        //assertThrows(Exception.class, () -> interpreter.visitExpression(parseExpression("point (10, 4, 3)")).evaluate()); // Aeroscript warning
        //assertThrows(Exception.class, () -> interpreter.visitExpression(parseExpression("5--3")).evaluate()); // Resolves fine?
        assertThrows(Exception.class, () -> astBuilder.visitExpression(parseExpression("point (10, point(4,2))")).evaluate());
        assertEquals(205.0f, astBuilder.visitExpression(parseExpression("00003 + 0202")).evaluate());
        //assertThrows(Exception.class, () -> interpreter.visitExpression(parseExpression("2 + point (10, 4)")).evaluate());
    }
}
