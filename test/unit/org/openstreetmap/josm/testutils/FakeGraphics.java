// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A fake Graphics to be used in headless mode.
 */
public final class FakeGraphics extends Graphics2D {
    // This is needed just in case someone wants to fake paint something
    private Rectangle bounds;

    @Override
    public void setXORMode(Color c1) {
    }

    @Override
    public void setPaintMode() {
    }

    @Override
    public void setFont(Font font) {
    }

    @Override
    public void setColor(Color c) {
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    @Override
    public void setClip(Shape clip) {
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return new Canvas().getFontMetrics(getFont());
    }

    @Override
    public Font getFont() {
        return new Font(null, Font.PLAIN, 0);
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public Rectangle getClipBounds() {
        return this.bounds;
    }

    @Override
    public Shape getClip() {
        return null;
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
    }

    // CHECKSTYLE.OFF: ParameterNumber
    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            Color bgcolor, ImageObserver observer) {
        return false;
    }
    // CHECKSTYLE.ON: ParameterNumber

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public Graphics create() {
        return this;
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
    }

    @Override
    public void translate(double tx, double ty) {
    }

    @Override
    public void translate(int x, int y) {
    }

    @Override
    public void transform(AffineTransform Tx) {
    }

    @Override
    public void shear(double shx, double shy) {
    }

    @Override
    public void setTransform(AffineTransform Tx) {
    }

    @Override
    public void setStroke(Stroke s) {
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
    }

    @Override
    public void setRenderingHint(Key hintKey, Object hintValue) {
    }

    @Override
    public void setPaint(Paint paint) {
    }

    @Override
    public void setComposite(Composite comp) {
    }

    @Override
    public void setBackground(Color color) {
    }

    @Override
    public void scale(double sx, double sy) {
    }

    @Override
    public void rotate(double theta, double x, double y) {
    }

    @Override
    public void rotate(double theta) {
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return false;
    }

    @Override
    public AffineTransform getTransform() {
        return null;
    }

    @Override
    public Stroke getStroke() {
        return null;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    @Override
    public Object getRenderingHint(Key hintKey) {
        return null;
    }

    @Override
    public Paint getPaint() {
        return null;
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return new FontRenderContext(null, false, false);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return null;
    }

    @Override
    public Composite getComposite() {
        return null;
    }

    @Override
    public Color getBackground() {
        return null;
    }

    @Override
    public void fill(Shape s) {
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    }

    @Override
    public void drawString(String str, float x, float y) {
    }

    @Override
    public void drawString(String str, int x, int y) {
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return false;
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
    }

    @Override
    public void draw(Shape s) {
    }

    @Override
    public void clip(Shape s) {
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
    }
}
