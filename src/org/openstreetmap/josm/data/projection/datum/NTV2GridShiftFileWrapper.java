// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.InputStream;

import org.openstreetmap.josm.io.MirroredInputStream;

/**
 * Wrapper for {@link NTV2GridShiftFile}.
 *
 * Loads the shift file from disk, when it is first accessed.
 * @since 5226
 */
public class NTV2GridShiftFileWrapper {

    /**
     * Used in Germany to convert coordinates between the DHDN (<i>Deutsches Hauptdreiecksnetz</i>)
     * and ETRS89 (<i>European Terrestrial Reference System 1989</i>) datums.
     * @see <a href="http://crs.bkg.bund.de/crseu/crs/descrtrans/eu-descrtrans.php?crs_id=REVfREhETiAvIEdLXzM=&op_id=REVfREhETiAoQmVUQSwgMjAwNykgdG8gRVRSUzg5">
     * Description of Transformation - DE_DHDN (BeTA, 2007) to ETRS89</a>
     */
    public final static NTV2GridShiftFileWrapper BETA2007 = new NTV2GridShiftFileWrapper("resource://data/projection/BETA2007.gsb");

    /**
     * Used in France to convert coordinates between the NTF (<i>Nouvelle triangulation de la France</i>)
     * and RGF93 (<i>Réseau géodésique français 1993</i>) datums.
     * @see <a href="http://geodesie.ign.fr/contenu/fichiers/documentation/algorithmes/notice/NT111_V1_HARMEL_TransfoNTF-RGF93_FormatGrilleNTV2.pdf">
     * [French] Transformation de coordonnées NTF – RGF93 / Format de grille NTv2</a>
     */
    public final static NTV2GridShiftFileWrapper ntf_rgf93 = new NTV2GridShiftFileWrapper("resource://data/projection/ntf_r93_b.gsb");


    private NTV2GridShiftFile instance = null;
    private String gridFileName;

    /**
     * Constructs a new {@code NTV2GridShiftFileWrapper}.
     * @param filename Path to the grid file (GSB format)
     */
    public NTV2GridShiftFileWrapper(String filename) {
        this.gridFileName = filename;
    }

    /**
     * Returns the actual {@link NTV2GridShiftFile} behind this wrapper.
     * The grid file is only loaded once, when first accessed.
     * @return The NTv2 grid file
     */
    public NTV2GridShiftFile getShiftFile() {
        if (instance == null) {
            try {
                InputStream is = new MirroredInputStream(gridFileName);
                instance = new NTV2GridShiftFile();
                instance.loadGridShiftFile(is, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

}
