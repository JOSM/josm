/*
 * BoundedElement.java
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
 * Created on January 26, 2004, 9:00 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


/**
 * Maintains bounding box for this element
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class TransformableElement extends SVGElement 
{

    AffineTransform xform = null;
//    AffineTransform invXform = null;

    /** Creates a new instance of BoundedElement */
    public TransformableElement() {
    }

    public TransformableElement(String id, SVGElement parent)
    {
        super(id, parent);
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String transform = attrs.getValue("transform");
        if (transform != null)
        {
            xform = parseTransform(transform);
        }
    }
*/
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("transform")))
        {
            xform = parseTransform(sty.getStringValue());
        }
    }
    
    protected Shape shapeToParent(Shape shape)
    {
        if (xform == null) return shape;
        return xform.createTransformedShape(shape);
    }

    protected Rectangle2D boundsToParent(Rectangle2D rect)
    {
        if (xform == null) return rect;
        return xform.createTransformedShape(rect).getBounds2D();
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("transform")))
        {
            AffineTransform newXform = parseTransform(sty.getStringValue());
            if (!newXform.equals(xform))
            {
                xform = newXform;
                return true;
            }
        }
        
        return false;
    }
}
