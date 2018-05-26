// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.ImageIcon;

/**
 * Helper class for HiDPI support.
 *
 * Gives access to the class <code>BaseMultiResolutionImage</code> via reflection,
 * in case it is on classpath. This is to be expected for Java 9, but not for Java 8 runtime.
 *
 * @since 12722
 */
public final class HiDPISupport {

    private static volatile Optional<Class<? extends Image>> baseMultiResolutionImageClass;
    private static volatile Optional<Constructor<? extends Image>> baseMultiResolutionImageConstructor;
    private static volatile Optional<Method> resolutionVariantsMethod;

    private HiDPISupport() {
        // Hide default constructor
    }

    /**
     * Create a multi-resolution image from a base image and an {@link ImageResource}.
     * <p>
     * Will only return multi-resolution image, if HiDPI-mode is detected. Then
     * the image stack will consist of the base image and one that fits the
     * HiDPI scale of the main display.
     * @param base the base image
     * @param ir a corresponding image resource
     * @return multi-resolution image if necessary and possible, the base image otherwise
     */
    public static Image getMultiResolutionImage(Image base, ImageResource ir) {
        double uiScale = getHiDPIScale();
        if (uiScale != 1.0 && getBaseMultiResolutionImageConstructor().isPresent()) {
            ImageIcon zoomed = ir.getImageIcon(new Dimension(
                    (int) Math.round(base.getWidth(null) * uiScale),
                    (int) Math.round(base.getHeight(null) * uiScale)), false);
            Image mrImg = getMultiResolutionImage(Arrays.asList(base, zoomed.getImage()));
            if (mrImg != null) return mrImg;
        }
        return base;
    }

    /**
     * Create a multi-resolution image from a list of images.
     * @param imgs the images, supposedly the same image at different resolutions,
     * must not be empty
     * @return corresponding multi-resolution image, if possible, the first image
     * in the list otherwise
     */
    public static Image getMultiResolutionImage(List<Image> imgs) {
        CheckParameterUtil.ensure(imgs, "imgs", "not empty", ls -> !ls.isEmpty());
        Optional<Constructor<? extends Image>> baseMrImageConstructor = getBaseMultiResolutionImageConstructor();
        if (baseMrImageConstructor.isPresent()) {
            try {
                return baseMrImageConstructor.get().newInstance((Object) imgs.toArray(new Image[0]));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                Logging.error("Unexpected error while instantiating object of class BaseMultiResolutionImage: " + ex);
            }
        }
        return imgs.get(0);
    }

