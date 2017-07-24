// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement.placement;

import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.MapViewPath.PathSegmentConsumer;
import org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation;

/**
 * Places the label onto the line.
 *
 * @author Michael Zangl
 * @since 11722
 * @since 11748 moved to own file
 */
public class OnLineStrategy implements PositionForAreaStrategy {
    /**
     * An instance of this class.
     */
    public static final OnLineStrategy INSTANCE = new OnLineStrategy(0);

    private final double yOffset;

    /**
     * Create a new strategy that places the text on the line.
     * @param yOffset The offset sidewards to the line.
     */
    public OnLineStrategy(double yOffset) {
        this.yOffset = yOffset;
    }

    @Override
    public MapViewPositionAndRotation findLabelPlacement(MapViewPath path, Rectangle2D nb) {
        return findOptimalWayPosition(nb, path).map(best -> {
            MapViewPoint center = best.start.interpolate(best.end, .5);
            double theta = upsideTheta(best);
            MapViewPoint moved = center.getMapViewState().getForView(
                    center.getInViewX() - Math.sin(theta) * yOffset,
                    center.getInViewY() + Math.cos(theta) * yOffset);
            return new MapViewPositionAndRotation(moved, theta);
        }).orElse(null);
    }

    private static double upsideTheta(HalfSegment best) {
        double theta = theta(best.start, best.end);
        if (theta < -Math.PI / 2) {
            return theta + Math.PI;
        } else if (theta > Math.PI / 2) {
            return theta - Math.PI;
        } else {
            return theta;
        }
    }

    @Override
    public boolean supportsGlyphVector() {
        return true;
    }

    @Override
    public List<GlyphVector> generateGlyphVectors(MapViewPath path, Rectangle2D nb, List<GlyphVector> gvs,
            boolean isDoubleTranslationBug) {
        // Find the position on the way the font should be placed.
        // If none is found, use the middle of the way.
        double middleOffset = findOptimalWayPosition(nb, path).map(segment -> segment.offset)
                .orElse(path.getLength() / 2);

        // Check that segment of the way. Compute in which direction the text should be rendered.
        // It is rendered in a way that ensures that at least 50% of the text are rotated with the right side up.
        UpsideComputingVisitor upside = new UpsideComputingVisitor(middleOffset - nb.getWidth() / 2,
                middleOffset + nb.getWidth() / 2);
        path.visitLine(upside);
        boolean doRotateText = upside.shouldRotateText();

        // Compute the list of glyphs to draw, along with their offset on the current line.
        List<OffsetGlyph> offsetGlyphs = computeOffsetGlyphs(gvs,
                middleOffset + (doRotateText ? 1 : -1) * nb.getWidth() / 2, doRotateText);

        // Order the glyphs along the line to ensure that they are drawn corretly.
        Collections.sort(offsetGlyphs, Comparator.comparing(OffsetGlyph::getOffset));

        // Now translate all glyphs. This will modify the glyphs stored in gvs.
        path.visitLine(new GlyphRotatingVisitor(offsetGlyphs, isDoubleTranslationBug));
        return gvs;
    }

    /**
     * Create a list of glyphs with an offset along the way
     * @param gvs The list of glyphs
     * @param startOffset The offset in the line
     * @param rotateText Rotate the text by 180°
     * @return The list of glyphs.
     */
    private static List<OffsetGlyph> computeOffsetGlyphs(List<GlyphVector> gvs, double startOffset, boolean rotateText) {
        double offset = startOffset;
        ArrayList<OffsetGlyph> offsetGlyphs = new ArrayList<>();
        for (GlyphVector gv : gvs) {
            double gvOffset = offset;
            IntStream.range(0, gv.getNumGlyphs())
                    .mapToObj(i -> new OffsetGlyph(gvOffset, rotateText, gv, i))
                    .forEach(offsetGlyphs::add);
            offset += (rotateText ? -1 : 1) + gv.getLogicalBounds().getBounds2D().getWidth();
        }
        return offsetGlyphs;
    }

    private static Optional<HalfSegment> findOptimalWayPosition(Rectangle2D rect, MapViewPath path) {
        // find half segments that are long enough to draw text on (don't draw text over the cross hair in the center of each segment)
        List<HalfSegment> longHalfSegment = new ArrayList<>();
        double minSegmentLength = 2 * (rect.getWidth() + 4);
        double length = path.visitLine((inLineOffset, start, end, startIsOldEnd) -> {
            double segmentLength = start.distanceToInView(end);
            if (segmentLength > minSegmentLength) {
                MapViewPoint center = start.interpolate(end, .5);
                double q = computeQuality(start, center);
                // prefer the first one for quality equality.
                longHalfSegment.add(new HalfSegment(start, center, q + .1, inLineOffset + .25 * segmentLength));

                q = computeQuality(center, end);
                longHalfSegment.add(new HalfSegment(center, end, q, inLineOffset + .75 * segmentLength));
            }
        });

        // find the segment with the best quality. If there are several with best quality, the one close to the center is prefered.
        return longHalfSegment.stream().max(
                Comparator.comparingDouble(segment -> segment.quality - 1e-5 * Math.abs(segment.offset - length / 2)));
    }

    private static double computeQuality(MapViewPoint p1, MapViewPoint p2) {
        double q = 0;
        if (p1.isInView()) {
            q += 1;
        }
        if (p2.isInView()) {
            q += 1;
        }
        return q;
    }

    /**
     * A half segment that can be used to place text on it. Used in the drawTextOnPath algorithm.
     * @author Michael Zangl
     */
    private static class HalfSegment {
        /**
         * start point of half segment
         */
        private final MapViewPoint start;

        /**
         * end point of half segment
         */
        private final MapViewPoint end;

