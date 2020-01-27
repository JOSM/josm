// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Shape;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.ImageView;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Specialized Image View allowing to display SVG images.
 * @since 8933
 */
public class JosmImageView extends ImageView {

    private static final int LOADING_FLAG = 1;
    private static final int WIDTH_FLAG = 4;
    private static final int HEIGHT_FLAG = 8;
    private static final int RELOAD_FLAG = 16;
    private static final int RELOAD_IMAGE_FLAG = 32;

    private final Field imageField;
    private final Field stateField;
    private final Field widthField;
    private final Field heightField;

    /**
     * Constructs a new {@code JosmImageView}.
     * @param elem the element to create a view for
     * @throws SecurityException see {@link Class#getDeclaredField} for details
     * @throws NoSuchFieldException see {@link Class#getDeclaredField} for details
     */
    public JosmImageView(Element elem) throws NoSuchFieldException {
        super(elem);
        imageField = getDeclaredField("image");
        stateField = getDeclaredField("state");
        widthField = getDeclaredField("width");
        heightField = getDeclaredField("height");
        ReflectionUtils.setObjectsAccessible(imageField, stateField, widthField, heightField);
    }

    private static Field getDeclaredField(String name) throws NoSuchFieldException {
        try {
            return ImageView.class.getDeclaredField(name);
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to access field by reflection", e);
            return null;
        }
    }

    /**
     * Makes sure the necessary properties and image is loaded.
     */
    private void doSync() {
        try {
            int s = (int) stateField.get(this);
            if ((s & RELOAD_IMAGE_FLAG) != 0) {
                doRefreshImage();
            }
            s = (int) stateField.get(this);
            if ((s & RELOAD_FLAG) != 0) {
                synchronized (this) {
                    stateField.set(this, ((int) stateField.get(this) | RELOAD_FLAG) ^ RELOAD_FLAG);
                }
                setPropertiesFromAttributes();
            }
        } catch (IllegalArgumentException | ReflectiveOperationException | SecurityException e) {
           Logging.error(e);
       }
    }

    /**
     * Loads the image and updates the size accordingly. This should be
     * invoked instead of invoking <code>loadImage</code> or
     * <code>updateImageSize</code> directly.
     * @throws IllegalAccessException see {@link Field#set} and {@link Method#invoke} for details
     * @throws IllegalArgumentException see {@link Field#set} and {@link Method#invoke} for details
     * @throws InvocationTargetException see {@link Method#invoke} for details
     * @throws NoSuchMethodException see {@link Class#getDeclaredMethod} for details
     * @throws SecurityException see {@link Class#getDeclaredMethod} for details
     */
    private void doRefreshImage() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        synchronized (this) {
            // clear out width/height/reloadimage flag and set loading flag
            stateField.set(this, ((int) stateField.get(this) | LOADING_FLAG | RELOAD_IMAGE_FLAG | WIDTH_FLAG |
                     HEIGHT_FLAG) ^ (WIDTH_FLAG | HEIGHT_FLAG |
                                     RELOAD_IMAGE_FLAG));
            imageField.set(this, null);
            widthField.set(this, 0);
            heightField.set(this, 0);
        }

        try {
            // Load the image
            doLoadImage();

            // And update the size params
            Method updateImageSize = ImageView.class.getDeclaredMethod("updateImageSize");
            ReflectionUtils.setObjectsAccessible(updateImageSize);
            updateImageSize.invoke(this);
        } finally {
            synchronized (this) {
                // Clear out state in case someone threw an exception.
                stateField.set(this, ((int) stateField.get(this) | LOADING_FLAG) ^ LOADING_FLAG);
            }
        }
    }

    /**
     * Loads the image from the URL <code>getImageURL</code>. This should
     * only be invoked from <code>refreshImage</code>.
     * @throws IllegalAccessException see {@link Field#set} and {@link Method#invoke} for details
     * @throws IllegalArgumentException see {@link Field#set} and {@link Method#invoke} for details
     * @throws InvocationTargetException see {@link Method#invoke} for details
     * @throws NoSuchMethodException see {@link Class#getDeclaredMethod} for details
     * @throws SecurityException see {@link Class#getDeclaredMethod} for details
     */
    private void doLoadImage() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        URL src = getImageURL();
        if (src != null) {
            String urlStr = src.toExternalForm();
            if (urlStr.endsWith(".svg") || urlStr.endsWith(".svg?format=raw")) {
                ImageIcon imgIcon = new ImageProvider(urlStr).setOptional(true).get();
                setLoadsSynchronously(true); // make sure width/height are properly updated
                imageField.set(this, imgIcon != null ? imgIcon.getImage() : null);
            } else {
                Method loadImage = ImageView.class.getDeclaredMethod("loadImage");
                ReflectionUtils.setObjectsAccessible(loadImage);
                loadImage.invoke(this);
            }
        } else {
            imageField.set(this, null);
        }
    }

    @Override
    public Image getImage() {
        doSync();
        return super.getImage();
    }

    @Override
    public AttributeSet getAttributes() {
        doSync();
        return super.getAttributes();
    }

    @Override
    public void paint(Graphics g, Shape a) {
        doSync();
        super.paint(g, a);
    }

    @Override
    public float getPreferredSpan(int axis) {
        doSync();
        return super.getPreferredSpan(axis);
    }

    @Override
    public void setSize(float width, float height) {
        doSync();
        super.setSize(width, height);
    }
}
