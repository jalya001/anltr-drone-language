package no.uio.aeroscript.error;

public class TypeError extends RuntimeException {
    public TypeError(String msg) {
        super(msg);
    }
    public TypeError() {
        super("Unspecified type error.");
    }
}