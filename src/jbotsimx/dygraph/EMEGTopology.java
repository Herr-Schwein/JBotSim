package jbotsimx.dygraph;

import java.util.List;

import jbotsim.Link;
import jbotsim.Node;
import jbotsim.PRNG;
import jbotsim.Topology;

public class EMEGTopology extends Topology{
    private double birthRate;
    private double deathRate;
    public EMEGTopology(double birthRate, double deathRate){
        this.birthRate=birthRate;
        this.deathRate=deathRate;
        setCommunicationRange(0);
    }
    public void initializeEdges() {
        for (Link l : super.getLinks())
            super.removeLink(l);
        List<Node> nodes = super.getNodes();
        for (int i=0; i<nodes.size(); i++){
            for (int j=i+1; j<nodes.size(); j++){
                Link l=new Link(nodes.get(i), nodes.get(j));
                if (PRNG.nextDouble() < birthRate/(birthRate+deathRate))
                    super.addLink(l);
            }
        }
    }
    public void updateLinks() {
        List<Node> nodes = super.getNodes();
        for (int i=0; i<nodes.size(); i++){
            for (int j=i+1; j<nodes.size(); j++){
                Link l=new Link(nodes.get(i), nodes.get(j));
                if (super.getLinks().contains(l)){
                    if (PRNG.nextDouble() < deathRate)
                        super.removeLink(l);
                }else{
                    if (PRNG.nextDouble() < birthRate)
                        super.addLink(l);
                }
            }
        }
    }
}