    /**
     * Wrapper for the method <code>java.awt.image.BaseMultiResolutionImage#getBaseImage()</code>.
     * <p>
     * Will return the argument <code>img</code> unchanged, if it is not a multi-resolution image.
     * @param img the image
     * @return if <code>img</code> is a <code>java.awt.image.BaseMultiResolutionImage</code>,
     * then the base image, otherwise the image itself
     */
    public static Image getBaseImage(Image img) {
        Optional<Class<? extends Image>> baseMrImageClass = getBaseMultiResolutionImageClass();
        Optional<Method> resVariantsMethod = getResolutionVariantsMethod();
        if (!baseMrImageClass.isPresent() || !resVariantsMethod.isPresent()) {
            return img;
        }
        if (baseMrImageClass.get().isInstance(img)) {
            try {
                @SuppressWarnings("unchecked")
                List<Image> imgVars = (List<Image>) resVariantsMethod.get().invoke(img);
                if (!imgVars.isEmpty()) {
                    return imgVars.get(0);
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Logging.error("Unexpected error while calling method: " + ex);
            }
        }
        return img;
    }

    /**
     * Wrapper for the method <code>java.awt.image.MultiResolutionImage#getResolutionVariants()</code>.
     * <p>
     * Will return the argument as a singleton list, in case it is not a multi-resolution image.
     * @param img the image
     * @return if <code>img</code> is a <code>java.awt.image.BaseMultiResolutionImage</code>,
     * then the result of the method <code>#getResolutionVariants()</code>, otherwise the image
     * itself as a singleton list
     */
    public static List<Image> getResolutionVariants(Image img) {
        Optional<Class<? extends Image>> baseMrImageClass = getBaseMultiResolutionImageClass();
        Optional<Method> resVariantsMethod = getResolutionVariantsMethod();
        if (!baseMrImageClass.isPresent() || !resVariantsMethod.isPresent()) {
            return Collections.singletonList(img);
        }
        if (baseMrImageClass.get().isInstance(img)) {
            try {
                @SuppressWarnings("unchecked")
                List<Image> imgVars = (List<Image>) resVariantsMethod.get().invoke(img);
                if (!imgVars.isEmpty()) {
                    return imgVars;
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Logging.error("Unexpected error while calling method: " + ex);
            }
        }
        return Collections.singletonList(img);
    }

    /**
     * Detect the GUI scale for HiDPI mode.
     * <p>
     * This method may not work as expected for a multi-monitor setup. It will
     * only take the default screen device into account.
     * @return the GUI scale for HiDPI mode, a value of 1.0 means standard mode.
     */
    private static double getHiDPIScale() {
        if (GraphicsEnvironment.isHeadless())
            return 1.0;
        GraphicsConfiguration gc = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().
                getDefaultConfiguration();
        AffineTransform transform = gc.getDefaultTransform();
        if (!Utils.equalsEpsilon(transform.getScaleX(), transform.getScaleY())) {
            Logging.warn("Unexpected ui transform: " + transform);
        }
        return transform.getScaleX();
    }

    /**
     * Perform an operation on multi-resolution images.
     *
     * When input image is not multi-resolution, it will simply apply the processor once.
     * Otherwise, the processor will be called for each resolution variant and the
     * resulting images assembled to become the output multi-resolution image.
     * @param img input image, possibly multi-resolution
     * @param processor processor taking a plain image as input and returning a single
     * plain image as output
     * @return multi-resolution image assembled from the output of calls to <code>processor</code>
     * for each resolution variant
     */
    public static Image processMRImage(Image img, Function<Image, Image> processor) {
        return processMRImages(Collections.singletonList(img), imgs -> processor.apply(imgs.get(0)));
    }

    /**
     * Perform an operation on multi-resolution images.
     *
     * When input images are not multi-resolution, it will simply apply the processor once.
     * Otherwise, the processor will be called for each resolution variant and the
     * resulting images assembled to become the output multi-resolution image.
     * @param imgs input images, possibly multi-resolution
     * @param processor processor taking a list of plain images as input and returning
     * a single plain image as output
     * @return multi-resolution image assembled from the output of calls to <code>processor</code>
     * for each resolution variant
     */
    public static Image processMRImages(List<Image> imgs, Function<List<Image>, Image> processor) {
        CheckParameterUtil.ensureThat(!imgs.isEmpty(), "at least one element expected");
        if (!getBaseMultiResolutionImageClass().isPresent()) {
            return processor.apply(imgs);
        }
        List<List<Image>> allVars = imgs.stream().map(HiDPISupport::getResolutionVariants).collect(Collectors.toList());
        int maxVariants = allVars.stream().mapToInt(List<Image>::size).max().getAsInt();
        if (maxVariants == 1)
            return processor.apply(imgs);
        List<Image> imgsProcessed = IntStream.range(0, maxVariants)
                .mapToObj(
                        k -> processor.apply(
                                allVars.stream().map(vars -> vars.get(k)).collect(Collectors.toList())
                        )
                ).collect(Collectors.toList());
        return getMultiResolutionImage(imgsProcessed);
    }

    private static Optional<Class<? extends Image>> getBaseMultiResolutionImageClass() {
        if (baseMultiResolutionImageClass == null) {
            synchronized (HiDPISupport.class) {
                if (baseMultiResolutionImageClass == null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends Image> c = (Class<? extends Image>) Class.forName("java.awt.image.BaseMultiResolutionImage");
                        baseMultiResolutionImageClass = Optional.ofNullable(c);
                    } catch (ClassNotFoundException ex) {
                        // class is not present in Java 8
                        baseMultiResolutionImageClass = Optional.empty();
                        Logging.trace(ex);
                    }
                }
            }
        }
        return baseMultiResolutionImageClass;
    }

    private static Optional<Constructor<? extends Image>> getBaseMultiResolutionImageConstructor() {
        if (baseMultiResolutionImageConstructor == null) {
            synchronized (HiDPISupport.class) {
                if (baseMultiResolutionImageConstructor == null) {
                    getBaseMultiResolutionImageClass().ifPresent(klass -> {
                        try {
                            Constructor<? extends Image> constr = klass.getConstructor(Image[].class);
                            baseMultiResolutionImageConstructor = Optional.ofNullable(constr);
                        } catch (NoSuchMethodException ex) {
                            Logging.error("Cannot find expected constructor: " + ex);
                        }
                    });
                    if (baseMultiResolutionImageConstructor == null) {
                        baseMultiResolutionImageConstructor = Optional.empty();
                    }
                }
            }
        }
        return baseMultiResolutionImageConstructor;
    }

    private static Optional<Method> getResolutionVariantsMethod() {
        if (resolutionVariantsMethod == null) {
            synchronized (HiDPISupport.class) {
                if (resolutionVariantsMethod == null) {
                    getBaseMultiResolutionImageClass().ifPresent(klass -> {
                        try {
                            Method m = klass.getMethod("getResolutionVariants");
                            resolutionVariantsMethod = Optional.ofNullable(m);
                        } catch (NoSuchMethodException ex) {
                            Logging.error("Cannot find expected method: "+ex);
                        }
                    });
                    if (resolutionVariantsMethod == null) {
                        resolutionVariantsMethod = Optional.empty();
                    }
                }
            }
        }
        return resolutionVariantsMethod;
    }
}
