// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.And;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Child;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Not;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Or;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Parent;

/**
 * The context switch offers possibility to use tags of referenced primitive when constructing primitive name.
 * @author jttt
 * @since 4546
 */
public class ContextSwitchTemplate implements TemplateEntry {

    private static final TemplateEngineDataProvider EMPTY_PROVIDER = new TemplateEngineDataProvider() {
        @Override
        public Object getTemplateValue(String name, boolean special) {
            return null;
        }

        @Override
        public Collection<String> getTemplateKeys() {
            return Collections.emptyList();
        }

        @Override
        public boolean evaluateCondition(Match condition) {
            return false;
        }
    };

    private abstract static class ContextProvider extends Match {
        protected Match condition;

        abstract List<OsmPrimitive> getPrimitives(OsmPrimitive root);
    }

    private static class ParentSet extends ContextProvider {
        private final Match childCondition;

        ParentSet(Match child) {
            this.childCondition = child;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            throw new UnsupportedOperationException();
        }

        @Override
        List<OsmPrimitive> getPrimitives(OsmPrimitive root) {
            List<OsmPrimitive> children;
            if (childCondition instanceof ContextProvider) {
                children = ((ContextProvider) childCondition).getPrimitives(root);
            } else if (childCondition.match(root)) {
                children = Collections.singletonList(root);
            } else {
                children = Collections.emptyList();
            }

            List<OsmPrimitive> result = new ArrayList<>();
            for (OsmPrimitive child: children) {
                for (OsmPrimitive parent: child.getReferrers(true)) {
                    if (condition == null || condition.match(parent)) {
                        result.add(parent);
                    }
                }
            }
            return result;
        }
    }

    private static class ChildSet extends ContextProvider {
        private final Match parentCondition;

        ChildSet(Match parentCondition) {
            this.parentCondition = parentCondition;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            throw new UnsupportedOperationException();
        }

        @Override
        List<OsmPrimitive> getPrimitives(OsmPrimitive root) {
            List<OsmPrimitive> parents;
            if (parentCondition instanceof ContextProvider) {
                parents = ((ContextProvider) parentCondition).getPrimitives(root);
            } else if (parentCondition.match(root)) {
                parents = Collections.singletonList(root);
            } else {
                parents = Collections.emptyList();
            }
            List<OsmPrimitive> result = new ArrayList<>();
            for (OsmPrimitive p: parents) {
                if (p instanceof Way) {
                    for (Node n: ((Way) p).getNodes()) {
                        if (condition != null && condition.match(n)) {
                            result.add(n);
                        }
                        result.add(n);
                    }
                } else if (p instanceof Relation) {
                    for (RelationMember rm: ((Relation) p).getMembers()) {
                        if (condition != null && condition.match(rm.getMember())) {
                            result.add(rm.getMember());
                        }
                    }
                }
            }
            return result;
        }
    }

    private static class OrSet extends ContextProvider {
        private final ContextProvider lhs;
        private final ContextProvider rhs;

        OrSet(ContextProvider lhs, ContextProvider rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            throw new UnsupportedOperationException();
        }

        @Override
        List<OsmPrimitive> getPrimitives(OsmPrimitive root) {
            List<OsmPrimitive> result = new ArrayList<>();
            for (OsmPrimitive o: lhs.getPrimitives(root)) {
                if (condition == null || condition.match(o)) {
                    result.add(o);
                }
            }
            for (OsmPrimitive o: rhs.getPrimitives(root)) {
                if (condition == null || (condition.match(o) && !result.contains(o))) {
                    result.add(o);
                }
            }
            return result;
        }
    }

    private static class AndSet extends ContextProvider {
        private final ContextProvider lhs;
        private final ContextProvider rhs;

        AndSet(ContextProvider lhs, ContextProvider rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            throw new UnsupportedOperationException();
        }

