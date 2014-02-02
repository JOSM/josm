// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.AlphanumComparator;

public class RelationSorter {

    private static interface AdditionalSorter {
        public boolean acceptsMember(RelationMember m);
        public List<RelationMember> sortMembers(List<RelationMember> list);
    }

    private static final Collection<AdditionalSorter> additionalSorters = new ArrayList<AdditionalSorter>();

    static {
        // first adequate sorter is used, so order matters
        additionalSorters.add(new AssociatedStreetRoleStreetSorter());
        additionalSorters.add(new AssociatedStreetRoleAddressHouseSorter());
    }

    /**
     * Class that sorts the {@code street} members of
     * {@code type=associatedStreet} and {@code type=street} relations.
     */
    private static class AssociatedStreetRoleStreetSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(RelationMember m) {
            return "street".equals(m.getRole());
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            return sortMembersByConnectivity(list);
        }
    }

    /**
     * Class that sorts the {@code address} and {@code house} members of
     * {@code type=associatedStreet} and {@code type=street} relations.
     */
    private static class AssociatedStreetRoleAddressHouseSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(RelationMember m) {
            return "address".equals(m.getRole()) || "house".equals(m.getRole());
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            Collections.sort(list, new Comparator<RelationMember>() {
                @Override
                public int compare(RelationMember a, RelationMember b) {
                    final int houseNumber = AlphanumComparator.getInstance().compare(
                            a.getMember().get("addr:housenumber"),
                            b.getMember().get("addr:housenumber"));
                    if (houseNumber != 0) {
                        return houseNumber;
                    }
                    final String aDisplayName = a.getMember().getDisplayName(DefaultNameFormatter.getInstance());
                    final String bDisplayName = b.getMember().getDisplayName(DefaultNameFormatter.getInstance());
                    return AlphanumComparator.getInstance().compare(aDisplayName, bDisplayName);
                }
            });
            return list;
        }
    }

    /**
     * Sort a collection of relation members by the way they are linked.
     *
     * @param relationMembers collection of relation members
     * @return sorted collection of relation members
     */
    public List<RelationMember> sortMembers(List<RelationMember> relationMembers) {
        List<RelationMember> newMembers = new ArrayList<RelationMember>();

        // Sort members with custom mechanisms (relation-dependent)
        List<RelationMember> defaultMembers = new ArrayList<RelationMember>(relationMembers.size());
        // Maps sorter to assigned members for sorting. Use LinkedHashMap to retain order.
        Map<AdditionalSorter, List<RelationMember>> customMap = new LinkedHashMap<AdditionalSorter, List<RelationMember>>();

        // Dispatch members to the first adequate sorter
        for (RelationMember m : relationMembers) {
            boolean wasAdded = false;
            for (AdditionalSorter sorter : additionalSorters) {
                if (sorter.acceptsMember(m)) {
                    List<RelationMember> list;
                    list = customMap.get(sorter);
                    if (list == null) {
                        customMap.put(sorter, list = new LinkedList<RelationMember>());
                    }
                    list.add(m);
                    wasAdded = true;
                    break;
                }
            }
            if (!wasAdded) {
                defaultMembers.add(m);
            }
        }

        // Sort members and add them to result
        for (Entry<AdditionalSorter, List<RelationMember>> entry : customMap.entrySet()) {
            newMembers.addAll(entry.getKey().sortMembers(entry.getValue()));
        }
        newMembers.addAll(sortMembersByConnectivity(defaultMembers));
        return newMembers;
    }

    public static List<RelationMember> sortMembersByConnectivity(List<RelationMember> defaultMembers) {

        List<RelationMember> newMembers = new ArrayList<RelationMember>();

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
