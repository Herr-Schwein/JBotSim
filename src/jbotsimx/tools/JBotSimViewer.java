package jbotsimx.tools;

import jbotsim.Topology;
import jbotsimx.format.common.Format;
import jbotsimx.ui.JViewer;

public class JBotSimViewer {
    public static void main(String[] args) {
        Topology tp = new Topology();
        if (args.length > 0) {
            Format.importFromFile(tp, args[0]);
        }
        new JViewer(tp);
        tp.start();
    }
}