        @Override
        List<OsmPrimitive> getPrimitives(OsmPrimitive root) {
            List<OsmPrimitive> result = new ArrayList<>();
            List<OsmPrimitive> lhsList = lhs.getPrimitives(root);
            for (OsmPrimitive o: rhs.getPrimitives(root)) {
                if (lhsList.contains(o) && (condition == null || condition.match(o))) {
                    result.add(o);
                }
            }
            return result;
        }
    }

    private final ContextProvider context;
    private final TemplateEntry template;

    private static Match transform(Match m, int searchExpressionPosition) throws ParseError {
        if (m instanceof Parent) {
            Match child = transform(((Parent) m).getOperand(), searchExpressionPosition);
            return new ParentSet(child);
        } else if (m instanceof Child) {
            Match parent = transform(((Child) m).getOperand(), searchExpressionPosition);
            return new ChildSet(parent);
        } else if (m instanceof And) {
            Match lhs = transform(((And) m).getLhs(), searchExpressionPosition);
            Match rhs = transform(((And) m).getRhs(), searchExpressionPosition);

            if (lhs instanceof ContextProvider && rhs instanceof ContextProvider)
                return new AndSet((ContextProvider) lhs, (ContextProvider) rhs);
            else if (lhs instanceof ContextProvider) {
                ContextProvider cp = (ContextProvider) lhs;
                if (cp.condition == null) {
                    cp.condition = rhs;
                } else {
                    cp.condition = new And(cp.condition, rhs);
                }
                return cp;
            } else if (rhs instanceof ContextProvider) {
                ContextProvider cp = (ContextProvider) rhs;
                if (cp.condition == null) {
                    cp.condition = lhs;
                } else {
                    cp.condition = new And(lhs, cp.condition);
                }
                return cp;
            } else
                return m;
        } else if (m instanceof Or) {
            Match lhs = transform(((Or) m).getLhs(), searchExpressionPosition);
            Match rhs = transform(((Or) m).getRhs(), searchExpressionPosition);

            if (lhs instanceof ContextProvider && rhs instanceof ContextProvider)
                return new OrSet((ContextProvider) lhs, (ContextProvider) rhs);
            else if (lhs instanceof ContextProvider)
                throw new ParseError(
                        tr("Error in search expression on position {0} - right side of or(|) expression must return set of primitives",
                                searchExpressionPosition));
            else if (rhs instanceof ContextProvider)
                throw new ParseError(
                        tr("Error in search expression on position {0} - left side of or(|) expression must return set of primitives",
                                searchExpressionPosition));
            else
                return m;
        } else if (m instanceof Not) {
            Match match = transform(((Not) m).getMatch(), searchExpressionPosition);
            if (match instanceof ContextProvider)
                throw new ParseError(
                        tr("Error in search expression on position {0} - not(-) cannot be used in this context",
                                searchExpressionPosition));
            else
                return m;
        } else
            return m;
    }

    /**
     * Constructs a new {@code ContextSwitchTemplate}.
     * @param match match
     * @param template template
     * @param searchExpressionPosition search expression position
     * @throws ParseError if a parse error occurs, or if the match transformation returns the same primitive
     */
    public ContextSwitchTemplate(Match match, TemplateEntry template, int searchExpressionPosition) throws ParseError {
        Match m = transform(match, searchExpressionPosition);
        if (!(m instanceof ContextProvider))
            throw new ParseError(
                    tr("Error in search expression on position {0} - expression must return different then current primitive",
                            searchExpressionPosition));
        else {
            context = (ContextProvider) m;
        }
        this.template = template;
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        if (dataProvider instanceof OsmPrimitive) {
            List<OsmPrimitive> primitives = context.getPrimitives((OsmPrimitive) dataProvider);
            if (primitives != null && !primitives.isEmpty()) {
                template.appendText(result, primitives.get(0));
            }
        }
        template.appendText(result, EMPTY_PROVIDER);
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        if (dataProvider instanceof OsmPrimitive) {
            List<OsmPrimitive> primitives = context.getPrimitives((OsmPrimitive) dataProvider);
            if (primitives != null && !primitives.isEmpty()) {
                return template.isValid(primitives.get(0));
            }
        }
        return false;
    }
}
