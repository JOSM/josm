/*
 * MoveTo.java
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
 * Created on January 26, 2004, 8:40 PM
 */

package com.kitfox.svg.pathcmd;

//import org.apache.batik.ext.awt.geom.ExtendedGeneralPath;
import java.awt.geom.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class QuadraticSmooth extends PathCommand {

    public float x = 0f;
    public float y = 0f;

    /** Creates a new instance of MoveTo */
    public QuadraticSmooth() {
    }

    public QuadraticSmooth(boolean isRelative, float x, float y) {
        super(isRelative);
        this.x = x;
        this.y = y;
    }

//    public void appendPath(ExtendedGeneralPath path, BuildHistory hist)
    public void appendPath(GeneralPath path, BuildHistory hist)
    {
        float offx = isRelative ? hist.history[0].x : 0f;
        float offy = isRelative ? hist.history[0].y : 0f;

        float oldKx = hist.history.length >= 2 ? hist.history[1].x : hist.history[0].x;
        float oldKy = hist.history.length >= 2 ? hist.history[1].y : hist.history[0].y;
        float oldX = hist.history[0].x;
        float oldY = hist.history[0].y;
        //Calc knot as reflection of old knot
        float kx = oldX * 2f - oldKx;
        float ky = oldY * 2f - oldKy;

        path.quadTo(kx, ky, x + offx, y + offy);
        hist.setPointAndKnot(x + offx, y + offy, kx, ky);
    }

    public int getNumKnotsAdded()
    {
        return 4;
    }
}
