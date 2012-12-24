// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.RelationMember;

public class RelationSorter {

    private static interface AdditionalSorter {
        public boolean acceptsMember(RelationMember m);
        public List<RelationMember> sortMembers(List<RelationMember> list);
    }

    private static final Collection<AdditionalSorter> additionalSorters = new ArrayList<AdditionalSorter>();

    static {
        additionalSorters.add(new AssociatedStreetSorter());
    }

    /**
     * Class that sorts type=associatedStreet relation's houses.
     */
    private static class AssociatedStreetSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(RelationMember m) {
            return m != null
                    && m.getRole() != null && m.getRole().equals("house")
                    && m.getMember() != null && m.getMember().get("addr:housenumber") != null;
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            Collections.sort(list, new Comparator<RelationMember>() {
                @Override
                public int compare(RelationMember a, RelationMember b) {
                    if (a == b || a.getMember() == b.getMember()) return 0;
                    String addrA = a.getMember().get("addr:housenumber").trim();
                    String addrB = b.getMember().get("addr:housenumber").trim();
                    if (addrA.equals(addrB)) return 0;
                    // Strip non-digits (from "1B" addresses for example)
                    String addrAnum = addrA.replaceAll("\\D+", "");
                    String addrBnum = addrB.replaceAll("\\D+", "");
                    // Compare only numbers
                    try {
                        Integer res = Integer.parseInt(addrAnum) - Integer.parseInt(addrBnum);
                        if (res != 0) return res;
                    } catch (NumberFormatException e) {
                        // Ignore NumberFormatException. If the number is not composed of digits, strings are compared next
                    }
                    // Same number ? Compare full strings
                    return addrA.compareTo(addrB);
                }
            });
            return list;
        }
    }

    /*
     * Sort a collection of relation members by the way they are linked.
     *
     * @param relationMembers collection of relation members
     * @return sorted collection of relation members
     */
    public List<RelationMember> sortMembers(List<RelationMember> relationMembers) {
        ArrayList<RelationMember> newMembers = new ArrayList<RelationMember>();

        // Sort members with custom mechanisms (relation-dependent)
        List<RelationMember> defaultMembers = new ArrayList<RelationMember>(relationMembers.size());
        Map<AdditionalSorter, List<RelationMember>> customMap = new HashMap<AdditionalSorter, List<RelationMember>>();

        // Dispatch members to correct sorters
        for (RelationMember m : relationMembers) {
            for (AdditionalSorter sorter : additionalSorters) {
                List<RelationMember> list = defaultMembers;
                if (sorter.acceptsMember(m)) {
                    list = customMap.get(sorter);
                    if (list == null) {
                        customMap.put(sorter, list = new LinkedList<RelationMember>());
                    }
                }
                list.add(m);
            }
        }

        // Sort members and add them to result
        for (AdditionalSorter s : customMap.keySet()) {
            newMembers.addAll(s.sortMembers(customMap.get(s)));
        }

        RelationNodeMap map = new RelationNodeMap(defaultMembers);
        // List of groups of linked members
        //
        List<LinkedList<Integer>> allGroups = new ArrayList<LinkedList<Integer>>();

        // current group of members that are linked among each other
        // Two successive members are always linked i.e. have a common node.
        //
        LinkedList<Integer> group;

        Integer first;
        while ((first = map.pop()) != null) {
            group = new LinkedList<Integer>();
            group.add(first);

            allGroups.add(group);

            Integer next = first;
            while ((next = map.popAdjacent(next)) != null) {
                group.addLast(next);
            }

            // The first element need not be in front of the list.
            // So the search goes in both directions
            //
            next = first;
            while ((next = map.popAdjacent(next)) != null) {
                group.addFirst(next);
            }
        }

        for (LinkedList<Integer> tmpGroup : allGroups) {
            for (Integer p : tmpGroup) {
                newMembers.add(defaultMembers.get(p));
            }
        }

        // Finally, add members that have not been sorted at all
        for (Integer i : map.getNotSortableMembers()) {
            newMembers.add(defaultMembers.get(i));
        }

        return newMembers;
    }

}
