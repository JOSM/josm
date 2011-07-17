/*
 * Font.java
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
 * Created on February 20, 2004, 10:00 PM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;

/**
 *
 * @author kitfox
 */
public class Hkern extends SVGElement
{
    String u1;
    String u2;
    int k;


    protected void build() throws SVGException
    {
        super.build();

        StyleAttribute sty = new StyleAttribute();


        //Read glyph spacing info
        if (getPres(sty.setName("u1"))) u1 = sty.getStringValue();

        if (getPres(sty.setName("u2"))) u2 = sty.getStringValue();

        if (getPres(sty.setName("k"))) k = sty.getIntValue();
    }

    public boolean updateTime(double curTime) throws SVGException
    {
        //Fonts can't change
        return false;
    }
    
    
}
