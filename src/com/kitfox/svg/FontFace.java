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

import java.awt.geom.*;
import java.awt.*;


/**
 * Implements an embedded font.
 *
 * SVG specification: http://www.w3.org/TR/SVG/fonts.html
 *
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class FontFace extends SVGElement
{
    String fontFamily;

    /** Em size of coordinate system font is defined in */
    int unitsPerEm = 1000;

    int ascent = -1;
    int descent = -1;
    int accentHeight = -1;

    int underlinePosition = -1;
    int underlineThickness = -1;
    int strikethroughPosition = -1;
    int strikethroughThickness = -1;
    int overlinePosition = -1;
    int overlineThickness = -1;

    /** Creates a new instance of Font */
    public FontFace()
    {
    }
/*
    public void loaderStartElement(SVGLoaderHelper helper, Attributes attrs, SVGElement parent)
    {
		//Load style string
        super.loaderStartElement(helper, attrs, parent);

        fontFamily = attrs.getValue("font-family");

        String unitsPerEm = attrs.getValue("units-per-em");
        String ascent = attrs.getValue("ascent");
        String descent = attrs.getValue("descent");
        String accentHeight = attrs.getValue("accent-height");

        String underlinePosition = attrs.getValue("underline-position");
        String underlineThickness = attrs.getValue("underline-thickness");
        String strikethroughPosition = attrs.getValue("strikethrough-position");
        String strikethroughThickness = attrs.getValue("strikethrough-thickness");
        String overlinePosition = attrs.getValue("overline-position");
        String overlineThickness = attrs.getValue("overline-thickness");


        if (unitsPerEm != null) this.unitsPerEm = XMLParseUtil.parseInt(unitsPerEm);
        if (ascent != null) this.ascent = XMLParseUtil.parseInt(ascent);
        if (descent != null) this.descent = XMLParseUtil.parseInt(descent);
        if (accentHeight != null) this.accentHeight = XMLParseUtil.parseInt(accentHeight);

        if (underlinePosition != null) this.underlinePosition = XMLParseUtil.parseInt(underlinePosition);
        if (underlineThickness != null) this.underlineThickness = XMLParseUtil.parseInt(underlineThickness);
        if (strikethroughPosition != null) this.strikethroughPosition = XMLParseUtil.parseInt(strikethroughPosition);
        if (strikethroughThickness != null) this.strikethroughThickness = XMLParseUtil.parseInt(strikethroughThickness);
        if (overlinePosition != null) this.overlinePosition = XMLParseUtil.parseInt(overlinePosition);
        if (overlineThickness != null) this.overlineThickness = XMLParseUtil.parseInt(overlineThickness);

//        unitFontXform.setToScale(1.0 / (double)unitsPerEm, 1.0 / (double)unitsPerEm);
    }
  */  
    /*
    public void loaderEndElement(SVGLoaderHelper helper)
    {
        super.loaderEndElement(helper);

        build();
        
//        unitFontXform.setToScale(1.0 / (double)unitsPerEm, 1.0 / (double)unitsPerEm);
    }
     */
    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        
        if (getPres(sty.setName("font-family"))) fontFamily = sty.getStringValue();
        
        if (getPres(sty.setName("units-per-em"))) unitsPerEm = sty.getIntValue();
        if (getPres(sty.setName("ascent"))) ascent = sty.getIntValue();
        if (getPres(sty.setName("descent"))) descent = sty.getIntValue();
        if (getPres(sty.setName("accent-height"))) accentHeight = sty.getIntValue();

        if (getPres(sty.setName("underline-position"))) underlinePosition = sty.getIntValue();
        if (getPres(sty.setName("underline-thickness"))) underlineThickness = sty.getIntValue();
        if (getPres(sty.setName("strikethrough-position"))) strikethroughPosition = sty.getIntValue();
        if (getPres(sty.setName("strikethrough-thickenss"))) strikethroughThickness = sty.getIntValue();
        if (getPres(sty.setName("overline-position"))) overlinePosition = sty.getIntValue();
        if (getPres(sty.setName("overline-thickness"))) overlineThickness = sty.getIntValue();
    }


    public String getFontFamily() { return fontFamily; }

    public int getUnitsPerEm() { return unitsPerEm; }

    public int getAscent()
    {
        if (ascent == -1)
            ascent = unitsPerEm - ((Font)parent).getVertOriginY();
        return ascent;
    }

    public int getDescent()
    {
        if (descent == -1)
            descent = ((Font)parent).getVertOriginY();
        return descent;
    }

    public int getAccentHeight()
    {
        if (accentHeight == -1)
            accentHeight = getAscent();
        return accentHeight;
    }

    public int getUnderlinePosition()
    {
        if (underlinePosition == -1)
            underlinePosition = unitsPerEm * 5 / 6;
        return underlinePosition;
    }

    public int getUnderlineThickness()
    {
        if (underlineThickness == -1)
            underlineThickness = unitsPerEm / 20;
        return underlineThickness;
    }

    public int getStrikethroughPosition()
    {
        if (strikethroughPosition == -1)
            strikethroughPosition = unitsPerEm * 3 / 6;
        return strikethroughPosition;
    }

    public int getStrikethroughThickness()
    {
        if (strikethroughThickness == -1)
            strikethroughThickness = unitsPerEm / 20;
        return strikethroughThickness;
    }

    public int getOverlinePosition()
    {
        if (overlinePosition == -1)
            overlinePosition = unitsPerEm * 5 / 6;
        return overlinePosition;
    }

    public int getOverlineThickness()
    {
        if (overlineThickness == -1)
            overlineThickness = unitsPerEm / 20;
        return overlineThickness;
    }
    
    /**
     * Updates all attributes in this diagram associated with a time event.
     * Ie, all attributes with track information.
     * @return - true if this node has changed state as a result of the time
     * update
     */
    public boolean updateTime(double curTime)
    {
        //Fonts can't change
        return false;
    }
}
