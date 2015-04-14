package org.openstreetmap.josm.io.remotecontrol.handler;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImportHandlerTest {

    @Test
    public void test7434() throws Exception {

        final ImportHandler req = new ImportHandler();
        req.setUrl("http://localhost:8111/import?url=http://localhost:8888/relations?relations=19711&mode=recursive");
        assertThat(req.args.get("url"), CoreMatchers.is("http://localhost:8888/relations?relations=19711&mode=recursive"));

    }

}
