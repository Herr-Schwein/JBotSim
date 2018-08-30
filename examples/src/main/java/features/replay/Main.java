package examples.features.replay;

import jbotsim.Topology;
import jbotsimx.format.xml.XMLParser;
import jbotsimx.format.xml.XMLTopologyParser;
import jbotsimx.replay.TracePlayer;
import jbotsimx.replay.TraceRecorder;
import jbotsimx.ui.CommandListener;
import jbotsimx.ui.JViewer;

public class Main implements CommandListener, TracePlayer.ReplayTerminatedListener {
    private static final String START_RECORDER = "Start recorder";
    private static final String STOP_RECORDER = "Stop recorder";
    private static final String REPLAY_TRACE = "Replay trace";

    private static final String TRACE_FILENAME = "trace.xml";

    private Topology topology;
    private JViewer jViewer;
    private TraceRecorder recorder = null;
    private TracePlayer player = null;

    public Main() throws XMLParser.ParserException {
        topology = new Topology();
        XMLTopologyParser parser = new XMLTopologyParser(topology);
        parser.parse(getClass().getResourceAsStream("/examples/features/replay/network.xml"));

        jViewer = new JViewer(topology);
        jViewer.getJTopology().addCommandListener(this);
        jViewer.getJTopology().addCommand(START_RECORDER);
        jViewer.getJTopology().addCommand(STOP_RECORDER);
        jViewer.getJTopology().addCommand(REPLAY_TRACE);
    }

    @Override
    public void onCommand(String command) {
        try {
            if (command.equals(START_RECORDER)) {
                recorder = new TraceRecorder(topology);
                recorder.start();
            } else if (command.equals(STOP_RECORDER)) {
                recorder.stopAndWrite(TRACE_FILENAME);
                recorder = null;
            } else if (command.equals(REPLAY_TRACE)) {
                if (recorder != null) {
                    recorder.stopAndWrite(TRACE_FILENAME);
                    recorder = null;
                }
                topology.clear();
                player = new TracePlayer(topology);
                player.loadAndStart(TRACE_FILENAME);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReplayTerminated(TracePlayer tracePlayer) {
        player = null;
    }

    public void start() {
        topology.start();
    }

    public static void main(String[] args) {
        try {
            Main main = new Main();
            main.start ();
        } catch (XMLParser.ParserException e) {
            e.printStackTrace();
        }
    }
}
