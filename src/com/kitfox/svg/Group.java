/*
 * Stop.java
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
 * Created on January 26, 2004, 1:56 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;


/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class Group extends ShapeElement
{

    //Cache bounding box for faster clip testing
    Rectangle2D boundingBox;
    Shape cachedShape;

    //Cache clip bounds
    final Rectangle clipBounds = new Rectangle();

    /** Creates a new instance of Stop */
    public Group() {
    }

    /*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
        //Load style string
        super.loaderStartElement(helper, attrs, parent);

        //String transform = attrs.getValue("transform");
    }
     */

    /**
     * Called after the start element but before the end element to indicate
     * each child tag that has been processed
     */
    public void loaderAddChild(SVGLoaderHelper helper, SVGElement child) throws SVGElementException
    {
        super.loaderAddChild(helper, child);

//        members.add(child);
    }

    protected boolean outsideClip(Graphics2D g) throws SVGException
    {
        g.getClipBounds(clipBounds);
        Rectangle2D rect = getBoundingBox();

//if (rect == null)
//{
//    rect = getBoundingBox();
//}

//        if (rect.intersects(clipBounds))
        if (rect.intersects(clipBounds))
        {
            return false;
        }

        return true;
    }

    void pick(Point2D point, boolean boundingBox, List retVec) throws SVGException
    {
        Point2D xPoint = new Point2D.Double(point.getX(), point.getY());
        if (xform != null)
        {
            try
            {
                xform.inverseTransform(point, xPoint);
            } 
            catch (NoninvertibleTransformException ex)
            {
                throw new SVGException(ex);
            }
        }
        
        
        for (Iterator it = children.iterator(); it.hasNext();)
        {
            SVGElement ele = (SVGElement)it.next();
            if (ele instanceof RenderableElement)
            {
                RenderableElement rendEle = (RenderableElement)ele;
                
                rendEle.pick(xPoint, boundingBox, retVec);
            }
        }
    }

    void pick(Rectangle2D pickArea, AffineTransform ltw, boolean boundingBox, List retVec) throws SVGException
    {
        if (xform != null)
        {
            ltw = new AffineTransform(ltw);
            ltw.concatenate(xform);
        }
        
        
        for (Iterator it = children.iterator(); it.hasNext();)
        {
            SVGElement ele = (SVGElement)it.next();
            if (ele instanceof RenderableElement)
            {
                RenderableElement rendEle = (RenderableElement)ele;
                
                rendEle.pick(pickArea, ltw, boundingBox, retVec);
            }
        }
    }

    public void render(Graphics2D g) throws SVGException
    {
        //Don't process if not visible
        StyleAttribute styleAttrib = new StyleAttribute();
        if (getStyle(styleAttrib.setName("visibility")))
        {
            if (!styleAttrib.getStringValue().equals("visible")) return;
        }
        
        //Do not process offscreen groups
        boolean ignoreClip = diagram.ignoringClipHeuristic();
        if (!ignoreClip && outsideClip(g)) return;

        beginLayer(g);

        Iterator it = children.iterator();

        try
        {
            g.getClipBounds(clipBounds);
        }
        catch (Exception e)
        {
            //For some reason, getClipBounds can throw a null pointer exception for 
            // some types of Graphics2D
            ignoreClip = true;
        }

        while (it.hasNext())
        {
            SVGElement ele = (SVGElement)it.next();
            if (ele instanceof RenderableElement)
            {
                RenderableElement rendEle = (RenderableElement)ele;

//                if (shapeEle == null) continue;

                if (!(ele instanceof Group))
                {
                    //Skip if clipping area is outside our bounds
                    if (!ignoreClip && !rendEle.getBoundingBox().intersects(clipBounds)) 
                    {
                        continue;
                    }
                }

                rendEle.render(g);
            }
        }

        finishLayer(g);
    }


    /**
     * Retrieves the cached bounding box of this group
     */
    public Shape getShape()
    {
        if (cachedShape == null) calcShape();
        return cachedShape;
    }

    public void calcShape()
    {
        Area retShape = new Area();

        for (Iterator it = children.iterator(); it.hasNext();)
        {
            SVGElement ele = (SVGElement)it.next();

            if (ele instanceof ShapeElement)
            {
                ShapeElement shpEle = (ShapeElement)ele;
                Shape shape = shpEle.getShape();
                if (shape != null)
                {
                    retShape.add(new Area(shape));
                }
            }
        }

        cachedShape = shapeToParent(retShape);
    }

    /**
     * Retrieves the cached bounding box of this group
     */
    public Rectangle2D getBoundingBox() throws SVGException
    {
        if (boundingBox == null) calcBoundingBox();
//        calcBoundingBox();
        return boundingBox;
    }

    /**
     * Recalculates the bounding box by taking the union of the bounding boxes
     * of all children.  Caches the result.
     */
    public void calcBoundingBox() throws SVGException
    {
//        Rectangle2D retRect = new Rectangle2D.Float();
        Rectangle2D retRect = null;

        for (Iterator it = children.iterator(); it.hasNext();)
        {
            SVGElement ele = (SVGElement)it.next();

            if (ele instanceof RenderableElement)
            {
                RenderableElement rendEle = (RenderableElement)ele;
                Rectangle2D bounds = rendEle.getBoundingBox();
                if (bounds != null)
                {
                    if (retRect == null) retRect = bounds;
                    else retRect = retRect.createUnion(bounds);
                }
            }
        }

//        if (xform != null)
//        {
//            retRect = xform.createTransformedShape(retRect).getBounds2D();
//        }

        //If no contents, use degenerate rectangle
        if (retRect == null) retRect = new Rectangle2D.Float();

        boundingBox = boundsToParent(retRect);
    }

    public boolean updateTime(double curTime) throws SVGException
    {
        boolean changeState = super.updateTime(curTime);
        Iterator it = children.iterator();

        //Distribute message to all members of this group
        while (it.hasNext())
        {
            SVGElement ele = (SVGElement)it.next();
            boolean updateVal = ele.updateTime(curTime);

            changeState = changeState || updateVal;

            //Update our shape if shape aware children change
            if (ele instanceof ShapeElement) cachedShape = null;
            if (ele instanceof RenderableElement) boundingBox = null;
        }

        return changeState;
    }
}
