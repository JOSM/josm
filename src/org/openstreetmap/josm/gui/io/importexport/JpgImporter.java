// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

/**
*  File importer allowing to import geotagged images
*  @deprecated use {@link ImageImporter} instead
*/
@Deprecated
public class JpgImporter extends ImageImporter {

    /**
    * Constructs a new {@code JpgImporter}.
    */
    public JpgImporter() {
        super(false);
    }

    /**
    * Constructs a new {@code JpgImporter} with folders selection, if wanted.
    * @param includeFolders If true, includes folders in the file filter
    * @since 5438
    */
    public JpgImporter(boolean includeFolders) {
        super(includeFolders);
    }
}
