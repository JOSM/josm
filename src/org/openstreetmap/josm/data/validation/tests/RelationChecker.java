// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Roles;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPresets;
import org.openstreetmap.josm.tools.Utils;

/**
 * Check for wrong relations.
 * @since 3669
 */
public class RelationChecker extends Test {

    protected static final int ROLE_UNKNOWN      = 1701;
    protected static final int ROLE_EMPTY        = 1702;
    protected static final int WRONG_TYPE        = 1703;
    protected static final int HIGH_COUNT        = 1704;
    protected static final int LOW_COUNT         = 1705;
    protected static final int ROLE_MISSING      = 1706;
    protected static final int RELATION_UNKNOWN  = 1707;
    protected static final int RELATION_EMPTY    = 1708;

    /**
     * Error message used to group errors related to role problems.
     * @since 6731
     */
    public static final String ROLE_VERIF_PROBLEM_MSG = tr("Role verification problem");

    /**
     * Constructor
     */
    public RelationChecker() {
        super(tr("Relation checker"),
                tr("Checks for errors in relations."));
    }

    @Override
    public void initialize() {
        initializePresets();
    }

    private static Collection<TaggingPreset> relationpresets = new LinkedList<>();

    /**
     * Reads the presets data.
     */
    public static synchronized void initializePresets() {
        if (!relationpresets.isEmpty()) {
            // the presets have already been initialized
            return;
        }
        for (TaggingPreset p : TaggingPresets.getTaggingPresets()) {
            for (TaggingPresetItem i : p.data) {
                if (i instanceof Roles) {
                    relationpresets.add(p);
                    break;
                }
            }
        }
    }

    private static class RolePreset {
        private final List<Role> roles;
        private final String name;

        RolePreset(List<Role> roles, String name) {
            this.roles = roles;
            this.name = name;
        }
    }

    private static class RoleInfo {
        private int total;
    }

    @Override
    public void visit(Relation n) {
        Map<String, RolePreset> allroles = buildAllRoles(n);
        if (allroles.isEmpty() && n.hasTag("type", "route")
                && n.hasTag("route", "train", "subway", "monorail", "tram", "bus", "trolleybus", "aerialway", "ferry")) {
            errors.add(new TestError(this, Severity.WARNING,
                    tr("Route scheme is unspecified. Add {0} ({1}=public_transport; {2}=legacy)", "public_transport:version", "2", "1"),
                    RELATION_UNKNOWN, n));
        } else if (allroles.isEmpty()) {
            errors.add(new TestError(this, Severity.WARNING, tr("Relation type is unknown"), RELATION_UNKNOWN, n));
        }

        Map<String, RoleInfo> map = buildRoleInfoMap(n);
        if (map.isEmpty()) {
            errors.add(new TestError(this, Severity.ERROR, tr("Relation is empty"), RELATION_EMPTY, n));
        } else if (!allroles.isEmpty()) {
            checkRoles(n, allroles, map);
        }
    }

    private Map<String, RoleInfo> buildRoleInfoMap(Relation n) {
        Map<String, RoleInfo> map = new HashMap<>();
        for (RelationMember m : n.getMembers()) {
            String role = m.getRole();
            RoleInfo ri = map.get(role);
            if (ri == null) {
                ri = new RoleInfo();
                map.put(role, ri);
            }
            ri.total++;
        }
        return map;
    }

    // return Roles grouped by key
    private Map<String, RolePreset> buildAllRoles(Relation n) {
        Map<String, RolePreset> allroles = new HashMap<>();

        for (TaggingPreset p : relationpresets) {
            boolean matches = true;
            Roles r = null;
            for (TaggingPresetItem i : p.data) {
                if (i instanceof Key) {
                    Key k = (Key) i;
                    if (!k.value.equals(n.get(k.key))) {
                        matches = false;
                        break;
                    }
                } else if (i instanceof Roles) {
                    r = (Roles) i;
                }
            }
            if (matches && r != null) {
                for (Role role: r.roles) {
                    String key = role.key;
                    List<Role> roleGroup = null;
                    if (allroles.containsKey(key)) {
                        roleGroup = allroles.get(key).roles;
                    } else {
                        roleGroup = new LinkedList<>();
                        allroles.put(key, new RolePreset(roleGroup, p.name));
                    }
                    roleGroup.add(role);
                }
            }
        }
        return allroles;
    }

    private boolean checkMemberType(Role r, RelationMember member) {
        if (r.types != null) {
            switch (member.getDisplayType()) {
            case NODE:
                return r.types.contains(TaggingPresetType.NODE);
            case CLOSEDWAY:
                return r.types.contains(TaggingPresetType.CLOSEDWAY);
            case WAY:
                return r.types.contains(TaggingPresetType.WAY);
            case MULTIPOLYGON:
            case RELATION:
                return r.types.contains(TaggingPresetType.RELATION);
            default: // not matching type
                return false;
            }
        } else {
            // if no types specified, then test is passed
            return true;
        }
    }

