/*
 * AdobeComposite.java
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
 * Created on April 1, 2004, 6:40 AM
 */

package com.kitfox.svg.composite;

import java.awt.*;
import java.awt.image.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class AdobeComposite implements Composite
{
    public static final int CT_NORMAL = 0;
    public static final int CT_MULTIPLY = 1;
    public static final int CT_LAST = 2;

    final int compositeType;
    final float extraAlpha;

    /** Creates a new instance of AdobeComposite */
    public AdobeComposite(int compositeType, float extraAlpha)
    {
        this.compositeType = compositeType;
        this.extraAlpha = extraAlpha;

        if (compositeType < 0 || compositeType >= CT_LAST)
        {
            new Exception("Invalid composite type").printStackTrace();
        }

        if (extraAlpha < 0f || extraAlpha > 1f)
        {
            new Exception("Invalid alpha").printStackTrace();
        }
    }

    public int getCompositeType() { return compositeType; }

    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints)
    {
        return new AdobeCompositeContext(compositeType, extraAlpha);
    }

}
