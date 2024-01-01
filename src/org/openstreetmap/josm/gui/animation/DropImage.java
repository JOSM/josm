// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.animation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Random;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * A random image displayed when {@link ChristmasExtension} is active.
 * @since 18929
 */
class DropImage implements IAnimObject {
    private static final Random seed = new Random();

    static final int averageFallSpeed = 4;     // 2-6

    private int w;
    private int h;

    private final Point edge = new Point();
    private final int fallSpeed;
    private Image image;

    DropImage(int w, int h) {
        this.w = w;
        this.h = h;
        edge.x = seed.nextInt(w - 1);
        edge.y = seed.nextInt(h + 1);
        fallSpeed = averageFallSpeed / 2 + seed.nextInt(averageFallSpeed / 2);
        image = getImage();
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(image, edge.x, edge.y, null);
    }

    @Override
    public void setExtend(int w, int h) {
        this.w = w;
        this.h = h;
    }

    @Override
    public void animate() {
        edge.y += fallSpeed;
        if (edge.x > w - 1 || edge.y > h) {
            edge.x = seed.nextInt(w - 1);
            edge.y = -image.getWidth(null) * 2;
            image = getImage();
        }
    }
    
    private Image getImage() {
        int size = 15 + seed.nextInt(5);
        String name = "logo";
        try {
            ArrayList<String> result = new ArrayList<>();
            String path = "images/presets/";
            URL url = DropImage.class.getClassLoader().getResource(path);
            if (url != null && url.getProtocol().equals("file")) {
                ArrayList<File> dirs = new ArrayList<>();
                dirs.add(new File(url.toURI()));
                do {
                    File[] files = dirs.remove(0).listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile()) {
                                result.add(f.getPath());
                            } else {
                                dirs.add(f);
                            }
                        }
                    }
                } while (!dirs.isEmpty());
                name = result.get(seed.nextInt(result.size()));
            } else if (url != null && url.getProtocol().equals("jar")) {
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String fileName = entries.nextElement().getName();
                        if (fileName.startsWith(path) && !fileName.endsWith("/")) {
                            result.add(fileName.substring(7));
                        }
                    }
                }
                name = result.get(seed.nextInt(result.size()));
            }
            return new ImageProvider(name).setMaxSize(new Dimension(size, size)).get().getImage();
        } catch (Exception ex) {
            Logging.log(Logging.LEVEL_DEBUG, ex);
        }
        return new ImageProvider("logo").setMaxSize(new Dimension(size, size)).get().getImage();
    }
}
