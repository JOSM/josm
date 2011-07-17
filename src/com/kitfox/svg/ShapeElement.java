/*
 * ShapeElement.java
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
 * Created on January 26, 2004, 5:21 PM
 */

package com.kitfox.svg;

import com.kitfox.svg.Marker.MarkerLayout;
import com.kitfox.svg.Marker.MarkerPos;
import com.kitfox.svg.xml.StyleAttribute;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;



/**
 * Parent of shape objects
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
abstract public class ShapeElement extends RenderableElement 
{

    /**
     * This is necessary to get text elements to render the stroke the correct
     * width.  It is an alternative to producing new font glyph sets at different
     * sizes.
     */
    protected float strokeWidthScalar = 1f;

    /** Creates a new instance of ShapeElement */
    public ShapeElement() {
    }

    abstract public void render(java.awt.Graphics2D g) throws SVGException;

    /*
    protected void setStrokeWidthScalar(float strokeWidthScalar)
    {
        this.strokeWidthScalar = strokeWidthScalar;
    }
     */

    void pick(Point2D point, boolean boundingBox, List retVec) throws SVGException
    {
        StyleAttribute styleAttrib = new StyleAttribute();
//        if (getStyle(styleAttrib.setName("fill")) && getShape().contains(point))
        if ((boundingBox ? getBoundingBox() : getShape()).contains(point))
        {
            retVec.add(getPath(null));
        }
    }

    void pick(Rectangle2D pickArea, AffineTransform ltw, boolean boundingBox, List retVec) throws SVGException
    {
        StyleAttribute styleAttrib = new StyleAttribute();
//        if (getStyle(styleAttrib.setName("fill")) && getShape().contains(point))
        if (ltw.createTransformedShape((boundingBox ? getBoundingBox() : getShape())).intersects(pickArea))
        {
            retVec.add(getPath(null));
        }
    }

    protected void renderShape(Graphics2D g, Shape shape) throws SVGException
    {
//g.setColor(Color.green);

        StyleAttribute styleAttrib = new StyleAttribute();
        
        //Don't process if not visible
        if (getStyle(styleAttrib.setName("visibility")))
        {
            if (!styleAttrib.getStringValue().equals("visible")) return;
        }

        if (getStyle(styleAttrib.setName("display")))
        {
            if (styleAttrib.getStringValue().equals("none")) return;
        }

        //None, solid color, gradient, pattern
        Paint fillPaint = Color.black;  //Default to black.  Must be explicitly set to none for no fill.
        if (getStyle(styleAttrib.setName("fill")))
        {
            if (styleAttrib.getStringValue().equals("none")) fillPaint = null;
            else
            {
                fillPaint = styleAttrib.getColorValue();
                if (fillPaint == null)
                {
                    URI uri = styleAttrib.getURIValue(getXMLBase());
                    if (uri != null)
                    {
                        Rectangle2D bounds = shape.getBounds2D();
                        AffineTransform xform = g.getTransform();

                        SVGElement ele = diagram.getUniverse().getElement(uri);
                        fillPaint = ((FillElement)ele).getPaint(bounds, xform);
                    }
                }
            }
        }

        //Default opacity
        float opacity = 1f;
        if (getStyle(styleAttrib.setName("opacity")))
        {
            opacity = styleAttrib.getRatioValue();
        }
        
        float fillOpacity = opacity;
        if (getStyle(styleAttrib.setName("fill-opacity")))
        {
            fillOpacity *= styleAttrib.getRatioValue();
        }


        Paint strokePaint = null;  //Default is to stroke with none
        if (getStyle(styleAttrib.setName("stroke")))
        {
            if (styleAttrib.getStringValue().equals("none")) strokePaint = null;
            else
            {
                strokePaint = styleAttrib.getColorValue();
                if (strokePaint == null)
                {
                    URI uri = styleAttrib.getURIValue(getXMLBase());
                    if (uri != null)
                    {
                        Rectangle2D bounds = shape.getBounds2D();
                        AffineTransform xform = g.getTransform();

                        SVGElement ele = diagram.getUniverse().getElement(uri);
                        strokePaint = ((FillElement)ele).getPaint(bounds, xform);
                    }
                }
            }
        }

        float[] strokeDashArray = null;
        if (getStyle(styleAttrib.setName("stroke-dasharray")))
        {
            strokeDashArray = styleAttrib.getFloatList();
            if (strokeDashArray.length == 0) strokeDashArray = null;
        }

        float strokeDashOffset = 0f;
        if (getStyle(styleAttrib.setName("stroke-dashoffset")))
        {
            strokeDashOffset = styleAttrib.getFloatValueWithUnits();
        }

        int strokeLinecap = BasicStroke.CAP_BUTT;
        if (getStyle(styleAttrib.setName("stroke-linecap")))
        {
            String val = styleAttrib.getStringValue();
            if (val.equals("round"))
            {
                strokeLinecap = BasicStroke.CAP_ROUND;
            }
            else if (val.equals("square"))
            {
                strokeLinecap = BasicStroke.CAP_SQUARE;
            }
        }

        int strokeLinejoin = BasicStroke.JOIN_MITER;
        if (getStyle(styleAttrib.setName("stroke-linejoin")))
        {
            String val = styleAttrib.getStringValue();
            if (val.equals("round"))
            {
                strokeLinejoin = BasicStroke.JOIN_ROUND;
            }
            else if (val.equals("bevel"))
            {
                strokeLinejoin = BasicStroke.JOIN_BEVEL;
            }
        }

        float strokeMiterLimit = 4f;
        if (getStyle(styleAttrib.setName("stroke-miterlimit")))
        {
            strokeMiterLimit = Math.max(styleAttrib.getFloatValueWithUnits(), 1);
        }

        float strokeOpacity = opacity;
        if (getStyle(styleAttrib.setName("stroke-opacity")))
        {
            strokeOpacity *= styleAttrib.getRatioValue();
        }

        float strokeWidth = 1f;
        if (getStyle(styleAttrib.setName("stroke-width")))
        {
            strokeWidth = styleAttrib.getFloatValueWithUnits();
        }
//        if (strokeWidthScalar != 1f)
//        {
            strokeWidth *= strokeWidthScalar;
//        }

        Marker markerStart = null;
        if (getStyle(styleAttrib.setName("marker-start")))
        {
            if (!styleAttrib.getStringValue().equals("none"))
            {
                URI uri = styleAttrib.getURIValue(getXMLBase());
                markerStart = (Marker)diagram.getUniverse().getElement(uri);
            }
        }

        Marker markerMid = null;
        if (getStyle(styleAttrib.setName("marker-mid")))
        {
            if (!styleAttrib.getStringValue().equals("none"))
            {
                URI uri = styleAttrib.getURIValue(getXMLBase());
                markerMid = (Marker)diagram.getUniverse().getElement(uri);
            }
        }

        Marker markerEnd = null;
        if (getStyle(styleAttrib.setName("marker-end")))
        {
            if (!styleAttrib.getStringValue().equals("none"))
            {
                URI uri = styleAttrib.getURIValue(getXMLBase());
                markerEnd = (Marker)diagram.getUniverse().getElement(uri);
            }
        }


        //Draw the shape
        if (fillPaint != null && fillOpacity != 0f)
        {
            if (fillOpacity <= 0)
            {
                //Do nothing
            }
            else if (fillOpacity < 1f)
            {
                Composite cachedComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillOpacity));

                g.setPaint(fillPaint);
                g.fill(shape);
            
                g.setComposite(cachedComposite);
            }
            else
            {
                g.setPaint(fillPaint);
                g.fill(shape);
            }
        }


        if (strokePaint != null && strokeOpacity != 0f)
        {
            BasicStroke stroke;
            if (strokeDashArray == null)
            {
                stroke = new BasicStroke(strokeWidth, strokeLinecap, strokeLinejoin, strokeMiterLimit);
            }
            else
            {
                stroke = new BasicStroke(strokeWidth, strokeLinecap, strokeLinejoin, strokeMiterLimit, strokeDashArray, strokeDashOffset);
            }

            Shape strokeShape = stroke.createStrokedShape(shape);

            if (strokeOpacity <= 0)
            {
                //Do nothing
            }
            else if (strokeOpacity < 1f)
            {
                Composite cachedComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strokeOpacity));

                g.setPaint(strokePaint);
                g.fill(strokeShape);

                g.setComposite(cachedComposite);
            }
            else
            {
                g.setPaint(strokePaint);
                g.fill(strokeShape);
            }
        }

        if (markerStart != null || markerMid != null || markerEnd != null)
        {
            MarkerLayout layout = new MarkerLayout();
            layout.layout(shape);
            
            ArrayList list = layout.getMarkerList();
            for (int i = 0; i < list.size(); ++i)
            {
                MarkerPos pos = (MarkerPos)list.get(i);

                switch (pos.type)
                {
                    case Marker.MARKER_START:
                        if (markerStart != null)
                        {
                            markerStart.render(g, pos, strokeWidth);
                        }
                        break;
                    case Marker.MARKER_MID:
                        if (markerMid != null)
                        {
                            markerMid.render(g, pos, strokeWidth);
                        }
                        break;
                    case Marker.MARKER_END:
                        if (markerEnd != null)
                        {
                            markerEnd.render(g, pos, strokeWidth);
                        }
                        break;
                }
            }
        }
    }
    
    abstract public Shape getShape();

    protected Rectangle2D includeStrokeInBounds(Rectangle2D rect) throws SVGException
    {
        StyleAttribute styleAttrib = new StyleAttribute();
        if (!getStyle(styleAttrib.setName("stroke"))) return rect;

        double strokeWidth = 1;
        if (getStyle(styleAttrib.setName("stroke-width"))) strokeWidth = styleAttrib.getDoubleValue();

        rect.setRect(
            rect.getX() - strokeWidth / 2,
            rect.getY() - strokeWidth / 2,
            rect.getWidth() + strokeWidth,
            rect.getHeight() + strokeWidth);

        return rect;
    }

}
