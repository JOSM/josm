// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;

/**
 * @author Matthias Julius
 */
public class OsmIdTextField extends JTextField {

    private OsmIdValidator validator;

    public OsmIdTextField() {
        validator = OsmIdValidator.decorate(this);
    }

    public void setType(OsmPrimitiveType type) {
        validator.type = type;
    }

    public long getOsmId() {
        return validator.getOsmId();
    }

    /**
     * Get entered ID list - supports "1,2,3" "1 2   ,3" or even "1 2 3 v2 6 v8"
     * @return list of id's
     */
    public List<PrimitiveId> getIds() {
        return validator.ids;
    }

    public boolean readOsmIds() {
        return validator.readOsmIds();
    }


    /**
     * Validator for a changeset ID entered in a {@see JTextComponent}.
     *
     */
    static private class OsmIdValidator extends AbstractTextComponentValidator {

        static public OsmIdValidator decorate(JTextComponent tc) {
            return new OsmIdValidator(tc);
        }

        private List<PrimitiveId> ids = new ArrayList<PrimitiveId>();
        private OsmPrimitiveType type;

        public OsmIdValidator(JTextComponent tc) {
            super(tc, false);
        }

        @Override
        public boolean isValid() {
            return getOsmId() > 0 || readOsmIds() != false;
        }

        @Override
        public void validate() {
            if (!isValid()) {
                feedbackInvalid(tr("The current value is not a valid OSM ID. Please enter an integer value > 0"));
            } else {
                feedbackValid(tr("Please enter an integer value > 0"));
            }
        }

        public long getOsmId() {
            String value  = getComponent().getText();
            if (value == null || value.trim().length() == 0) return 0;
            try {
                long osmId = Long.parseLong(value.trim());
                if (osmId > 0) 
                    return osmId;
                return 0;
            } catch(NumberFormatException e) {
                return 0;
            }
        }
        
        public boolean readOsmIds() {
            String value  = getComponent().getText();
            char c;
            if (value == null || value.trim().length() == 0) return false;
            try {
                ids.clear();
                StringTokenizer st = new StringTokenizer(value,",.+/ \t\n");
                String s;
                while (st.hasMoreTokens()) {
                    s = st.nextToken();
                    // convert tokens to int skipping v-words (version v2 etc)
                    c = s.charAt(0);
                    if (c=='v') {
                        continue;
                    }
                    else if (c=='n') {
                        ids.add(new SimplePrimitiveId(Long.parseLong(s.substring(1)), OsmPrimitiveType.NODE));
                    } else if (c=='w') {
                        ids.add(new SimplePrimitiveId(Long.parseLong(s.substring(1)), OsmPrimitiveType.WAY));
                    } else if (c=='r') { 
                        ids.add(new SimplePrimitiveId(Long.parseLong(s.substring(1)), OsmPrimitiveType.RELATION));
                    } else if (type==OsmPrimitiveType.NODE) {
                        ids.add(new SimplePrimitiveId(Long.parseLong(s), OsmPrimitiveType.NODE));
                    } else if (type==OsmPrimitiveType.WAY) {
                        ids.add(new SimplePrimitiveId(Long.parseLong(s), OsmPrimitiveType.WAY));
                    } else if (type==OsmPrimitiveType.RELATION) {
                        ids.add(new SimplePrimitiveId(Long.parseLong(s), OsmPrimitiveType.RELATION));
                    }
                }
                return true;
            } catch(NumberFormatException e) {
                return false;
            }
        }
    }
}
