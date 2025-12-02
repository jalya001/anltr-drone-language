package no.uio.aeroscript.ast.stmt;

public abstract class Statement {
    public abstract void execute();

    @Override
    public String toString() {
        return "Statement";
    }
}
