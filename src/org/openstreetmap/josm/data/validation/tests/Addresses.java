// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;

/**
 * Performs validation tests on addresses (addr:housenumber) and associatedStreet relations.
 * @since 5644
 */
public class Addresses extends Test {

    protected static final int HOUSE_NUMBER_WITHOUT_STREET = 2601;
    protected static final int DUPLICATE_HOUSE_NUMBER = 2602;
    protected static final int MULTIPLE_STREET_NAMES = 2603;
    protected static final int MULTIPLE_STREET_RELATIONS = 2604;
    protected static final int HOUSE_NUMBER_TOO_FAR = 2605;

    protected static final String ADDR_HOUSE_NUMBER  = "addr:housenumber";
    protected static final String ADDR_INTERPOLATION = "addr:interpolation";
    protected static final String ADDR_PLACE         = "addr:place";
    protected static final String ADDR_STREET        = "addr:street";
    protected static final String ASSOCIATED_STREET  = "associatedStreet";

    protected class AddressError extends TestError {

        public AddressError(int code, OsmPrimitive p, String message) {
            this(code, Collections.singleton(p), message);
        }
        public AddressError(int code, Collection<OsmPrimitive> collection, String message) {
            this(code, collection, message, null, null);
        }
        public AddressError(int code, Collection<OsmPrimitive> collection, String message, String description, String englishDescription) {
            super(Addresses.this, Severity.WARNING, message, description, englishDescription, code, collection);
        }
    }

    /**
     * Constructor
     */
    public Addresses() {
        super(tr("Addresses"), tr("Checks for errors in addresses and associatedStreet relations."));
    }

    protected List<Relation> getAndCheckAssociatedStreets(OsmPrimitive p) {
        List<Relation> list = OsmPrimitive.getFilteredList(p.getReferrers(), Relation.class);
        for (Iterator<Relation> it = list.iterator(); it.hasNext();) {
            Relation r = it.next();
            if (!r.hasTag("type", ASSOCIATED_STREET)) {
                it.remove();
            }
        }
        if (list.size() > 1) {
            List<OsmPrimitive> errorList = new ArrayList<OsmPrimitive>(list);
            errorList.add(0, p);
            errors.add(new AddressError(MULTIPLE_STREET_RELATIONS, errorList, tr("Multiple associatedStreet relations")));
        }
        return list;
    }

    protected void checkHouseNumbersWithoutStreet(OsmPrimitive p) {
        List<Relation> associatedStreets = getAndCheckAssociatedStreets(p);
        // Find house number without proper location (neither addr:street, associatedStreet, addr:place or addr:interpolation)
        if (p.hasKey(ADDR_HOUSE_NUMBER) && !p.hasKey(ADDR_STREET) && !p.hasKey(ADDR_PLACE)) {
            for (Relation r : associatedStreets) {
                if (r.hasTag("type", ASSOCIATED_STREET)) {
                    return;
                }
            }
            for (Way w : OsmPrimitive.getFilteredList(p.getReferrers(), Way.class)) {
                if (w.hasKey(ADDR_INTERPOLATION) && w.hasKey(ADDR_STREET)) {
                    return;
                }
            }
            // No street found
            errors.add(new AddressError(HOUSE_NUMBER_WITHOUT_STREET, p, tr("House number without street")));
        }
    }

    @Override
    public void visit(Node n) {
        checkHouseNumbersWithoutStreet(n);
    }

    @Override
    public void visit(Way w) {
        checkHouseNumbersWithoutStreet(w);
    }

