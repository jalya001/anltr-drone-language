package no.uio.aeroscript.type;

public class Point {
    private final Float x;
    private final Float y;

    public Point(Float x, Float y) {
        this.x = x;
        this.y = y;
    }

    public Float getX() { return x; }
    public Float getY() { return y; }

    @Override
    public String toString() {
        return "(" + x.toString() + ", " + y.toString() + ")";
    }
}
