/*
 * PatternPaintContext.java
 *
 *
 *  The Salamander Project - 2D and 3D graphics libraries in Java
 *  Copyright (C) 2004 Mark McKay
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 *  projects can be found at http://www.kitfox.com
 *
 * Created on April 1, 2004, 3:37 AM
 */

package com.kitfox.svg.pattern;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class PatternPaintContext implements PaintContext
{
    BufferedImage source;  //Image we're rendering from
    Rectangle deviceBounds;  //int size of rectangle we're rendering to
//    AffineTransform userXform;  //xform from user space to device space
//    AffineTransform distortXform;  //distortion applied to this pattern

    AffineTransform xform;  //distortion applied to this pattern

    int sourceWidth;
    int sourceHeight;

    //Raster we use to build tile
    BufferedImage buf;

    /** Creates a new instance of PatternPaintContext */
    public PatternPaintContext(BufferedImage source, Rectangle deviceBounds, AffineTransform userXform, AffineTransform distortXform)
    {
//System.err.println("Bounds " + deviceBounds);
        this.source = source;
        this.deviceBounds = deviceBounds;
        try {
//            this.distortXform = distortXform.createInverse();
//            this.userXform = userXform.createInverse();

//            xform = userXform.createInverse();
//            xform.concatenate(distortXform.createInverse());
            xform = distortXform.createInverse();
            xform.concatenate(userXform.createInverse());
        }
        catch (Exception e) { e.printStackTrace(); }

        sourceWidth = source.getWidth();
        sourceHeight = source.getHeight();
    }

    public void dispose() {
    }

    public ColorModel getColorModel() {
        return source.getColorModel();
    }

    public Raster getRaster(int x, int y, int w, int h)
    {
//System.err.println("" + x + ", " + y + ", " + w + ", " + h);
        if (buf == null || buf.getWidth() != w || buf.getHeight() != buf.getHeight())
        {
            buf = new BufferedImage(w, h, source.getType());
        }

//        Point2D.Float srcPt = new Point2D.Float(), srcPt2 = new Point2D.Float(), destPt = new Point2D.Float();
        Point2D.Float srcPt = new Point2D.Float(), destPt = new Point2D.Float();
        for (int j = 0; j < h; j++)
        {
            for (int i = 0; i < w; i++)
            {
                destPt.setLocation(i + x, j + y);

                xform.transform(destPt, srcPt);

//                userXform.transform(destPt, srcPt2);
//                distortXform.transform(srcPt2, srcPt);

                int ii = ((int)srcPt.x) % sourceWidth;
                if (ii < 0) ii += sourceWidth;
                int jj = ((int)srcPt.y) % sourceHeight;
                if (jj < 0) jj += sourceHeight;

                buf.setRGB(i, j, source.getRGB(ii, jj));
            }
        }

        return buf.getData();
    }

    public static void main(String[] argv)
    {
        int i = -4;
        System.err.println("Hello " + (i % 4));
    }

}