    @Override
    public void visit(Relation r) {
        checkHouseNumbersWithoutStreet(r);
        if (r.hasTag("type", ASSOCIATED_STREET)) {
            // Used to count occurences of each house number in order to find duplicates
            Map<String, List<OsmPrimitive>> map = new HashMap<String, List<OsmPrimitive>>();
            // Used to detect different street names
            String relationName = r.get("name");
            Set<OsmPrimitive> wrongStreetNames = new HashSet<OsmPrimitive>();
            // Used to check distance
            Set<OsmPrimitive> houses = new HashSet<OsmPrimitive>();
            Set<Way> street = new HashSet<Way>();
            for (RelationMember m : r.getMembers()) {
                String role = m.getRole();
                OsmPrimitive p = m.getMember();
                if (role.equals("house")) {
                    houses.add(p);
                    String number = p.get(ADDR_HOUSE_NUMBER);
                    if (number != null) {
                        number = number.trim().toUpperCase();
                        List<OsmPrimitive> list = map.get(number);
                        if (list == null) {
                            map.put(number, list = new ArrayList<OsmPrimitive>());
                        }
                        list.add(p);
                    }
                } else if (role.equals("street")) {
                    if (p instanceof Way) {
                        street.add((Way) p);
                    }
                    if (relationName != null && p.hasKey("name") && !relationName.equals(p.get("name"))) {
                        if (wrongStreetNames.isEmpty()) {
                            wrongStreetNames.add(r);
                        }
                        wrongStreetNames.add(p);
                    }
                }
            }
            // Report duplicate house numbers
            String englishDescription = marktr("House number ''{0}'' duplicated");
            for (Entry<String, List<OsmPrimitive>> entry : map.entrySet()) {
                List<OsmPrimitive> list = entry.getValue();
                if (list.size() > 1) {
                    errors.add(new AddressError(DUPLICATE_HOUSE_NUMBER, list,
                            tr("Duplicate house numbers"), tr(englishDescription, entry.getKey()), englishDescription));
                }
            }
            // Report wrong street names
            if (!wrongStreetNames.isEmpty()) {
                errors.add(new AddressError(MULTIPLE_STREET_NAMES, wrongStreetNames,
                        tr("Multiple street names in relation")));
            }
            // Report addresses too far away
            if (!street.isEmpty()) {
                for (OsmPrimitive house : houses) {
                    if (house.isUsable()) {
                        checkDistance(house, street);
                    }
                }
            }
        }
    }

    protected void checkDistance(OsmPrimitive house, Collection<Way> street) {
        EastNorth centroid;
        if (house instanceof Node) {
            centroid = ((Node) house).getEastNorth();
        } else if (house instanceof Way) {
            List<Node> nodes = ((Way)house).getNodes();
            if (house.hasKey(ADDR_INTERPOLATION)) {
                for (Node n : nodes) {
                    if (n.hasKey(ADDR_HOUSE_NUMBER)) {
                        checkDistance(n, street);
                    }
                }
                return;
            }
            centroid = Geometry.getCentroid(nodes);
        } else {
            return; // TODO handle multipolygon houses ?
        }
        if (centroid == null) return; // fix #8305
        double maxDistance = Main.pref.getDouble("validator.addresses.max_street_distance", 200.0);
        boolean hasIncompleteWays = false;
        for (Way streetPart : street) {
            for (Pair<Node, Node> chunk : streetPart.getNodePairs(false)) {
                EastNorth p1 = chunk.a.getEastNorth();
                EastNorth p2 = chunk.b.getEastNorth();
                if (p1 != null && p2 != null) {
                    EastNorth closest = Geometry.closestPointToSegment(p1, p2, centroid);
                    if (closest.distance(centroid) <= maxDistance) {
                        return;
                    }
                } else {
                    Main.warn("Addresses test skipped chunck "+chunk+" for street part "+streetPart+" because p1 or p2 is null");
                }
            }
            if (!hasIncompleteWays && streetPart.isIncomplete()) {
                hasIncompleteWays = true;
            }
        }
        // No street segment found near this house, report error on if the relation does not contain incomplete street ways (fix #8314)
        if (hasIncompleteWays) return;
        List<OsmPrimitive> errorList = new ArrayList<OsmPrimitive>(street);
        errorList.add(0, house);
        errors.add(new AddressError(HOUSE_NUMBER_TOO_FAR, errorList,
                tr("House number too far from street")));
    }
}
