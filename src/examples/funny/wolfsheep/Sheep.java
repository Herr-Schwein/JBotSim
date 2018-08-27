package examples.funny.wolfsheep;

import jbotsim.Node;
import jbotsim.PRNG;
import jbotsim.Topology;

/**
 * Created by acasteig on 31/08/16.
 */
public class Sheep extends Node {
    private int speed = 1;
    private boolean isAlive = true;

    @Override
    public void onStart() {
        setIcon("sheep.png");
        setSize(12);
        setDirection(PRNG.nextDouble() * Math.PI * 2);
    }

    @Override
    public void onClock() {
        move(speed);
        wrapLocation();
    }

    @Override
    public void onPostClock() {
        Topology tp = getTopology();
        if ( ! isAlive ) {
            tp.removeNode(this);
            if (PRNG.nextDouble() < 0.5){
                tp.addNode(-1, -1, new Wolf());
            }
        }else{
            if (PRNG.nextDouble() < 0.01){
                tp.addNode(-1, -1, new Sheep());
            }
        }
    }

    public void kill(){
        isAlive = false;
    }
}
