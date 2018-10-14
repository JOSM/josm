/*
 * SVG Salamander
 * Copyright (c) 2004, Mark McKay
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 *   - Redistributions of source code must retain the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 * projects can be found at http://www.kitfox.com
 *
 * Created on February 18, 2004, 1:49 PM
 */

package com.kitfox.svg.xml;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kitfox.svg.SVGConst;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class XMLParseUtil
{
    static final Matcher fpMatch = Pattern.compile("([-+]?((\\d*\\.\\d+)|(\\d+))([eE][+-]?\\d+)?)(\\%|in|cm|mm|pt|pc|px|em|ex)?").matcher("");
    static final Matcher intMatch = Pattern.compile("[-+]?\\d+").matcher("");
    static final Matcher quoteMatch = Pattern.compile("^'|'$").matcher("");

    /** Creates a new instance of XMLParseUtil */
    private XMLParseUtil()
    {
    }

    public static String[] parseStringList(String list)
    {
        final Matcher matchWs = Pattern.compile("[^\\s]+").matcher("");
        matchWs.reset(list);

        LinkedList<String> matchList = new LinkedList<>();
        while (matchWs.find())
        {
            matchList.add(matchWs.group());
        }

        String[] retArr = new String[matchList.size()];
        return matchList.toArray(retArr);
    }

    public static double parseDouble(String val)
    {
        return findDouble(val);
    }

    /**
     * Searches the given string for the first floating point number it contains,
     * parses and returns it.
     */
    public synchronized static double findDouble(String val)
    {
        if (val == null) return 0;

        fpMatch.reset(val);
        try
        {
            if (!fpMatch.find()) return 0;
        }
        catch (StringIndexOutOfBoundsException e)
        {
            Logger.getLogger(SVGConst.SVG_LOGGER).log(Level.WARNING,
                "XMLParseUtil: regex parse problem: '" + val + "'", e);
        }

        val = fpMatch.group(1);
        //System.err.println("Parsing " + val);

        double retVal = 0;
        try
        {
            retVal = Double.parseDouble(val);

            float pixPerInch;
            try {
                pixPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
            }
            catch (NoClassDefFoundError err)
            {
                //Default value for headless X servers
                pixPerInch = 72;
            }
            final float inchesPerCm = .3936f;
            final String units = fpMatch.group(6);

            if ("%".equals(units)) retVal /= 100;
            else if ("in".equals(units))
            {
                retVal *= pixPerInch;
            }
            else if ("cm".equals(units))
            {
                retVal *= inchesPerCm * pixPerInch;
            }
            else if ("mm".equals(units))
            {
                retVal *= inchesPerCm * pixPerInch * .1f;
            }
            else if ("pt".equals(units))
            {
                retVal *= (1f / 72f) * pixPerInch;
            }
            else if ("pc".equals(units))
            {
                retVal *= (1f / 6f) * pixPerInch;
            }
        }
        catch (Exception e)
        {}
        return retVal;
    }

    /**
     * Scans an input string for double values.  For each value found, places
     * in a list.  This method regards any characters not part of a floating
     * point value to be seperators.  Thus this will parse whitespace seperated,
     * comma seperated, and many other separation schemes correctly.
     */
    public synchronized static double[] parseDoubleList(String list)
    {
        if (list == null) return null;

        fpMatch.reset(list);

        LinkedList<Double> doubList = new LinkedList<>();
        while (fpMatch.find())
        {
            String val = fpMatch.group(1);
            doubList.add(Double.valueOf(val));
        }

        double[] retArr = new double[doubList.size()];
        Iterator<Double> it = doubList.iterator();
        int idx = 0;
        while (it.hasNext())
        {
            retArr[idx++] = it.next().doubleValue();
        }

        return retArr;
    }

    /**
     * Searches the given string for the first floating point number it contains,
     * parses and returns it.
     */
    public synchronized static float findFloat(String val)
    {
        if (val == null) return 0f;

        fpMatch.reset(val);
        if (!fpMatch.find()) return 0f;

        val = fpMatch.group(1);
        //System.err.println("Parsing " + val);

        float retVal = 0f;
        try
        {
            retVal = Float.parseFloat(val);
            String units = fpMatch.group(6);
            if ("%".equals(units)) retVal /= 100;
        }
        catch (Exception e)
        {}
        return retVal;
    }

    public synchronized static float[] parseFloatList(String list)
    {
        if (list == null) return null;

        fpMatch.reset(list);

        LinkedList<Float> floatList = new LinkedList<>();
        while (fpMatch.find())
        {
            String val = fpMatch.group(1);
            floatList.add(Float.valueOf(val));
        }

        float[] retArr = new float[floatList.size()];
        Iterator<Float> it = floatList.iterator();
        int idx = 0;
        while (it.hasNext())
        {
            retArr[idx++] = it.next().floatValue();
        }

        return retArr;
    }

    /**
     * Searches the given string for the first integer point number it contains,
     * parses and returns it.
     */
    public static int findInt(String val)
    {
        if (val == null) return 0;

        intMatch.reset(val);
        if (!intMatch.find()) return 0;

        val = intMatch.group();

        int retVal = 0;
        try
        { retVal = Integer.parseInt(val); }
        catch (Exception e)
        {}
        return retVal;
    }

    public static int[] parseIntList(String list)
    {
        if (list == null) return null;

        intMatch.reset(list);

        LinkedList<Integer> intList = new LinkedList<>();
        while (intMatch.find())
        {
            String val = intMatch.group();
            intList.add(Integer.valueOf(val));
        }

        int[] retArr = new int[intList.size()];
        Iterator<Integer> it = intList.iterator();
        int idx = 0;
        while (it.hasNext())
        {
            retArr[idx++] = it.next().intValue();
        }

        return retArr;
    }

    /**
     * The input string represents a ratio.  Can either be specified as a
     * double number on the range of [0.0 1.0] or as a percentage [0% 100%]
     */
    public static double parseRatio(String val)
    {
        if (val == null || val.equals("")) return 0.0;

        if (val.charAt(val.length() - 1) == '%')
        {
            parseDouble(val.substring(0, val.length() - 1));
        }
        return parseDouble(val);
    }

    public static NumberWithUnits parseNumberWithUnits(String val)
    {
        if (val == null) return null;

        return new NumberWithUnits(val);
    }

    /**
     * Takes a CSS style string and returns a hash of them.
     * @param styleString - A CSS formatted string of styles.  Eg,
     *     "font-size:12;fill:#d32c27;fill-rule:evenodd;stroke-width:1pt;"
     * @param map - A map to which these styles will be added
     */
    public static HashMap<String, StyleAttribute> parseStyle(String styleString, HashMap<String, StyleAttribute> map) {
        final Pattern patSemi = Pattern.compile(";");

        String[] styles = patSemi.split(styleString);

        for (int i = 0; i < styles.length; i++)
        {
            if (styles[i].length() == 0)
            {
                continue;
            }

            int colon = styles[i].indexOf(':');
            if (colon == -1)
            {
                continue;
            }

            String key = styles[i].substring(0, colon).trim();
            String value = quoteMatch.reset(styles[i].substring(colon + 1).trim()).replaceAll("");

            map.put(key, new StyleAttribute(key, value));
        }

        return map;
    }
}
