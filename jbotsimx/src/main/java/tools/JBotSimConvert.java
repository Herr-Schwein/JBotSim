package jbotsimx.tools;

import jbotsim.Topology;
import jbotsimx.format.common.Format;

public class JBotSimConvert {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: " + JBotSimConvert.class.getName() + " input-file output-file");
            System.exit(1);
        }
        Topology tp = Format.importFromFile(args[0]);
        if (tp != null) {
            Format.exportToFile(tp, args[1]);
        }
        System.exit(0);
    }
}
