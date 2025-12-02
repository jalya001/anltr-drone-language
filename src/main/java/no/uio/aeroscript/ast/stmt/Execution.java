package no.uio.aeroscript.ast.stmt;

import java.util.HashMap;

import no.uio.aeroscript.runtime.Interpreter;

public class Execution extends Statement {
    private Interpreter interpreter;
    public String id;
    public Statement[] statements;
    public Boolean prefix;
    public String next;
    public HashMap<String, Execution> executions;
    public HashMap<String, String> reactions;
    public HashMap<String, String> messages;

    public Execution(Interpreter interpreter, String id, Statement[] statements, Boolean prefix, String next) {
        this.interpreter = interpreter;
        this.id = id;
        this.statements = statements;
        this.prefix = prefix;
        this.next = next;
        this.executions = new HashMap<>();
        this.reactions = new HashMap<>();
        this.messages = new HashMap<>();
    }

    @Override
    public void execute() {
        System.out.println("Execute: " + id);

        if (next != null) {
            interpreter.pushToStack(interpreter.getModeExecutions().get(next));
        }

        Lambda exitModeCommand = new Lambda("Exit Mode", () -> interpreter.getLambda("modeEnd").run());
        interpreter.pushToStack(exitModeCommand);

        interpreter.pushAllToStackReverse(statements);
        
        Lambda enterModeCommand = new Lambda("Enter Mode", () -> interpreter.pushMode(this));
        interpreter.pushToStack(enterModeCommand);
    }

    @Override
    public String toString() {
        return "Execution " + id;
    }
}