        /**
         * quality factor (off screen / partly on screen / fully on screen)
         */
        private final double quality;

        /**
         * The offset in the path.
         */
        private final double offset;

        /**
         * Create a new half segment
         * @param start The start along the way
         * @param end The end of the segment
         * @param quality A quality factor.
         * @param offset The offset in the path.
         */
        HalfSegment(MapViewPoint start, MapViewPoint end, double quality, double offset) {
            super();
            this.start = start;
            this.end = end;
            this.quality = quality;
            this.offset = offset;
        }

        @Override
        public String toString() {
            return "HalfSegment [start=" + start + ", end=" + end + ", quality=" + quality + ']';
        }
    }

    /**
     * A visitor that computes the side of the way that is the upper one for each segment and computes the dominant upper side of the way.
     * This is used to always place at least 50% of the text correctly.
     */
    private static class UpsideComputingVisitor implements PathSegmentConsumer {

        private final double startOffset;
        private final double endOffset;

        private double upsideUpLines;
        private double upsideDownLines;

        UpsideComputingVisitor(double startOffset, double endOffset) {
            super();
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Override
        public void addLineBetween(double inLineOffset, MapViewPoint start, MapViewPoint end, boolean startIsOldEnd) {
            if (inLineOffset > endOffset) {
                return;
            }
            double length = start.distanceToInView(end);
            if (inLineOffset + length < startOffset) {
                return;
            }

            double segmentStart = Math.max(inLineOffset, startOffset);
            double segmentEnd = Math.min(inLineOffset + length, endOffset);

            double segmentLength = segmentEnd - segmentStart;

            if (start.getInViewX() < end.getInViewX()) {
                upsideUpLines += segmentLength;
            } else {
                upsideDownLines += segmentLength;
            }
        }

        /**
         * Check if the text should be rotated by 180°
         * @return if the text should be rotated.
         */
        boolean shouldRotateText() {
            return upsideUpLines < upsideDownLines;
        }
    }

    /**
     * Rotate the glyphs along a path.
     */
    private class GlyphRotatingVisitor implements PathSegmentConsumer {
        private final Iterator<OffsetGlyph> gvs;
        private final boolean isDoubleTranslationBug;
        private OffsetGlyph next;

        /**
         * Create a new {@link GlyphRotatingVisitor}
         * @param gvs The glyphs to draw. Sorted along the line
         * @param isDoubleTranslationBug true to fix a double translation bug.
         */
        GlyphRotatingVisitor(List<OffsetGlyph> gvs, boolean isDoubleTranslationBug) {
            this.isDoubleTranslationBug = isDoubleTranslationBug;
            this.gvs = gvs.iterator();
            takeNext();
            while (next != null && next.offset < 0) {
                // skip them
                takeNext();
            }
        }

        private void takeNext() {
            if (gvs.hasNext()) {
                next = gvs.next();
            } else {
                next = null;
            }
        }

        @Override
        public void addLineBetween(double inLineOffset, MapViewPoint start, MapViewPoint end, boolean startIsOldEnd) {
            double segLength = start.distanceToInView(end);
            double segEnd = inLineOffset + segLength;
            double theta = theta(start, end);
            while (next != null && next.offset < segEnd) {
                Rectangle2D rect = next.getBounds();
                double centerY = 0;
                MapViewPoint p = start.interpolate(end, (next.offset - inLineOffset) / segLength);

                AffineTransform trfm = new AffineTransform();
                trfm.translate(-rect.getCenterX(), -centerY);
                trfm.translate(p.getInViewX(), p.getInViewY());
                trfm.rotate(theta + next.preRotate, rect.getWidth() / 2, centerY);
                trfm.translate(0, next.glyph.getFont().getSize2D() * .25);
                trfm.translate(0, yOffset);
                if (isDoubleTranslationBug) {
                    // scale the translation components by one half
                    AffineTransform tmp = AffineTransform.getTranslateInstance(-0.5 * trfm.getTranslateX(),
                            -0.5 * trfm.getTranslateY());
                    tmp.concatenate(trfm);
                    trfm = tmp;
                }
                next.glyph.setGlyphTransform(next.glyphIndex, trfm);
                takeNext();
            }
        }
    }

    private static class OffsetGlyph {
        private final double offset;
        private final double preRotate;
        private final GlyphVector glyph;
        private final int glyphIndex;

        OffsetGlyph(double offset, boolean rotateText, GlyphVector glyph, int glyphIndex) {
            super();
            this.preRotate = rotateText ? Math.PI : 0;
            this.glyph = glyph;
            this.glyphIndex = glyphIndex;
            Rectangle2D rect = getBounds();
            this.offset = offset + (rotateText ? -1 : 1) * (rect.getX() + rect.getWidth() / 2);
        }

        Rectangle2D getBounds() {
            return glyph.getGlyphLogicalBounds(glyphIndex).getBounds2D();
        }

        double getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return "OffsetGlyph [offset=" + offset + ", preRotate=" + preRotate + ", glyphIndex=" + glyphIndex + ']';
        }
    }

    private static double theta(MapViewPoint start, MapViewPoint end) {
        return Math.atan2(end.getInViewY() - start.getInViewY(), end.getInViewX() - start.getInViewX());
    }

    @Override
    public PositionForAreaStrategy withAddedOffset(Point2D addToOffset) {
        if (Math.abs(addToOffset.getY()) < 1e-5) {
            return this;
        } else {
            return new OnLineStrategy(this.yOffset - addToOffset.getY());
        }
    }

    @Override
    public String toString() {
        return "OnLineStrategy [yOffset=" + yOffset + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(yOffset);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OnLineStrategy other = (OnLineStrategy) obj;
        return Double.doubleToLongBits(yOffset) == Double.doubleToLongBits(other.yOffset);
    }
}
