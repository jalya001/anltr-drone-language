package no.uio.aeroscript.ast.stmt;

public class Lambda extends Statement {
    public final String label;
    private final Runnable action;

    public Lambda(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    @Override
    public void execute() {
        System.out.println("Running Lambda " + label);
        action.run();
    }

    @Override
    public String toString() {
        return label;
    }
}