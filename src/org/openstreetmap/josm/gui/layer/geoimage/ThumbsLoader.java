// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.MediaTracker;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.io.CacheFiles;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.ImageEntry;

public class ThumbsLoader implements Runnable {
        volatile boolean stop = false;
        List<ImageEntry> data;
        MediaTracker tracker;
        CacheFiles cache;
        boolean cacheOff = Main.pref.getBoolean("geoimage.noThumbnailCache", false);
        
        public ThumbsLoader(List<ImageEntry> data) {
            this.data = new ArrayList<ImageEntry>(data);
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
                    Main.map.mapView.repaint();
                }
            }
            (new Thread() {
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
            final int size = 16;
            final String cacheIdent = entry.file.toString()+":"+size;
            
            if (!cacheOff) {
                BufferedImage cached = cache.getImg(cacheIdent);
                if(cached != null) {
                    System.err.println(" from cache"); 
                    return cached;
                }
            }
            
            Image img = Toolkit.getDefaultToolkit().createImage(entry.file.getPath());
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                System.err.println(" InterruptedException");
                return null;
            }
            BufferedImage scaledBI = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledBI.createGraphics();
            while (!g.drawImage(img, 0, 0, 16, 16, null))
            {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException ie) {} //FIXME: timeout?
            }
            g.dispose();
            tracker.removeImage(img);
            
            if (!cacheOff && scaledBI != null && scaledBI.getWidth() > 0) {
                cache.saveImg(cacheIdent, scaledBI);
            }
            
            System.err.println("");
            return scaledBI;
        }

    }
