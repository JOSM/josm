/*
 * BuildHistory.java
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
 * Created on January 26, 2004, 9:18 PM
 */

package com.kitfox.svg.pathcmd;

import java.awt.geom.Point2D;

/**
 * When building a path from command segments, most need to cache information
 * (such as the point finished at) for future commands.  This structure allows
 * that
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class BuildHistory {

//    Point2D.Float[] history = new Point2D.Float[2];
    Point2D.Float[] history = {new Point2D.Float(), new Point2D.Float()};
    Point2D.Float start = new Point2D.Float();
    int length = 0;

    /** Creates a new instance of BuildHistory */
    public BuildHistory() {
    }

    public void setPoint(float x, float y)
    {
        history[0].setLocation(x, y);
        length = 1;
    }

    public void setStart(float x, float y)
    {
        start.setLocation(x, y);
    }

    public void setPointAndKnot(float x, float y, float kx, float ky)
    {
        history[0].setLocation(x, y);
        history[1].setLocation(kx, ky);
        length = 2;
    }
}
