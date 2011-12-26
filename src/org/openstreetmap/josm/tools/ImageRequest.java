// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.io.File;
import java.util.Collection;
import javax.swing.ImageIcon;

/**
 * Alternative way to request an image.
 * E.g.
 *
 * ImageIcon icon = new ImageRequest().setName(imgName).setWidth(100).setHeight(120).get();
 *
 * or in funky double-brace style
 *
 * ImageIcon icon = new ImageProvider.Request(){{name=imgName; width=100; height=120;}}.get();
 */
public class ImageRequest {
    protected Collection<String> dirs;
    protected String id;
    protected String subdir;
    protected String name;
    protected File archive;
    protected int width = -1;
    protected int height = -1;
    protected int maxWidth = -1;
    protected int maxHeight = -1;
    protected boolean sanitize;
    protected boolean required = true;

    public ImageRequest setDirs(Collection<String> dirs) {
        this.dirs = dirs;
        return this;
    }

    public ImageRequest setId(String id) {
        this.id = id;
        return this;
    }

    public ImageRequest setSubdir(String subdir) {
        this.subdir = subdir;
        return this;
    }

    public ImageRequest setName(String name) {
        this.name = name;
        return this;
    }

    public ImageRequest setArchive(File archive) {
        this.archive = archive;
        return this;
    }

    public ImageRequest setWidth(int width) {
        this.width = width;
        return this;
    }

    public ImageRequest setHeight(int height) {
        this.height = height;
        return this;
    }

    public ImageRequest setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public ImageRequest setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public ImageRequest setSanitize(boolean sanitize) {
        this.sanitize = sanitize;
        return this;
    }

    public ImageRequest setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public ImageIcon get() {
        ImageResource ir = ImageProvider.getIfAvailableImpl(dirs, id, subdir, name, archive);
        if (ir == null) {
            if (required) {
                String ext = name.indexOf('.') != -1 ? "" : ".???";
                throw new RuntimeException(tr("Fatal: failed to locate image ''{0}''. This is a serious configuration problem. JOSM will stop working.", name + ext));
            } else
                return null;
        }
        if (maxWidth != -1 || maxHeight != -1)
            return ir.getImageIconBounded(new Dimension(maxWidth, maxHeight), sanitize);
        else 
            return ir.getImageIcon(new Dimension(width, height), sanitize);
    }
    
}
