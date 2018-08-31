package jbotsimx.tools;

import jbotsim.Topology;
import jbotsimx.replay.TracePlayer;
import jbotsimx.ui.JViewer;

public class JBotSimPlayer {
    public static void main(String[] args) {
        try {
            Topology topology = new Topology();
            new JViewer(topology);
            TracePlayer tp = new TracePlayer(topology);
            tp.loadAndStart(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
