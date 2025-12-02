package no.uio.aeroscript;

import no.uio.aeroscript.antlr.AeroScriptLexer;
import no.uio.aeroscript.antlr.AeroScriptParser;
import no.uio.aeroscript.runtime.Interpreter;
import no.uio.aeroscript.runtime.REPL;
import no.uio.aeroscript.type.Memory;
import no.uio.aeroscript.type.Point;
import no.uio.aeroscript.ast.stmt.Execution;
import no.uio.aeroscript.ast.stmt.Statement;
import no.uio.aeroscript.compiletime.ASTBuilder;
import no.uio.aeroscript.compiletime.TypeChecker;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Stack;

public class Main {
    private static void error(String msg) {
        System.err.println("Error: " + msg);
        System.exit(1);
    }

    public static void main(String[] args) {
        System.setProperty("org.jline.terminal.dumb", "true");
        HashMap<Memory, Object> heap = new HashMap<>();
        Stack<Statement> stack = new Stack<>();
        float batteryLevel = 10000.0f;
        float altitude = 0.0f;
        Point initialPosition = new Point(0.0f, 0.0f);
        boolean yesREPL = false;

        if (args.length < 1) {
            //String[] arr = {"src\\test\\java\\no\\uio\\aeroscript\\resources\\reactions.aero", "-b", "1000000", "-repl"};
            //args = arr;
            System.err.println("Usage: java -jar aeroscript.jar <path to file> [-b <battery level>] [-p <x> <y>] [-a <altitude>] [-repl]");
            System.exit(1);
        }
        String path = args[0];
        
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-b":
                    if (i + 1 < args.length) batteryLevel = Float.parseFloat(args[++i]);
                    else error("-b requires a float");
                    break;
                case "-p":
                    if (i + 2 < args.length) {
                        initialPosition = new Point(Float.parseFloat(args[++i]), Float.parseFloat(args[++i]));
                    } else error("-p requires two floats");
                    break;
                case "-a":
                    if (i + 1 < args.length) altitude = Float.parseFloat(args[++i]);
                    else error("-a requires a float");
                    break;
                case "-repl":
                    yesREPL = true;
                    break;
                default:
                    error("Unknown argument: " + args[i]);
            }
        }

        HashMap<String, Object> vars = new HashMap<>();

        vars.put("initial position", initialPosition);
        vars.put("current position", initialPosition);
        vars.put("battery level", batteryLevel);
        vars.put("initial battery level", batteryLevel);
        vars.put("distance travelled", 0.0f);
        vars.put("altitude", altitude);

        heap.put(Memory.VARIABLES, vars);
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            try {
                AeroScriptLexer lexer = new AeroScriptLexer(CharStreams.fromString(content));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                AeroScriptParser parser = new AeroScriptParser(tokens);
                AeroScriptParser.ProgramContext programContext = parser.program();

                TypeChecker typeChecker = new TypeChecker();
                Interpreter interpreter = new Interpreter(heap, stack);
                ASTBuilder astBuilder = new ASTBuilder(interpreter);

                Execution program = astBuilder.visitProgram(programContext);
                typeChecker.walk(program);

                if (yesREPL) {
                    interpreter.runProgramREPL(program);
                    REPL repl = new REPL(interpreter);
                    System.out.println("Welcome to the AeroScript REPL!");
                    while(!repl.isTerminating()) {
                        System.setProperty("org.jline.terminal", "jline.UnsupportedTerminal");
                        LineReader reader = LineReaderBuilder.builder().build();
                        String next;
                        String left;
                        String[] splits;
                        do {
                            next = reader.readLine("MO> ");
                            if (next == null) break;
                            splits = next.trim().split(" ", 2);
                            left = splits.length == 1 ? "" : splits[1].trim();
                        } while (!repl.command(splits[0], left));
                    }
                } else {
                    interpreter.runProgram(program);
                }
                
                System.out.println("Initial Position: " + initialPosition);
                System.out.println("Initial Battery level: " + batteryLevel);
                System.out.println("Final Position: " + interpreter.getCurrentPosition());
                System.out.println("Final Battery level: " + interpreter.getBatteryLevel());
                System.out.println("Distance travelled: " + interpreter.getDistanceTravelled());

                System.out.println("Execution complete!");
            } catch (ParseCancellationException e) {
                System.err.println("Parser error: " + e.getMessage());
            }
        }  catch (/*IOException e*/Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}