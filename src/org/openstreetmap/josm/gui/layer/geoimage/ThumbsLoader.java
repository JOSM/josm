// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.io.CacheFiles;
import org.openstreetmap.josm.Main;

public class ThumbsLoader implements Runnable {
        public static final int maxSize = 120;
        public static final int minSize = 22;
        volatile boolean stop = false;
        List<ImageEntry> data;
        GeoImageLayer layer;
        MediaTracker tracker;
        CacheFiles cache;
        boolean cacheOff = Main.pref.getBoolean("geoimage.noThumbnailCache", false);

        public ThumbsLoader(GeoImageLayer layer) {
            this.layer = layer;
            this.data = new ArrayList<ImageEntry>(layer.data);
            if (!cacheOff) {
                cache = new CacheFiles("geoimage-thumbnails", false);
                cache.setExpire(CacheFiles.EXPIRE_NEVER, false);
                cache.setMaxSize(120, false);
            }
        }

        public void run() {
            System.err.println("Load Thumbnails");
            tracker = new MediaTracker(Main.map.mapView);
            for (int i = 0; i < data.size(); i++) {
                if (stop) return;

                System.err.print("fetching image "+i);

                data.get(i).thumbnail = loadThumb(data.get(i));

                if (Main.map != null && Main.map.mapView != null) {
                    try {
                        layer.updateOffscreenBuffer = true;
                    } catch (Exception e) {}
                    Main.map.mapView.repaint();
                }
            }
            try {
                layer.updateOffscreenBuffer = true;
            } catch (Exception e) {}
            Main.map.mapView.repaint();
            (new Thread() {             // clean up the garbage - shouldn't hurt
                public void run() {
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException ie) {}
                    System.gc();
                }
            }).start();

        }

        private BufferedImage loadThumb(ImageEntry entry) {
            final String cacheIdent = entry.getFile().toString()+":"+maxSize;

            if (!cacheOff) {
                BufferedImage cached = cache.getImg(cacheIdent);
                if(cached != null) {
                    System.err.println(" from cache");
                    return cached;
                }
            }

            Image img = Toolkit.getDefaultToolkit().createImage(entry.getFile().getPath());
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                System.err.println(" InterruptedException");
                return null;
            }
            if (tracker.isErrorID(1) || img.getWidth(null) <= 0 || img.getHeight(null) <= 0) {
                System.err.println(" Invalid image");
                return null;
            }
            Rectangle targetSize = ImageDisplay.calculateDrawImageRectangle(
                new Rectangle(0, 0, img.getWidth(null), img.getHeight(null)),
                new Rectangle(0, 0, maxSize, maxSize));
            BufferedImage scaledBI = new BufferedImage(targetSize.width, targetSize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledBI.createGraphics();
            while (!g.drawImage(img, 0, 0, targetSize.width, targetSize.height, null))
            {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException ie) {}
            }
            g.dispose();
            tracker.removeImage(img);

            if (scaledBI == null || scaledBI.getWidth() <= 0 || scaledBI.getHeight() <= 0) {
                System.err.println(" Invalid image");
                return null;
            }

            if (!cacheOff) {
                cache.saveImg(cacheIdent, scaledBI);
            }

            System.err.println("");
            return scaledBI;
        }

    }
