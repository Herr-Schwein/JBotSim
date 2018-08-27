package examples.basic.moving;

import jbotsim.Node;
import jbotsim.PRNG;

/**
 * Created by acasteig on 2/20/15.
 */
public class MovingNode extends Node{
    @Override
    public void onStart() {
        setDirection(PRNG.nextDouble()*2*Math.PI);
    }

    @Override
    public void onClock() {
        move();
        wrapLocation(); // toroidal space
    }
}
