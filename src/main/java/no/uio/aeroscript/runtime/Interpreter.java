package no.uio.aeroscript.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;

import no.uio.aeroscript.ast.stmt.Execution;
import no.uio.aeroscript.ast.stmt.Lambda;
import no.uio.aeroscript.ast.stmt.Statement;
import no.uio.aeroscript.type.Memory;
import no.uio.aeroscript.type.Point;

@SuppressWarnings("unchecked")

public class Interpreter {
    public HashMap<Memory, Object> heap;
    private Stack<Statement> stack;
    private Stack<Execution> modeStack = new Stack<Execution>();
    public boolean running = false;

    public Interpreter(HashMap<Memory, Object> heap, Stack<Statement> stack) {
        this.heap = heap;
        this.stack = stack;
    }

    public void pause() { System.out.println("Pausing..."); running = false; }
    public void resume() { System.out.println("Resuming..."); running = true; }

    public ArrayList<Execution> runProgram(Execution program) {
        System.out.println("Running program.");
        //pushAllToStackReverse(program.statements, s -> ((Execution) s).pending);

        HashMap<String, Runnable> lambdas = new HashMap<>();
        lambdas.put("modeEnd", () -> popMode());
        heap.put(Memory.LAMBDAS, lambdas);

        resume();
        program.execute();
        
        Execution first = null;
        for (Statement stm : program.statements) {
            if (stm instanceof Execution) {
                first = (Execution) stm;
                break;
            }
        }
        setFirstExecution(first); // This is useless, but you want it
        // If there are no executions in the program, it sets it to null
        return executeStack();
    }

    public ArrayList<Execution> runProgramREPL(Execution program) {
        System.out.println("Running program for REPL.");

        HashMap<String, Runnable> lambdas = new HashMap<>();
        lambdas.put("modeEnd", () -> { pause(); pushToStack(new Lambda("popmode", () -> popMode())); });
        heap.put(Memory.LAMBDAS, lambdas);
        resume();
        program.execute();
        
        Execution first = null;
        for (Statement stm : program.statements) {
            if (stm instanceof Execution) {
                first = (Execution) stm;
                break;
            }
        }
        setFirstExecution(first); // This is useless, but you want it
        // If there are no executions in the program, it sets it to null
        return executeStack();
    }

    public ArrayList<Execution> executeStack() {
        ArrayList<Execution> executions = new ArrayList<Execution>();
        System.out.println(stack);
        while(!stack.isEmpty() && running) {
            Statement stm = stack.pop();
            if (stm instanceof Execution) executions.add((Execution) stm);
            stm.execute();
        }
        return executions; // There is no need to keep track of these in my opinion, but you asked for it
    }

    public void pushToStack(Statement stm) {
        if (stm == null) return;
        System.out.println("pushed: " + stm.toString());
        stack.push(stm);
    }

    public void pushAllToStackReverse(Statement[] statements) {
        for (int i = statements.length - 1; i >= 0; i--) {
            pushToStack(statements[i]);
        }
    }

    public void pushAllToStackReverse(Statement[] statements, Predicate<Statement> condition) {
        for (int i = statements.length - 1; i >= 0; i--) {
            if (condition.test(statements[i])) pushToStack(statements[i]);
        }
    }

    public void checkBatteryLevel() {
        if (getBatteryLevel() < 20) {
            System.out.println("Low battery. Triggering low battery reaction");
            reactToKey("low battery", getModeReactions());
        }
    }

    public void clearStack() {
        stack.clear();
    }

    private void executionInterrupt(String executionName) {
        popMode();
        Map<String, Execution> executions = getModeExecutions();
        if (!executions.containsKey(executionName)) throw new IllegalArgumentException("Execution " + executionName + " not found in " + getMode());
        Execution execution = executions.get(executionName);
        clearStack();
        execution.execute();
    }

    public void pushMode(Execution mode) {
        System.out.println("Pushing " + mode + " to ModeStack.");
        modeStack.push(mode);
    }

    public void popMode() {
        Execution mode = modeStack.pop();
        System.out.println("Exiting mode " + mode + ".");
    }

    private boolean reactToKey(String key, Map<String, String> map) {
        if (map.containsKey(key)) {
            System.out.println("Reacting to " + key);
            String executionName = map.get(key);
            executionInterrupt(executionName);
            return true;
        }
        return false;
    }

    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
        if (!reactToKey(message, getModeMessages()) && !reactToKey(message, getModeReactions())) {
            throw new IllegalArgumentException("Message " + message + " not found in " + getMode());
        }
        resume();
        executeStack();
    }


    
    public HashMap<String, Execution> getModeExecutions() {
        return modeStack.peek().executions;
    }

    public HashMap<String, String> getModeReactions() {
        return modeStack.peek().reactions;
    }

    public HashMap<String, String> getModeMessages() {
        return modeStack.peek().messages;
    }

    public Execution getMode() {
        return modeStack.peek();
    }
    
    public HashMap<String, Runnable> getLambdas() {
        return (HashMap<String, Runnable>) heap.get(Memory.LAMBDAS);
    }
    
    public Runnable getLambda(String lambda) {
        return ((HashMap<String, Runnable>) heap.get(Memory.LAMBDAS)).get(lambda);
    }

    public HashMap<String, Object> getVariables() {
        return (HashMap<String, Object>) heap.get(Memory.VARIABLES);
    }

    public Execution getFirstExecution() {
        return (Execution) ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).get("first execution");
    }

    public void setFirstExecution(Execution firstExecution) {
        ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).put("first execution", firstExecution);
    }

    // All the below could probably be simplified to a "varGet" and "varSet"
    public Point getCurrentPosition() {
        return (Point) ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).get("current position");
    }

    public Float getAltitude() {
        return (Float) ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).get("altitude");
    }

    public Float getDistanceTravelled() {
        return (Float) ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).get("distance travelled");
    }

    public Float getBatteryLevel() {
        return (Float) ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).get("battery level");
    }

    public void setCurrentPosition(Point position) {
        ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).put("current position", position);
    }

    public void setAltitude(Float altitude) {
        ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).put("altitude", altitude);
    }

    public void setDistanceTravelled(Float distance) {
        ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).put("distance travelled", distance);
    }

    public void setBatteryLevel(Float batteryLevel) {
        ((HashMap<String, Object>) heap.get(Memory.VARIABLES)).put("battery level", batteryLevel);
    }
}
