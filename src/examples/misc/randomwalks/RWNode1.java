package examples.misc.randomwalks;

import jbotsim.Node;

import jbotsim.Color;
import jbotsim.PRNG;

/**
 * Created by acasteig on 17/06/15.
 */
public class RWNode1 extends Node {
    @Override
    public void onSelection() {
        setColor(Color.black);
    }

    @Override
    public void onClock() {
        if (getColor() == Color.black) {
            Node next = getNeighbors().get(PRNG.nextInt(getNeighbors().size()));
            next.setColor(Color.black);
            setColor(null);
        }
    }
}
