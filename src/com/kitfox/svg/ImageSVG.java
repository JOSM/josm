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

import com.kitfox.svg.app.data.Handler;
import com.kitfox.svg.xml.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.*;
import java.util.List;

/**
 * Implements an embedded font.
 *
 * SVG specification: http://www.w3.org/TR/SVG/fonts.html
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class ImageSVG extends RenderableElement
{
    float x = 0f;
    float y = 0f;
    float width = 0f;
    float height = 0f;

//    BufferedImage href = null;
    URL imageSrc = null;

    AffineTransform xform;
    Rectangle2D bounds;

    /** Creates a new instance of Font */
    public ImageSVG()
    {
    }
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("x"))) x = sty.getFloatValueWithUnits();

        if (getPres(sty.setName("y"))) y = sty.getFloatValueWithUnits();

        if (getPres(sty.setName("width"))) width = sty.getFloatValueWithUnits();

        if (getPres(sty.setName("height"))) height = sty.getFloatValueWithUnits();

        try {
            if (getPres(sty.setName("xlink:href")))
            {
                URI src = sty.getURIValue(getXMLBase());
                if ("data".equals(src.getScheme()))
                {
                    imageSrc = new URL(null, src.toASCIIString(), new Handler());
                }
                else
                {
                    try {
                        imageSrc = src.toURL();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        imageSrc = null;
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new SVGException(e);
        }

        diagram.getUniverse().registerImage(imageSrc);
        
        //Set widths if not set
        BufferedImage img = diagram.getUniverse().getImage(imageSrc);
        if (img == null)
        {
            xform = new AffineTransform();
            bounds = new Rectangle2D.Float();
            return;
        }
        
        if (width == 0) width = img.getWidth();
        if (height == 0) height = img.getHeight();
        
        //Determine image xform
        xform = new AffineTransform();
//        xform.setToScale(this.width / img.getWidth(), this.height / img.getHeight());
//        xform.translate(this.x, this.y);
        xform.translate(this.x, this.y);
        xform.scale(this.width / img.getWidth(), this.height / img.getHeight());
        
        bounds = new Rectangle2D.Float(this.x, this.y, this.width, this.height);
    }
    
    
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    void pick(Point2D point, boolean boundingBox, List retVec) throws SVGException
    {
        if (getBoundingBox().contains(point))
        {
            retVec.add(getPath(null));
        }
    }

    void pick(Rectangle2D pickArea, AffineTransform ltw, boolean boundingBox, List retVec) throws SVGException
    {
        if (ltw.createTransformedShape(getBoundingBox()).intersects(pickArea))
        {
            retVec.add(getPath(null));
        }
    }

    public void render(Graphics2D g) throws SVGException
    {
        StyleAttribute styleAttrib = new StyleAttribute();
        if (getStyle(styleAttrib.setName("visibility")))
        {
            if (!styleAttrib.getStringValue().equals("visible")) return;
        }
        
        beginLayer(g);
        
        float opacity = 1f;
        if (getStyle(styleAttrib.setName("opacity")))
        {
            opacity = styleAttrib.getRatioValue();
        }
        
        if (opacity <= 0) return;

        Composite oldComp = null;
        
        if (opacity < 1)
        {
            oldComp = g.getComposite();
            Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g.setComposite(comp);
        }
        
        BufferedImage img = diagram.getUniverse().getImage(imageSrc);
        if (img == null) return;
        
        AffineTransform curXform = g.getTransform();
        g.transform(xform);
        
        g.drawImage(img, 0, 0, null);
        
        g.setTransform(curXform);
        if (oldComp != null) g.setComposite(oldComp);
        
        finishLayer(g);
    }
    
    public Rectangle2D getBoundingBox()
    {
        return boundsToParent(bounds);
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return false;
        boolean changeState = super.updateTime(curTime);

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean shapeChange = false;
        
        if (getPres(sty.setName("x")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x)
            {
                x = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("y")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y)
            {
                y = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("width")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != width)
            {
                width = newVal;
                shapeChange = true;
            }
        }
        
        if (getPres(sty.setName("height")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != height)
            {
                height = newVal;
                shapeChange = true;
            }
        }
        
        try {
            if (getPres(sty.setName("xlink:href")))
            {
                URI src = sty.getURIValue(getXMLBase());
                URL newVal = src.toURL();
                
                if (!newVal.equals(imageSrc))
                {
                    imageSrc = newVal;
                    shapeChange = true;
                }
            }
        }
        catch (IllegalArgumentException ie)
        {
            new Exception("Image provided with illegal value for href: \"" + sty.getStringValue() + '"', ie).printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        
        if (shapeChange)
        {
            build();
//            diagram.getUniverse().registerImage(imageSrc);
//
//            //Set widths if not set
//            BufferedImage img = diagram.getUniverse().getImage(imageSrc);
//            if (img == null)
//            {
//                xform = new AffineTransform();
//                bounds = new Rectangle2D.Float();
//            }
//            else
//            {
//                if (width == 0) width = img.getWidth();
//                if (height == 0) height = img.getHeight();
//
//                //Determine image xform
//                xform = new AffineTransform();
////                xform.setToScale(this.width / img.getWidth(), this.height / img.getHeight());
////                xform.translate(this.x, this.y);
//                xform.translate(this.x, this.y);
//                xform.scale(this.width / img.getWidth(), this.height / img.getHeight());
//
//                bounds = new Rectangle2D.Float(this.x, this.y, this.width, this.height);
//            }
//
//            return true;
        }
        
        return changeState || shapeChange;
    }
}
