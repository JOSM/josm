// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public final class FeatureAdapter {

    private static BrowserAdapter browserAdapter = new DefaultBrowserAdapter();
    private static ImageAdapter imageAdapter = new DefaultImageAdapter();
    private static TranslationAdapter translationAdapter = new DefaultTranslationAdapter();
    private static LoggingAdapter loggingAdapter = new DefaultLoggingAdapter();

    private FeatureAdapter() {
        // private constructor for utility classes
    }

    public interface BrowserAdapter {
        void openLink(String url);
    }

    public interface TranslationAdapter {
        String tr(String text, Object... objects);
        // TODO: more i18n functions
    }

    public interface LoggingAdapter {
        Logger getLogger(String name);
    }

    public interface ImageAdapter {
        BufferedImage read(URL input, boolean readMetadata, boolean enforceTransparency) throws IOException;
    }

    public static void registerBrowserAdapter(BrowserAdapter browserAdapter) {
        FeatureAdapter.browserAdapter = Objects.requireNonNull(browserAdapter);
    }

    public static void registerImageAdapter(ImageAdapter imageAdapter) {
        FeatureAdapter.imageAdapter = Objects.requireNonNull(imageAdapter);
    }

    public static void registerTranslationAdapter(TranslationAdapter translationAdapter) {
        FeatureAdapter.translationAdapter = Objects.requireNonNull(translationAdapter);
    }

    public static void registerLoggingAdapter(LoggingAdapter loggingAdapter) {
        FeatureAdapter.loggingAdapter = Objects.requireNonNull(loggingAdapter);
    }

    public static void openLink(String url) {
        browserAdapter.openLink(url);
    }

    public static BufferedImage readImage(URL url) throws IOException {
        return imageAdapter.read(url, false, false);
    }

    public static String tr(String text, Object... objects) {
        return translationAdapter.tr(text, objects);
    }

    public static Logger getLogger(String name) {
        return loggingAdapter.getLogger(name);
    }

    public static class DefaultBrowserAdapter implements BrowserAdapter {
        @Override
        public void openLink(String url) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println(tr("Opening link not supported on current platform (''{0}'')", url));
            }
        }
    }

    public static class DefaultImageAdapter implements ImageAdapter {
        @Override
        public BufferedImage read(URL input, boolean readMetadata, boolean enforceTransparency) throws IOException {
            return ImageIO.read(input);
        }
    }

    public static class DefaultTranslationAdapter implements TranslationAdapter {
        @Override
        public String tr(String text, Object... objects) {
            return MessageFormat.format(text, objects);
        }
    }

    public static class DefaultLoggingAdapter implements LoggingAdapter {
        @Override
        public Logger getLogger(String name) {
            return Logger.getLogger(name);
        }
    }
}
