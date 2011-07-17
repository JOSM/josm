/*
 * PatternPaint.java
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
public class PatternPaint implements Paint
{
    BufferedImage source;  //Image we're rendering from
    AffineTransform xform;

    /** Creates a new instance of PatternPaint */
    public PatternPaint(BufferedImage source, AffineTransform xform)
    {
        this.source = source;
        this.xform = xform;
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints)
    {
        return new PatternPaintContext(source, deviceBounds, xform, this.xform);
    }

    public int getTransparency()
    {
        return source.getColorModel().getTransparency();
    }

}
