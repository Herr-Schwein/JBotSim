package examples.funny.cowboy;

import jbotsim.Node;

import java.io.File;

public class Cow extends Node{
    static Node farmer;
    int speed = 0;

    public Cow(){
        setIcon("cow.png");
        setSize(30);
        disableWireless();
    }

    @Override
    public void onClock() {
        if (speed > 0)
            move(speed--);
        else
            if (speed == 0 && this.distance(farmer)<20) {
                setDirection(farmer.getLocation(),true);
                speed = 12;
            }
        wrapLocation();
    }
}
