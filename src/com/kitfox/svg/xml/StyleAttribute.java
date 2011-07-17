/*
 * StyleAttribute.java
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
 * Created on January 27, 2004, 2:53 PM
 */

package com.kitfox.svg.xml;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class StyleAttribute implements Serializable
{
    public static final long serialVersionUID = 0;

    static final Matcher matchUrl = Pattern.compile("\\s*url\\((.*)\\)\\s*").matcher("");
    static final Matcher matchFpNumUnits = Pattern.compile("\\s*([-+]?((\\d*\\.\\d+)|(\\d+))([-+]?[eE]\\d+)?)\\s*(px|cm|mm|in|pc|pt|em|ex)\\s*").matcher("");
    
    String name;
    String stringValue;

    boolean colorCompatable = false;
    boolean urlCompatable = false;

    /** Creates a new instance of StyleAttribute */
    public StyleAttribute()
    {
        this(null, null);
    }
    
    public StyleAttribute(String name) 
    {
        this.name = name;
        stringValue = null;
    }

    public StyleAttribute(String name, String stringValue) 
    {
        this.name = name;
        this.stringValue = stringValue;
    }

    public String getName() { return name; }
    public StyleAttribute setName(String name) { this.name = name; return this; }
    
    public String getStringValue() { return stringValue; }

    public String[] getStringList() 
    { 
        return XMLParseUtil.parseStringList(stringValue);
    }

    public void setStringValue(String value)
    {
        stringValue = value;
    }

    public boolean getBooleanValue() {
        return stringValue.toLowerCase().equals("true");
    }

    public int getIntValue() {
        return XMLParseUtil.findInt(stringValue);
    }

    public int[] getIntList() {
        return XMLParseUtil.parseIntList(stringValue);
    }

    public double getDoubleValue() {
        return XMLParseUtil.findDouble(stringValue);
    }

    public double[] getDoubleList() {
        return XMLParseUtil.parseDoubleList(stringValue);
    }

    public float getFloatValue() {
        return XMLParseUtil.findFloat(stringValue);
    }

    public float[] getFloatList() {
        return XMLParseUtil.parseFloatList(stringValue);
    }

    public float getRatioValue() {
        return (float)XMLParseUtil.parseRatio(stringValue);
//        try { return Float.parseFloat(stringValue); }
//        catch (Exception e) {}
//        return 0f;
    }

    public String getUnits() {
        matchFpNumUnits.reset(stringValue);
        if (!matchFpNumUnits.matches()) return null;
        return matchFpNumUnits.group(6);
    }

    public NumberWithUnits getNumberWithUnits() {
        return XMLParseUtil.parseNumberWithUnits(stringValue);
    }

    public float getFloatValueWithUnits()
    {
        NumberWithUnits number = getNumberWithUnits();
        return convertUnitsToPixels(number.getUnits(), number.getValue());
    }
    
    static public float convertUnitsToPixels(int unitType, float value)
    {
        if (unitType == NumberWithUnits.UT_UNITLESS || unitType == NumberWithUnits.UT_PERCENT)
        {
            return value;
        }
        
        float pixPerInch;
        try 
        {
            pixPerInch = (float)Toolkit.getDefaultToolkit().getScreenResolution();
        }
        catch (HeadlessException ex)
        {
            //default to 72 dpi
            pixPerInch = 72;
        }
        final float inchesPerCm = .3936f;

        switch (unitType)
        {
            case NumberWithUnits.UT_IN:
                return value * pixPerInch;
            case NumberWithUnits.UT_CM:
                return value * inchesPerCm * pixPerInch;
            case NumberWithUnits.UT_MM:
                return value * .1f * inchesPerCm * pixPerInch;
            case NumberWithUnits.UT_PT:
                return value * (1f / 72f) * pixPerInch;
            case NumberWithUnits.UT_PC:
                return value *  (1f / 6f) * pixPerInch;
        }

        return value;
    }

    public Color getColorValue()
    {
        return ColorTable.parseColor(stringValue);
    }

    public String parseURLFn()
    {
        matchUrl.reset(stringValue);
        if (!matchUrl.matches()) return null;
        return matchUrl.group(1);
    }

    public URL getURLValue(URL docRoot)
    {
        String fragment = parseURLFn();
        if (fragment == null) return null;
        try {
            return new URL(docRoot, fragment);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public URL getURLValue(URI docRoot)
    {
        String fragment = parseURLFn();
        if (fragment == null) return null;
        try {
            URI ref = docRoot.resolve(fragment);
            return ref.toURL();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public URI getURIValue()
    {
        return getURIValue(null);
    }
    
    /**
     * Parse this sytle attribute as a URL and return it in URI form resolved
     * against the passed base.
     *
     * @param base - URI to resolve against.  If null, will return value without
     * attempting to resolve it.
     */
    public URI getURIValue(URI base)
    {
        try {
            String fragment = parseURLFn();
            if (fragment == null) fragment = stringValue.replaceAll("\\s+", "");
            if (fragment == null) return null;
            
            //======================
            //This gets around a bug in the 1.5.0 JDK
            if (Pattern.matches("[a-zA-Z]:!\\\\.*", fragment))
            {
                File file = new File(fragment);
                return file.toURI();
            }
            //======================

            //[scheme:]scheme-specific-part[#fragment]
            
            URI uriFrag = new URI(fragment);
            if (uriFrag.isAbsolute())
            {
                //Has scheme
                return uriFrag;
            }
        
            if (base == null) return uriFrag;
        
            URI relBase = new URI(null, base.getSchemeSpecificPart(), null);
            URI relUri;
            if (relBase.isOpaque())
            {
                relUri = new URI(null, base.getSchemeSpecificPart(), uriFrag.getFragment());
            }
            else
            {
                relUri = relBase.resolve(uriFrag);
            }
            return new URI(base.getScheme() + ":" + relUri);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void main(String[] args)
    {
        try
        {
            URI uri = new URI("jar:http://www.kitfox.com/jackal/jackal.jar!/res/doc/about.svg");
            uri = uri.resolve("#myFragment");
            
            System.err.println(uri.toString());
            
            uri = new URI("http://www.kitfox.com/jackal/jackal.html");
            uri = uri.resolve("#myFragment");
            
            System.err.println(uri.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
