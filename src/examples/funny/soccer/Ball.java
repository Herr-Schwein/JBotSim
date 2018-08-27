package examples.funny.soccer;

import jbotsim.Node;

import jbotsim.PRNG;
import jbotsim.Point;

/**
 * Created by Arnaud Casteigts on 06/04/17.
 */
public class Ball extends Node{
    double speed = 0;
    static final double fadingRatio = 1.2;

    @Override
    public void onStart() {
        setIcon("ball.png");
        disableWireless();
    }

    @Override
    public void onClock() {
        if (speed > 0) {
            move(speed);
            speed = speed / fadingRatio;
            wrapLocation();
            if (speed < 1) {
                speed = 0;
            }
        }
    }

    public void randomShoot(){
        double x = PRNG.nextDouble()*getTopology().getWidth();
        double y = PRNG.nextDouble()*getTopology().getHeight();
        Point p = new Point(x, y);
        double speed = PRNG.nextDouble()*40 + 10;
        shoot(p, speed);
    }

    public void shoot(double angle, double speed){
        setDirection(angle);
        this.speed = speed;
    }

    public void shoot(Point direction, double speed){
        setDirection(direction);
        this.speed = speed;
    }
}