    /**
     * get all role definition for specified key and check, if some definition matches
     *
     * @param rolePreset containing preset for role of the member
     * @param member to be verified
     * @param n relation to be verified
     * @return <tt>true</tt> if member passed any of definition within preset
     *
     */
    private boolean checkMemberExpressionAndType(RolePreset rolePreset, RelationMember member, Relation n) {
        TestError possibleMatchError = null;
        if (rolePreset == null || rolePreset.roles == null) {
            // no restrictions on role types
            return true;
        }
        // iterate through all of the role definition within preset
        // and look for any matching definition
        for (Role r: rolePreset.roles) {
            if (checkMemberType(r, member)) {
                // member type accepted by role definition
                if (r.memberExpression == null) {
                    // no member expression - so all requirements met
                    return true;
                } else {
                    // verify if preset accepts such member
                    OsmPrimitive primitive = member.getMember();
                    if (!primitive.isUsable()) {
                        // if member is not usable (i.e. not present in working set)
                        // we can't verify expression - so we just skip it
                        return true;
                    } else {
                        // verify expression
                        if (r.memberExpression.match(primitive)) {
                            return true;
                        } else {
                            // possible match error
                            // we still need to iterate further, as we might have
                            // different present, for which memberExpression will match
                            // but stash the error in case no better reason will be found later
                            String s = marktr("Role member does not match expression {0} in template {1}");
                            possibleMatchError = new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                                    tr(s, r.memberExpression, rolePreset.name), s, WRONG_TYPE,
                                    member.getMember().isUsable() ? member.getMember() : n);

                        }
                    }
                }
            }
        }

        if (possibleMatchError != null) {
            // if any error found, then assume that member type was correct
            // and complain about not matching the memberExpression
            // (the only failure, that we could gather)
            errors.add(possibleMatchError);
        } else {
            // no errors found till now. So member at least failed at matching the type
            // it could also fail at memberExpression, but we can't guess at which
            String s = marktr("Role member type {0} does not match accepted list of {1} in template {2}");

            // prepare Set of all accepted types in template
            Collection<TaggingPresetType> types = EnumSet.noneOf(TaggingPresetType.class);
            for (Role r: rolePreset.roles) {
                types.addAll(r.types);
            }

            // convert in localization friendly way to string of accepted types
            String typesStr = Utils.join("/", Utils.transform(types, new Utils.Function<TaggingPresetType, Object>() {
                public Object apply(TaggingPresetType x) {
                    return tr(x.getName());
                }
            }));

            errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                    tr(s, member.getType(), typesStr, rolePreset.name), s, WRONG_TYPE,
                    member.getMember().isUsable() ? member.getMember() : n));
        }
        return false;
    }

    /**
     *
     * @param n relation to validate
     * @param allroles contains presets for specified relation
     * @param map contains statistics of occurances of specified role types in relation
     */
    private void checkRoles(Relation n, Map<String, RolePreset> allroles, Map<String, RoleInfo> map) {
        // go through all members of relation
        for (RelationMember member: n.getMembers()) {
            String role = member.getRole();

            // error reporting done inside
            checkMemberExpressionAndType(allroles.get(role), member, n);
        }

        // verify role counts based on whole role sets
        for (RolePreset rp: allroles.values()) {
            for (Role r: rp.roles) {
                String keyname = r.key;
                if (keyname.isEmpty()) {
                    keyname = tr("<empty>");
                }
                checkRoleCounts(n, r, keyname, map.get(r.key));
            }
        }
        // verify unwanted members
        for (String key : map.keySet()) {
            if (!allroles.containsKey(key)) {
                String templates = Utils.join("/", Utils.transform(allroles.keySet(), new Utils.Function<String, Object>() {
                    public Object apply(String x) {
                        return tr(x);
                    }
                }));

                if (!key.isEmpty()) {
                    String s = marktr("Role {0} unknown in templates {1}");

                    errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                            tr(s, key, templates), MessageFormat.format(s, key), ROLE_UNKNOWN, n));
                } else {
                    String s = marktr("Empty role type found when expecting one of {0}");
                    errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                            tr(s, templates), s, ROLE_EMPTY, n));
                }
            }
        }
    }

    private void checkRoleCounts(Relation n, Role r, String keyname, RoleInfo ri) {
        long count = (ri == null) ? 0 : ri.total;
        long vc = r.getValidCount(count);
        if (count != vc) {
            if (count == 0) {
                String s = marktr("Role {0} missing");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname), MessageFormat.format(s, keyname), ROLE_MISSING, n));
            } else if (vc > count) {
                String s = marktr("Number of {0} roles too low ({1})");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname, count), MessageFormat.format(s, keyname, count), LOW_COUNT, n));
            } else {
                String s = marktr("Number of {0} roles too high ({1})");
                errors.add(new TestError(this, Severity.WARNING, ROLE_VERIF_PROBLEM_MSG,
                        tr(s, keyname, count), MessageFormat.format(s, keyname, count), HIGH_COUNT, n));
            }
        }
    }

    @Override
    public Command fixError(TestError testError) {
        if (isFixable(testError)) {
            return new DeleteCommand(testError.getPrimitives());
        }
        return null;
    }

    @Override
    public boolean isFixable(TestError testError) {
        Collection<? extends OsmPrimitive> primitives = testError.getPrimitives();
        return testError.getCode() == RELATION_EMPTY && !primitives.isEmpty() && primitives.iterator().next().isNew();
    }
}
