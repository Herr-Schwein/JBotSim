package jbotsimx.dygraph;

import jbotsim.Node;
import jbotsim.PRNG;
import jbotsim.Topology;
import jbotsim.event.ClockListener;
import jbotsimx.ui.JViewer;

public class EMEGPlayer implements ClockListener{
    protected TVG tvg;
    protected Topology tp;
    protected double birthRate, deathRate, steadyProb;
    /**
     * Plays the specified _demo.J_tvg as an EMEG on the specified topology.
     * @param tvg the time-varying graph
     * @param tp the target topology
     * @param birthRate the desired birth rate
     * @param deathRate the desired death rate
     */
    public EMEGPlayer (TVG tvg, Topology tp, double birthRate, double deathRate){
        this.tvg=tvg;
        this.tp=tp;
        this.birthRate=birthRate;
        this.deathRate=deathRate;
        this.steadyProb = birthRate/(birthRate+deathRate);
        for (Node n : tvg.nodes)
            tp.addNode(n);
    }
    public void start(){
        tp.resetTime();
        tp.addClockListener(this);
        for (TVLink l : tvg.tvlinks)
            if (PRNG.nextDouble() < steadyProb)
                tp.addLink(l);
    }
    public void onClock(){
        updateLinks();
    }
    protected void updateLinks(){
        for (TVLink l : tvg.tvlinks){
            if (tp.getLinks().contains(l) && PRNG.nextDouble() < deathRate)
                tp.removeLink(l);
            else if (!tp.getLinks().contains(l) && PRNG.nextDouble() < birthRate)
                tp.addLink(l);
        }        
    }
    public static void main(String args[]){
        Topology tp=new Topology(400, 400);
        new JViewer(tp);
        TVG tvg=new TVG();
        tvg.buildCompleteGraph(30);
        (new EMEGPlayer(tvg, tp, .2, .4)).start();
        //new TopologyObserver(tp);
        tp.setClockSpeed(10);
    }
}
