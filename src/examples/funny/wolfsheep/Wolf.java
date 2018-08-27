package examples.funny.wolfsheep;

import jbotsim.Node;
import jbotsim.PRNG;

/**
 * Created by acasteig on 31/08/16.
 */
public class Wolf extends Node {
    private int speed = 2;

    @Override
    public void onStart() {
        setIcon("wolf.png");
        setSize(20);
        setSensingRange(50);
        setDirection(PRNG.nextDouble() * Math.PI * 2);
    }

    @Override
    public void onClock() {
        move(speed);
        wrapLocation();
    }

    @Override
    public void onPostClock() {
        if (PRNG.nextDouble() < 0.005){
            getTopology().removeNode(this);
        }
    }

    @Override
    public void onSensingIn(Node node) {
        if (node instanceof Sheep){
            ((Sheep) node).kill();
        }
    }
}
