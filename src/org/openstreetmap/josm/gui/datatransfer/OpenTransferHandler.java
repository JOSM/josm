// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.gui.datatransfer.importers.AbstractOsmDataPaster;
import org.openstreetmap.josm.gui.datatransfer.importers.FilePaster;
import org.openstreetmap.josm.gui.datatransfer.importers.OsmLinkPaster;

/**
 * This transfer handler allows to e.g. drop files to open them.
 *
 * @author Michael Zangl
 * @since 10620
 * @since 10881 rename
 */
public class OpenTransferHandler extends AbstractStackTransferHandler {

    private static final Collection<AbstractOsmDataPaster> SUPPORTED = Arrays.asList(new FilePaster(), new OsmLinkPaster());

    @Override
    protected Collection<AbstractOsmDataPaster> getSupportedPasters() {
        return Collections.unmodifiableCollection(SUPPORTED);
    }
}
