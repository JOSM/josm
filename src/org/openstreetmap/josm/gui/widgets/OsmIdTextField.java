// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.tools.Logging;

/**
 * A text field designed to enter one or several OSM primitive IDs.
 * @author Matthias Julius
 */
public class OsmIdTextField extends AbstractIdTextField<OsmIdTextField.OsmIdValidator> {

    /**
     * Constructs a new {@link OsmIdTextField}
     */
    public OsmIdTextField() {
        super(OsmIdValidator.class);
    }

    /**
     * Sets the type of primitive object
     * @param type The type of primitive object (
     *      {@link OsmPrimitiveType#NODE NODE},
     *      {@link OsmPrimitiveType#WAY WAY},
     *      {@link OsmPrimitiveType#RELATION RELATION})
     */
    public void setType(OsmPrimitiveType type) {
        validator.type = type;
    }

    /**
     * Get entered ID list - supports "1,2,3" "1 2   ,3" or even "1 2 3 v2 6 v8"
     * @return list of id's
     */
    public final List<PrimitiveId> getIds() {
        return new ArrayList<>(validator.ids);
    }

    /**
     * Reads the OSM primitive id(s)
     * @return true if valid OSM objects IDs have been read, false otherwise
     * @see OsmIdValidator#readOsmIds
     */
    @Override
    public boolean readIds() {
        return validator.readOsmIds();
    }

    /**
     * Validator for an OSM primitive ID entered in a {@link JTextComponent}.
     */
    public static class OsmIdValidator extends AbstractTextComponentValidator {

        private final List<PrimitiveId> ids = new ArrayList<>();
        private OsmPrimitiveType type;

        /**
         * Constructs a new {@link OsmIdValidator}
         * @param tc The text component to validate
         */
        public OsmIdValidator(JTextComponent tc) {
            super(tc, false);
        }

        @Override
        public boolean isValid() {
            return readOsmIds();
        }

        @Override
        public void validate() {
            if (!isValid()) {
                feedbackInvalid(tr("The current value is not a valid OSM ID. Please enter an integer value > 0"));
            } else {
                feedbackValid(tr("Please enter an integer value > 0"));
            }
        }

        /**
         * Reads the OSM primitive id(s)
         * @return true if valid OSM objects IDs have been read, false otherwise
         */
        public boolean readOsmIds() {
            String value = getComponent().getText();
            char c;
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            ids.clear();
            StringTokenizer st = new StringTokenizer(value, ",.+/ \t\n");
            String s;
            while (st.hasMoreTokens()) {
                s = st.nextToken();
                // convert tokens to int skipping v-words (version v2 etc)
                c = s.charAt(0);
                if (c == 'v') {
                    continue;
                } else {
                    try {
                        ids.addAll(SimplePrimitiveId.multipleFromString(s));
                    } catch (IllegalArgumentException ex) {
                        try {
                            Logging.trace(ex);
                            long id = Long.parseLong(s);
                            if (id <= 0) {
                                return false;
                            } else if (type == OsmPrimitiveType.NODE) {
                                ids.add(new SimplePrimitiveId(id, OsmPrimitiveType.NODE));
                            } else if (type == OsmPrimitiveType.WAY || type == OsmPrimitiveType.CLOSEDWAY) {
                                ids.add(new SimplePrimitiveId(id, OsmPrimitiveType.WAY));
                            } else if (type == OsmPrimitiveType.RELATION || type == OsmPrimitiveType.MULTIPOLYGON) {
                                ids.add(new SimplePrimitiveId(id, OsmPrimitiveType.RELATION));
                            } else {
                                return false;
                            }
                        } catch (IllegalArgumentException ex2) {
                            Logging.trace(ex2);
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
}
