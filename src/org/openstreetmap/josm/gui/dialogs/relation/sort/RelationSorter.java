// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.AlphanumComparator;

/**
 * This class sorts the relation members by connectivity.
 * <p>
 * Multiple {@link AdditionalSorter}s are implemented to handle special relation types.
 */
public class RelationSorter {

    private interface AdditionalSorter {
        boolean acceptsMember(List<RelationMember> relationMembers, RelationMember m);

        List<RelationMember> sortMembers(List<RelationMember> list);
    }

    private static final Collection<AdditionalSorter> ADDITIONAL_SORTERS = Arrays.asList(
        // first adequate sorter is used, so order matters
        new AssociatedStreetRoleStreetSorter(),
        new AssociatedStreetRoleAddressHouseSorter(),
        new PublicTransportRoleStopPlatformSorter(),
        new FromViaToSorter()
    );

    /**
     * Class that sorts the {@code street} members of
     * {@code type=associatedStreet} and {@code type=street} relations.
     */
    private static final class AssociatedStreetRoleStreetSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(List<RelationMember> relationMembers, RelationMember m) {
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
    private static final class AssociatedStreetRoleAddressHouseSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(List<RelationMember> relationMembers, RelationMember m) {
            return m.hasRole("address", "house");
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            list.sort((a, b) -> {
                final int houseNumber = AlphanumComparator.getInstance().compare(
                        a.getMember().get("addr:housenumber"),
                        b.getMember().get("addr:housenumber"));
                if (houseNumber != 0) {
                    return houseNumber;
                }
                final String aDisplayName = a.getMember().getDisplayName(DefaultNameFormatter.getInstance());
                final String bDisplayName = b.getMember().getDisplayName(DefaultNameFormatter.getInstance());
                return AlphanumComparator.getInstance().compare(aDisplayName, bDisplayName);
            });
            return list;
        }
    }

    /**
     * Class that sorts the {@code platform} and {@code stop} members of
     * {@code type=public_transport} relations.
     */
    private static final class PublicTransportRoleStopPlatformSorter implements AdditionalSorter {

        @Override
        public boolean acceptsMember(List<RelationMember> relationMembers, RelationMember m) {
            return m.getRole() != null && (m.getRole().startsWith("platform") || m.getRole().startsWith("stop"));
        }

        private static String getStopName(OsmPrimitive p) {
            return p.referrers(Relation.class)
                    .filter(ref -> ref.hasTag("type", "public_transport")
                            && ref.hasTag("public_transport", "stop_area")
                            && ref.getName() != null)
                    .map(Relation::getName)
                    .findFirst()
                    .orElse(p.getName());
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            final Map<String, RelationMember> platformByName = new HashMap<>();
            if (list.stream()
                    .filter(i -> i.getRole().startsWith("platform"))
                    .map(i -> platformByName.put(getStopName(i.getMember()), i))
                    .anyMatch(Objects::nonNull)) {
                // Platform with same name present. Stop to avoid damaging complicated relations.
                // This case can happily be handled differently.
                return list;
            }
            final List<RelationMember> sorted = new ArrayList<>(list.size());
            for (RelationMember i : list) {
                if (i.getRole().startsWith("stop")) {
                    sorted.add(i);
                    final RelationMember platform = platformByName.remove(getStopName(i.getMember()));
                    if (platform != null) {
                        sorted.add(platform);
                    }
                }
            }
            sorted.addAll(platformByName.values());
            return sorted;
        }
    }

    /**
     * Class that sorts the {@code from}, {@code via} and {@code to} members of
     * {@code type=restriction} relations.
     */
    private static final class FromViaToSorter implements AdditionalSorter {

        private static final List<String> ROLES = Arrays.asList("from", "via", "to");

        @Override
        public boolean acceptsMember(List<RelationMember> relationMembers, RelationMember m) {
            return ROLES.contains(m.getRole())
                    && relationMembers.stream().map(RelationMember::getRole).collect(Collectors.toSet()).containsAll(ROLES);
        }

        @Override
        public List<RelationMember> sortMembers(List<RelationMember> list) {
            list.sort(Comparator.comparingInt(m -> ROLES.indexOf(m.getRole())));
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
        List<RelationMember> newMembers = new ArrayList<>();

        // Sort members with custom mechanisms (relation-dependent)
        List<RelationMember> defaultMembers = new ArrayList<>(relationMembers.size());
        // Maps sorter to assigned members for sorting. Use LinkedHashMap to retain order.
        Map<AdditionalSorter, List<RelationMember>> customMap = new LinkedHashMap<>();

        // Dispatch members to the first adequate sorter
        for (RelationMember m : relationMembers) {
            boolean wasAdded = false;
            for (AdditionalSorter sorter : ADDITIONAL_SORTERS) {
                if (sorter.acceptsMember(relationMembers, m)) {
                    wasAdded = customMap.computeIfAbsent(sorter, k -> new LinkedList<>()).add(m);
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

    /**
     * Sorts a list of members by connectivity
     * @param defaultMembers The members to sort
     * @return A sorted list of the same members
     * @since 17862 (signature change, generics)
     */
    public static <T extends IRelationMember<? extends IPrimitive>> List<T> sortMembersByConnectivity(List<T> defaultMembers) {
        List<T> newMembers;

        RelationNodeMap<T> map = new RelationNodeMap<>(defaultMembers);
        // List of groups of linked members
        //
        List<LinkedList<Integer>> allGroups = new ArrayList<>();

        // current group of members that are linked among each other
        // Two successive members are always linked i.e. have a common node.
        //
        LinkedList<Integer> group;

        Integer first;
        while ((first = map.pop()) != null) {
            group = new LinkedList<>();
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

        newMembers = allGroups.stream().flatMap(Collection::stream).map(defaultMembers::get).collect(Collectors.toList());

        // Finally, add members that have not been sorted at all
        for (Integer i : map.getNotSortableMembers()) {
            newMembers.add(defaultMembers.get(i));
        }

        return newMembers;
    }

}
