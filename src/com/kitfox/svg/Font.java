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
import com.kitfox.svg.xml.*;
import org.xml.sax.*;

import java.util.*;

/**
 * Implements an embedded font.
 *
 * SVG specification: http://www.w3.org/TR/SVG/fonts.html
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class Font extends SVGElement
{
    int horizOriginX = 0;
    int horizOriginY = 0;
    int horizAdvX = -1;  //Must be specified
    int vertOriginX = -1;  //Defaults to horizAdvX / 2
    int vertOriginY = -1;  //Defaults to font's ascent
    int vertAdvY = -1;  //Defaults to one 'em'.  See font-face

    FontFace fontFace = null;
    MissingGlyph missingGlyph = null;
    final HashMap glyphs = new HashMap();

    /** Creates a new instance of Font */
    public Font()
    {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        String horizOriginX = attrs.getValue("horiz-origin-x");
        String horizOriginY = attrs.getValue("horiz-origin-y");
        String horizAdvX = attrs.getValue("horiz-adv-x");
        String vertOriginX = attrs.getValue("vert-origin-x");
        String vertOriginY = attrs.getValue("vert-origin-y");
        String vertAdvY = attrs.getValue("vert-adv-y");

        if (horizOriginX != null) this.horizOriginX = XMLParseUtil.parseInt(horizOriginX);
        if (horizOriginY != null) this.horizOriginY = XMLParseUtil.parseInt(horizOriginY);
        if (horizAdvX != null) this.horizAdvX = XMLParseUtil.parseInt(horizAdvX);
        if (vertOriginX != null) this.vertOriginX = XMLParseUtil.parseInt(vertOriginX);
        if (vertOriginY != null) this.vertOriginY = XMLParseUtil.parseInt(vertOriginY);
        if (vertAdvY != null) this.vertAdvY = XMLParseUtil.parseInt(vertAdvY);

    }
*/
    /**
     * Called after the start element but before the end element to indicate
     * each child tag that has been processed
     */
    public void loaderAddChild(SVGLoaderHelper helper, SVGElement child) throws SVGElementException
    {
		super.loaderAddChild(helper, child);

        if (child instanceof Glyph)
        {
            glyphs.put(((Glyph)child).getUnicode(), child);
        }
        else if (child instanceof MissingGlyph)
        {
            missingGlyph = (MissingGlyph)child;
        }
        else if (child instanceof FontFace)
        {
            fontFace = (FontFace)child;
        }
    }

    public void loaderEndElement(SVGLoaderHelper helper) throws SVGParseException
    {
        super.loaderEndElement(helper);

        //build();
        
        helper.universe.registerFont(this);
    }
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("horiz-origin-x"))) horizOriginX = sty.getIntValue();
        
        if (getPres(sty.setName("horiz-origin-y"))) horizOriginY = sty.getIntValue();
        
        if (getPres(sty.setName("horiz-adv-x"))) horizAdvX = sty.getIntValue();
        
        if (getPres(sty.setName("vert-origin-x"))) vertOriginX = sty.getIntValue();
        
        if (getPres(sty.setName("vert-origin-y"))) vertOriginY = sty.getIntValue();
        
        if (getPres(sty.setName("vert-adv-y"))) vertAdvY = sty.getIntValue();
    }
    
    public FontFace getFontFace() { return fontFace; }

    public MissingGlyph getGlyph(String unicode)
    {
        Glyph retVal = (Glyph)glyphs.get(unicode);
        if (retVal == null) return missingGlyph;
        return retVal;
    }

    public int getHorizOriginX() { return horizOriginX; }
    public int getHorizOriginY() { return horizOriginY; }
    public int getHorizAdvX() { return horizAdvX; }

    public int getVertOriginX()
    {
        if (vertOriginX != -1) return vertOriginX;
        vertOriginX = getHorizAdvX() / 2;
        return vertOriginX;
    }

    public int getVertOriginY()
    {
        if (vertOriginY != -1) return vertOriginY;
        vertOriginY = fontFace.getAscent();
        return vertOriginY;
    }

    public int getVertAdvY()
    {
        if (vertAdvY != -1) return vertAdvY;
        vertAdvY = fontFace.getUnitsPerEm();
        return vertAdvY;
    }
    
    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime) throws SVGException
    {
        //Fonts can't change
        return false;
        /*
        if (trackManager.getNumTracks() == 0) return false;
        
        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean stateChange = false;
        
        if (getPres(sty.setName("horiz-origin-x")))
        {
            int newVal = sty.getIntValue();
            if (newVal != horizOriginX)
            {
                horizOriginX = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("horiz-origin-y")))
        {
            int newVal = sty.getIntValue();
            if (newVal != horizOriginY)
            {
                horizOriginY = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("horiz-adv-x")))
        {
            int newVal = sty.getIntValue();
            if (newVal != horizAdvX)
            {
                horizAdvX = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("vert-origin-x")))
        {
            int newVal = sty.getIntValue();
            if (newVal != vertOriginX)
            {
                vertOriginX = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("vert-origin-y")))
        {
            int newVal = sty.getIntValue();
            if (newVal != vertOriginY)
            {
                vertOriginY = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("vert-adv-y")))
        {
            int newVal = sty.getIntValue();
            if (newVal != vertAdvY)
            {
                vertAdvY = newVal;
                stateChange = true;
            }
        }
        
        return shapeChange;
        */
    }
}
