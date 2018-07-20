package jbotsimx.format.dot;

import jbotsim.Topology;
import jbotsimx.format.common.Format;
import jbotsimx.format.xml.XMLTopologyFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class DotParserTest {
    private static final String TEST_RC_ROOT = "/dotinputs/";

    @Parameterized.Parameter
    public String dotFileName;

    @Parameterized.Parameters
    public static Collection<String> makers() {
        return Arrays.asList(
                "arboricity-100-2.dot",
                "barbell-6-5-4.dot",
                "cactus-20.dot",
                "kstar-20-2.dot",
                "paley-10.dot",
                "paley-10.xdot",
                "sunlet-10-directed.dot");
    }

    @Test
    public void dotParserTest() throws IOException {
        URL url = getClass().getResource(TEST_RC_ROOT + dotFileName);

        Topology tp = Format.importFromFile(url.getPath());
        assertNotNull(tp);

        String xmlTp = Format.exportToString(tp, new XMLTopologyFormatter());
        assertNotNull(xmlTp);

        String expectedXml = new String(Files.readAllBytes(Paths.get(url.getPath()+".xml")));

        assertEquals(expectedXml, xmlTp);
    }
}
