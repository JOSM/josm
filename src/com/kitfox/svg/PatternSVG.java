/*
 * Gradient.java
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
 * Created on January 26, 2004, 3:25 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.net.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.*;
import java.awt.image.*;

import com.kitfox.svg.pattern.*;
import com.kitfox.svg.xml.*;
import org.xml.sax.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class PatternSVG extends FillElement {

    public static final int GU_OBJECT_BOUNDING_BOX = 0;
    public static final int GU_USER_SPACE_ON_USE = 1;

    int gradientUnits = GU_OBJECT_BOUNDING_BOX;

    float x;
    float y;
    float width;
    float height;

    AffineTransform patternXform = new AffineTransform();
    Rectangle2D.Float viewBox;

    Paint texPaint;

    /** Creates a new instance of Gradient */
    public PatternSVG() {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String href = attrs.getValue("xlink:href");
        //If we have a link to another pattern, initialize ourselves with it's values
        if (href != null)
        {
//System.err.println("Gradient.loaderStartElement() href '" + href + "'");
            try {
                URI src = getXMLBase().resolve(href);
//                URL url = srcUrl.toURL();
//                URL url = new URL(helper.docRoot, href);
                PatternSVG patSrc = (PatternSVG)helper.universe.getElement(src);

                gradientUnits = patSrc.gradientUnits;
                x = patSrc.x;
                y = patSrc.y;
                width = patSrc.width;
                height = patSrc.height;
                viewBox = patSrc.viewBox;
                patternXform.setTransform(patSrc.patternXform);
                members.addAll(patSrc.members);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }


        String gradientUnits = attrs.getValue("gradientUnits");

        if (gradientUnits != null)
        {
            if (gradientUnits.toLowerCase().equals("userspaceonuse")) this.gradientUnits = GU_USER_SPACE_ON_USE;
            else this.gradientUnits = GU_OBJECT_BOUNDING_BOX;
        }

        String patternTransform = attrs.getValue("patternTransform");
        if (patternTransform != null)
        {
            patternXform = parseTransform(patternTransform);
        }

        String x = attrs.getValue("x");
        String y = attrs.getValue("y");
        String width = attrs.getValue("width");
        String height = attrs.getValue("height");

        if (x != null) this.x = XMLParseUtil.parseFloat(x);
        if (y != null) this.y = XMLParseUtil.parseFloat(y);
        if (width != null) this.width = XMLParseUtil.parseFloat(width);
        if (height != null) this.height = XMLParseUtil.parseFloat(height);

        String viewBoxStrn = attrs.getValue("viewBox");
        if (viewBoxStrn != null)
        {
            float[] dim = XMLParseUtil.parseFloatList(viewBoxStrn);
            viewBox = new Rectangle2D.Float(dim[0], dim[1], dim[2], dim[3]);
        }
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
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
		//Load style string
        String href = null;
        if (getPres(sty.setName("xlink:href"))) href = sty.getStringValue();
        //String href = attrs.getValue("xlink:href");
        //If we have a link to another pattern, initialize ourselves with it's values
        if (href != null)
        {
//System.err.println("Gradient.loaderStartElement() href '" + href + "'");
            try {
                URI src = getXMLBase().resolve(href);
                PatternSVG patSrc = (PatternSVG)diagram.getUniverse().getElement(src);

                gradientUnits = patSrc.gradientUnits;
                x = patSrc.x;
                y = patSrc.y;
                width = patSrc.width;
                height = patSrc.height;
                viewBox = patSrc.viewBox;
                patternXform.setTransform(patSrc.patternXform);
                children.addAll(patSrc.children);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        String gradientUnits = "";
        if (getPres(sty.setName("gradientUnits"))) gradientUnits = sty.getStringValue().toLowerCase();
        if (gradientUnits.equals("userspaceonuse")) this.gradientUnits = GU_USER_SPACE_ON_USE;
        else this.gradientUnits = GU_OBJECT_BOUNDING_BOX;

        String patternTransform = "";
        if (getPres(sty.setName("patternTransform"))) patternTransform = sty.getStringValue();
        patternXform = parseTransform(patternTransform);

        
        if (getPres(sty.setName("x"))) x = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("y"))) y = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("width"))) width = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("height"))) height = sty.getFloatValueWithUnits();
        
        if (getPres(sty.setName("viewBox")))
        {
            float[] dim = sty.getFloatList();
            viewBox = new Rectangle2D.Float(dim[0], dim[1], dim[2], dim[3]);
        }
            
        preparePattern();
    }
    
/*
    public void loaderEndElement(SVGLoaderHelper helper)
    {
        build();
    }
    */

    protected void preparePattern() throws SVGException
    {
        //For now, treat all fills as UserSpaceOnUse.  Otherwise, we'll need
        // a different paint for every object.
        int tileWidth = (int)width;
        int tileHeight = (int)height;

        float stretchX = 1f, stretchY = 1f;
        if (!patternXform.isIdentity())
        {
            //Scale our source tile so that we can have nice sampling from it.
            float xlateX = (float)patternXform.getTranslateX();
            float xlateY = (float)patternXform.getTranslateY();

            Point2D.Float pt = new Point2D.Float(), pt2 = new Point2D.Float();

            pt.setLocation(width, 0);
            patternXform.transform(pt, pt2);
            pt2.x -= xlateX;
            pt2.y -= xlateY;
            stretchX = (float)Math.sqrt(pt2.x * pt2.x + pt2.y * pt2.y) * 1.5f / width;

            pt.setLocation(height, 0);
            patternXform.transform(pt, pt2);
            pt2.x -= xlateX;
            pt2.y -= xlateY;
            stretchY = (float)Math.sqrt(pt2.x * pt2.x + pt2.y * pt2.y) * 1.5f / height;

            tileWidth *= stretchX;
            tileHeight *= stretchY;
        }

        if (tileWidth == 0 || tileHeight == 0) 
        {
            //Use defaults if tile has degenerate size
            return;
        }
        
        BufferedImage buf = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.setClip(0, 0, tileWidth, tileHeight);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Iterator it = children.iterator(); it.hasNext();)
        {
            SVGElement ele = (SVGElement)it.next();
            if (ele instanceof RenderableElement)
            {
                AffineTransform xform = new AffineTransform();

                if (viewBox == null)
                {
                    xform.translate(-x, -y);
                }
                else
                {
                    xform.scale(tileWidth / viewBox.width, tileHeight / viewBox.height);
                    xform.translate(-viewBox.x, -viewBox.y);
                }

                g.setTransform(xform);
                ((RenderableElement)ele).render(g);
            }
        }

        g.dispose();

//try {
//javax.imageio.ImageIO.write(buf, "png", new java.io.File("c:\\tmp\\texPaint.png"));
//} catch (Exception e ) {}

        if (patternXform.isIdentity())
        {
            texPaint = new TexturePaint(buf, new Rectangle2D.Float(x, y, width, height));
        }
        else
        {
            patternXform.scale(1 / stretchX, 1 / stretchY);
            texPaint = new PatternPaint(buf, patternXform);
        }
    }

    public Paint getPaint(Rectangle2D bounds, AffineTransform xform)
    {
        return texPaint;
    }

    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
        //Patterns don't change state
        return false;
    }
}
