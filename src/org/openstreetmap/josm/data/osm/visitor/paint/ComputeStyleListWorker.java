// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer.StyleRecord;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaIconElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextElement;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * Helper to compute style list.
 * @since 11914 (extracted from StyledMapRenderer)
 */
public class ComputeStyleListWorker extends RecursiveTask<List<StyleRecord>> implements PrimitiveVisitor {

    private static final long serialVersionUID = 1L;

    private final transient List<? extends IPrimitive> input;
    private final transient List<StyleRecord> output;

    private final transient ElemStyles styles;
    private final int directExecutionTaskSize;
    private final double circum;
    private final NavigatableComponent nc;

    private final boolean drawArea;
    private final boolean drawMultipolygon;
    private final boolean drawRestriction;

    /**
     * Constructs a new {@code ComputeStyleListWorker}.
     * @param circum distance on the map in meters that 100 screen pixels represent
     * @param nc navigatable component
     * @param input the primitives to process
     * @param output the list of styles to which styles will be added
     * @param directExecutionTaskSize the threshold deciding whether to subdivide the tasks
     * @since 13810 (signature)
     */
    ComputeStyleListWorker(double circum, NavigatableComponent nc,
            final List<? extends IPrimitive> input, List<StyleRecord> output, int directExecutionTaskSize) {
        this(circum, nc, input, output, directExecutionTaskSize, MapPaintStyles.getStyles());
    }

    /**
     * Constructs a new {@code ComputeStyleListWorker}.
     * @param circum distance on the map in meters that 100 screen pixels represent
     * @param nc navigatable component
     * @param input the primitives to process
     * @param output the list of styles to which styles will be added
     * @param directExecutionTaskSize the threshold deciding whether to subdivide the tasks
     * @param styles the {@link ElemStyles} instance used to generate primitive {@link StyleElement}s.
     * @since 12964
     * @since 13810 (signature)
     */
    ComputeStyleListWorker(double circum, NavigatableComponent nc,
            final List<? extends IPrimitive> input, List<StyleRecord> output, int directExecutionTaskSize,
            ElemStyles styles) {
        this.circum = circum;
        this.nc = nc;
        this.input = input;
        this.output = output;
        this.directExecutionTaskSize = directExecutionTaskSize;
        this.styles = styles;
        this.drawArea = circum <= Config.getPref().getInt("mappaint.fillareas", 10_000_000);
        this.drawMultipolygon = drawArea && Config.getPref().getBoolean("mappaint.multipolygon", true);
        this.drawRestriction = Config.getPref().getBoolean("mappaint.restriction", true);
        this.styles.setDrawMultipolygon(drawMultipolygon);
    }

    @Override
    protected List<StyleRecord> compute() {
        if (input.size() <= directExecutionTaskSize) {
            return computeDirectly();
        } else {
            final Collection<ForkJoinTask<List<StyleRecord>>> tasks = new ArrayList<>();
            for (int fromIndex = 0; fromIndex < input.size(); fromIndex += directExecutionTaskSize) {
                final int toIndex = Math.min(fromIndex + directExecutionTaskSize, input.size());
                tasks.add(new ComputeStyleListWorker(circum, nc, input.subList(fromIndex, toIndex),
                        new ArrayList<>(directExecutionTaskSize), directExecutionTaskSize, styles).fork());
            }
            for (ForkJoinTask<List<StyleRecord>> task : tasks) {
                output.addAll(task.join());
            }
            return output;
        }
    }

    /**
     * Compute directly (without using fork/join) the style list. Only called for small input.
     * @return list of computed style records
     */
    public List<StyleRecord> computeDirectly() {
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            for (final IPrimitive osm : input) {
                acceptDrawable(osm);
            }
            return output;
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("input-size", input.size()).put("output-size", output.size());
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
    }

    private void acceptDrawable(final IPrimitive osm) {
        try {
            if (osm.isDrawable()) {
                osm.accept(this);
            }
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e).put("osm", osm);
        }
    }

    @Override
    public void visit(INode n) {
        add(n, StyledMapRenderer.computeFlags(n, false));
    }

    @Override
    public void visit(IWay<?> w) {
        add(w, StyledMapRenderer.computeFlags(w, true));
    }

    @Override
    public void visit(IRelation<?> r) {
        add(r, StyledMapRenderer.computeFlags(r, true));
    }

    /**
     * Add new style records for the given node.
     * @param osm node
     * @param flags flags
     * @since 13810 (signature)
     */
    public void add(INode osm, int flags) {
        StyleElementList sl = styles.get(osm, circum, nc);
        for (StyleElement s : sl) {
            output.add(new StyleRecord(s, osm, flags));
        }
    }

    /**
     * Add new style records for the given way.
     * @param osm way
     * @param flags flags
     * @since 13810 (signature)
     */
    public void add(IWay<?> osm, int flags) {
        StyleElementList sl = styles.get(osm, circum, nc);
        for (StyleElement s : sl) {
            if ((drawArea && (flags & StyledMapRenderer.FLAG_DISABLED) == 0) || !(s instanceof AreaElement)) {
                output.add(new StyleRecord(s, osm, flags));
            }
        }
    }

    /**
     * Add new style records for the given relation.
     * @param osm relation
     * @param flags flags
     * @since 13810 (signature)
     */
    public void add(IRelation<?> osm, int flags) {
        StyleElementList sl = styles.get(osm, circum, nc);
        for (StyleElement s : sl) {
            if (drawAreaElement(flags, s) ||
               (drawMultipolygon && drawArea && s instanceof TextElement) ||
               (drawRestriction && s instanceof NodeElement)) {
                output.add(new StyleRecord(s, osm, flags));
            }
        }
    }

    private boolean drawAreaElement(int flags, StyleElement s) {
        return drawMultipolygon && drawArea && (s instanceof AreaElement || s instanceof AreaIconElement)
                && (flags & StyledMapRenderer.FLAG_DISABLED) == 0;
    }
}
