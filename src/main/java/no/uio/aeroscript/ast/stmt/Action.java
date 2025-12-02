package no.uio.aeroscript.ast.stmt;

import no.uio.aeroscript.ast.expr.Node;
import no.uio.aeroscript.runtime.Interpreter;
import no.uio.aeroscript.type.ActionType;
import no.uio.aeroscript.type.Point;

public class Action extends Statement {
    private Interpreter interpreter;
    public ActionType action;
    public Node data;
    public Node speed;
    public Node duration;

    public Action(Interpreter interpreter, ActionType action, Node data, Node speed, Node duration) {
        this.interpreter = interpreter;
        this.action = action;
        this.data = data;
        this.speed = speed;
        this.duration = duration;
    }

    @Override
    public void execute() {
        Float evalSpeed = (Float) speed.evaluate();
        Float evalDuration = (Float) duration.evaluate();
        Float cost = (evalDuration * 0.1f) + (evalSpeed * 1f); // Makes no sense
        
        Object evalData = data.evaluate();
        System.out.println("Act: " + action + " " + evalData.toString());

        switch (action) {
            case MOVE:
                Point nPoint;
                Float distance;
                if (evalData instanceof Point) {
                    Float px = ((Point) evalData).getX() - interpreter.getCurrentPosition().getX();
                    Float py = ((Point) evalData).getY() - interpreter.getCurrentPosition().getY();
                    distance = (float) Math.sqrt(Math.pow(px,2) + Math.pow(py,2)); // Small float intentional, since type weirdness
                    cost += distance * 0.7f;
                    nPoint = (Point) evalData;
                } else {
                    nPoint = new Point(interpreter.getCurrentPosition().getX() + (Float) evalData, interpreter.getCurrentPosition().getY());
                    distance = (Float) evalData;
                    cost += Math.abs((Float) evalData * 0.5f);
                }
                testBattery(cost);
                interpreter.setCurrentPosition(nPoint);
                interpreter.setDistanceTravelled(interpreter.getDistanceTravelled() + distance);
                break;
            case TURN: // But there is no rotation variable to change?
                cost += Math.abs((Float) evalData * 0.3f); // Getting abs fixes an old bug
                testBattery(cost);
                break;
            case SCEND:
                if ((Float) evalData > 0) {
                    cost += (Float) evalData * 0.6f;
                } else {
                    cost += Math.abs((Float) evalData * 0.2f);
                }
                testBattery(cost);
                interpreter.setAltitude(interpreter.getAltitude() + (Float) evalData);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
        interpreter.setBatteryLevel(interpreter.getBatteryLevel() - cost);
    }

    private void testBattery(Float cost) {
        System.out.println("Cost: " + cost);
        interpreter.checkBatteryLevel();
        if (interpreter.getBatteryLevel() < cost) {
            throw new RuntimeException ("Not enough battery to perform action.");
        }
    }

    @Override
    public String toString() {
        return "Action: " + action.toString() + " " + data.toString() + " " + speed.toString() + " " + duration.toString();
    }
}
