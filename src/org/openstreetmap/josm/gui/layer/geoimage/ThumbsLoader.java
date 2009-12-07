// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.MediaTracker;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.ImageEntry;

public class ThumbsLoader implements Runnable {
        List<ImageEntry> data;
        public ThumbsLoader(List<ImageEntry> data) {
            this.data = data;
        }

        public void run() {
            System.err.println("Load Thumbnails");
            MediaTracker tracker = new MediaTracker(Main.map.mapView);
            for (int i = 0; i < data.size(); i++) {
                System.err.println("getImg "+i);
                String path;
                path = data.get(i).file.getPath();
                Image img = Toolkit.getDefaultToolkit().createImage(path);
                tracker.addImage(img, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    System.err.println("InterruptedException");
                    return; //  FIXME
                }
                BufferedImage scaledBI = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaledBI.createGraphics();
                while (!g.drawImage(img, 0, 0, 16, 16, null))
                {
                    try {
                        Thread.sleep(10);
                    } catch(InterruptedException ie) {}
                }
                g.dispose();
                tracker.removeImage(img);

                data.get(i).thumbnail = scaledBI;
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

//                boolean error = tracker.isErrorID(1);
//                if (img != null && (img.getWidth(null) == 0 || img.getHeight(null) == 0)) {
//                    error = true;
//                }


        }

    }
