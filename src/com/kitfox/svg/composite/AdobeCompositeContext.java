/*
 * AdobeCompositeContext.java
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
 * Created on April 1, 2004, 6:41 AM
 */

package com.kitfox.svg.composite;

import java.awt.*;
import java.awt.image.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class AdobeCompositeContext implements CompositeContext
{
    final int compositeType;
    final float extraAlpha;

    float[] rgba_src = new float[4];
    float[] rgba_dstIn = new float[4];
    float[] rgba_dstOut = new float[4];

    /** Creates a new instance of AdobeCompositeContext */
    public AdobeCompositeContext(int compositeType, float extraAlpha)
    {
        this.compositeType = compositeType;
        this.extraAlpha = extraAlpha;

        rgba_dstOut[3] = 1f;
    }

    public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
    {
        int width = src.getWidth();
        int height = src.getHeight();

        for (int j = 0; j < height; j++)
        {
            for (int i = 0; i < width; i++)
            {
                src.getPixel(i, j, rgba_src);
                dstIn.getPixel(i, j, rgba_dstIn);

                //Ignore transparent pixels
                if (rgba_src[3] == 0)
                {
//                    dstOut.setPixel(i, j, rgba_dstIn);
                    continue;
                }

                float alpha = rgba_src[3];

                switch (compositeType)
                {
                    default:
                    case AdobeComposite.CT_NORMAL:
                        rgba_dstOut[0] = rgba_src[0] * alpha + rgba_dstIn[0] * (1f - alpha);
                        rgba_dstOut[1] = rgba_src[1] * alpha + rgba_dstIn[1] * (1f - alpha);
                        rgba_dstOut[2] = rgba_src[2] * alpha + rgba_dstIn[2] * (1f - alpha);
                        break;
                    case AdobeComposite.CT_MULTIPLY:
                        rgba_dstOut[0] = rgba_src[0] * rgba_dstIn[0] * alpha + rgba_dstIn[0] * (1f - alpha);
                        rgba_dstOut[1] = rgba_src[1] * rgba_dstIn[1] * alpha + rgba_dstIn[1] * (1f - alpha);
                        rgba_dstOut[2] = rgba_src[2] * rgba_dstIn[2] * alpha + rgba_dstIn[2] * (1f - alpha);
                        break;
                }
            }
        }
    }

    public void dispose() {
    }

}
