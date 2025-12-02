package no.uio.aeroscript.type;

public class Range {
    private final Float start;
    private final Float end;

    public Range(Float start, Float end) {
        this.start = start;
        this.end = end;
    }

    public Float getStart() { return start; }
    public Float getEnd() { return end; }
}
