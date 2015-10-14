// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link ImportHandler} class.
 */
public class ImportHandlerTest {

    /**
     * Non-regression test for bug #7434.
     */
    @Test
    public void testTicket7434() {
        final ImportHandler req = new ImportHandler();
        req.setUrl("http://localhost:8111/import?url=http://localhost:8888/relations?relations=19711&mode=recursive");
        assertEquals("http://localhost:8888/relations?relations=19711&mode=recursive", req.args.get("url"));
    }
}
