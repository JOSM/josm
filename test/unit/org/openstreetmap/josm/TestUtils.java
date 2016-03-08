// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.fail;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.AbstractProgressMonitor;
import org.openstreetmap.josm.gui.progress.CancelHandler;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressTaskId;
import org.openstreetmap.josm.io.Compression;

/**
 * Various utils, useful for unit tests.
 */
public final class TestUtils {

    private TestUtils() {
        // Hide constructor for utility classes
    }

    /**
     * Returns the path to test data root directory.
     * @return path to test data root directory
     */
    public static String getTestDataRoot() {
        String testDataRoot = System.getProperty("josm.test.data");
        if (testDataRoot == null || testDataRoot.isEmpty()) {
            testDataRoot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testDataRoot + "'");
        }
        return testDataRoot.endsWith("/") ? testDataRoot : testDataRoot + "/";
    }

    /**
     * Gets path to test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @return path to test data directory for given ticket id
     */
    public static String getRegressionDataDir(int ticketid) {
        return TestUtils.getTestDataRoot() + "/regress/" + ticketid;
    }

    /**
     * Gets path to given file in test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @param filename File name
     * @return path to given file in test data directory for given ticket id
     */
    public static String getRegressionDataFile(int ticketid, String filename) {
        return getRegressionDataDir(ticketid) + '/' + filename;
    }

    /**
     * Gets input stream to given file in test data directory for given ticket id.
     * @param ticketid Ticket numeric identifier
     * @param filename File name
     * @return path to given file in test data directory for given ticket id
     * @throws IOException if any I/O error occurs
     */
    public static InputStream getRegressionDataStream(int ticketid, String filename) throws IOException {
        return Compression.getUncompressedFileInputStream(new File(getRegressionDataDir(ticketid) + '/' + filename));
    }

    /**
     * Checks that the given Comparator respects its contract on the given table.
     * @param <T> type of elements
     * @param comparator The comparator to test
     * @param array The array sorted for test purpose
     */
    public static <T> void checkComparableContract(Comparator<T> comparator, T[] array) {
        System.out.println("Validating Comparable contract on array of "+array.length+" elements");
        // Check each compare possibility
        for (int i = 0; i < array.length; i++) {
            T r1 = array[i];
            for (int j = i; j < array.length; j++) {
                T r2 = array[j];
                int a = comparator.compare(r1, r2);
                int b = comparator.compare(r2, r1);
                if (i == j || a == b) {
                    if (a != 0 || b != 0) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                } else {
                    if (a != -b) {
                        fail(getFailMessage(r1, r2, a, b));
                    }
                }
                for (int k = j; k < array.length; k++) {
                    T r3 = array[k];
                    int c = comparator.compare(r1, r3);
                    int d = comparator.compare(r2, r3);
                    if (a > 0 && d > 0) {
                        if (c <= 0) {
                           fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a == 0 && d == 0) {
                        if (c != 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    } else if (a < 0 && d < 0) {
                        if (c >= 0) {
                            fail(getFailMessage(r1, r2, r3, a, b, c, d));
                        }
                    }
                }
            }
        }
        // Sort relation array
        Arrays.sort(array, comparator);
    }

    private static <T> String getFailMessage(T o1, T o2, int a, int b) {
        return new StringBuilder("Compared\no1: ").append(o1).append("\no2: ")
        .append(o2).append("\ngave: ").append(a).append("/").append(b)
        .toString();
    }

    private static <T> String getFailMessage(T o1, T o2, T o3, int a, int b, int c, int d) {
        return new StringBuilder(getFailMessage(o1, o2, a, b))
        .append("\nCompared\no1: ").append(o1).append("\no3: ").append(o3).append("\ngave: ").append(c)
        .append("\nCompared\no2: ").append(o2).append("\no3: ").append(o3).append("\ngave: ").append(d)
        .toString();
    }

    /**
     * Returns the Java version as an int value.
     * @return the Java version as an int value (7, 8, 9, etc.)
     */
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        // Allow these formats:
        // 1.7.0_91
        // 1.8.0_72-ea
        // 9-ea
        // 9
        // 9.0.1
        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');
        return Integer.parseInt(version.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
    }

    /**
     * Returns an instance of {@link AbstractProgressMonitor} which keeps track of the monitor state,
     * but does not show the progress.
     * @return a progress monitor
     */
    public static ProgressMonitor newTestProgressMonitor() {
        return new AbstractProgressMonitor(new CancelHandler()) {

            @Override
            protected void doBeginTask() {
            }

            @Override
            protected void doFinishTask() {
            }

            @Override
            protected void doSetIntermediate(boolean value) {
            }

            @Override
            protected void doSetTitle(String title) {
            }

            @Override
            protected void doSetCustomText(String title) {
            }

            @Override
            protected void updateProgress(double value) {
            }

            @Override
            public void setProgressTaskId(ProgressTaskId taskId) {
            }

            @Override
            public ProgressTaskId getProgressTaskId() {
                return null;
            }

            @Override
            public Component getWindowParent() {
                return null;
            }
        };
    }

    // CHECKSTYLE.OFF: AnonInnerLength
    // CHECKSTYLE.OFF: MethodLength
    // CHECKSTYLE.OFF: ParameterNumber

    /**
     * Returns an instance of {@link Graphics2D}.
     * @return a mockup graphics instance
     */
    public static Graphics2D newGraphics() {
        return new Graphics2D() {

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
                return new Font(null, 0, 0);
            }

            @Override
            public Color getColor() {
                return null;
            }

            @Override
            public Rectangle getClipBounds() {
                return null;
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

            @Override
            public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                    Color bgcolor, ImageObserver observer) {
                return false;
            }

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
        };
    }

    // CHECKSTYLE.ON: ParameterNumber
    // CHECKSTYLE.ON: MethodLength
    // CHECKSTYLE.ON: AnonInnerLength

    /**
     * Creates a new way with the given tags (see {@link OsmUtils#createPrimitive(java.lang.String)}) and the nodes added
     *
     * @param tags  the tags to set
     * @param nodes the nodes to add
     * @return a new way
     */
    public static Way newWay(String tags, Node... nodes) {
        final Way way = (Way) OsmUtils.createPrimitive("way " + tags);
        for (Node node : nodes) {
            way.addNode(node);
        }
        return way;
    }

    /**
     * Creates a new relation with the given tags (see {@link OsmUtils#createPrimitive(java.lang.String)}) and the members added
     *
     * @param tags  the tags to set
     * @param members the members to add
     * @return a new relation
     */
    public static Relation newRelation(String tags, RelationMember... members) {
        final Relation relation = (Relation) OsmUtils.createPrimitive("relation " + tags);
        for (RelationMember member : members) {
            relation.addMember(member);
        }
        return relation;
    }
}
