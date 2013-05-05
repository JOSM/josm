// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;

import org.openstreetmap.josm.io.MirroredInputStream;

/**
 * Wrapper for NTV2GridShiftFile.
 *
 * Loads the shift file from disk, when it is first accessed.
 */
public class NTV2GridShiftFileWrapper {

    public final static NTV2GridShiftFileWrapper BETA2007 = new NTV2GridShiftFileWrapper("resource://data/BETA2007.gsb");
    public final static NTV2GridShiftFileWrapper ntf_rgf93 = new NTV2GridShiftFileWrapper("resource://data/ntf_r93_b.gsb");
    

    private NTV2GridShiftFile instance = null;
    private String gridFileName;

    public NTV2GridShiftFileWrapper(String filename) {
        this.gridFileName = filename;
    }

    public NTV2GridShiftFile getShiftFile() {
        if (instance == null) {
            try {
                InputStream is = new MirroredInputStream(gridFileName);
                if (is == null)
                    throw new RuntimeException(tr("Error: failed to open input stream for resource ''/data/{0}''.", gridFileName));
                instance = new NTV2GridShiftFile();
                instance.loadGridShiftFile(is, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

}
