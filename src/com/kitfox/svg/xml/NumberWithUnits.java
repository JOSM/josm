/*
 * NumberWithUnits.java
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
 * Created on February 18, 2004, 2:43 PM
 */

package com.kitfox.svg.xml;

import java.io.Serializable;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class NumberWithUnits implements Serializable
{
    public static final long serialVersionUID = 0;
    
    public static final int UT_UNITLESS = 0;
    public static final int UT_PX = 1;  //Pixels
    public static final int UT_CM = 2;  //Centimeters
    public static final int UT_MM = 3;  //Millimeters
    public static final int UT_IN = 4;  //Inches
    public static final int UT_EM = 5;  //Default font height
    public static final int UT_EX = 6;  //Height of character 'x' in default font
    public static final int UT_PT = 7;  //Points - 1/72 of an inch
    public static final int UT_PC = 8;  //Picas - 1/6 of an inch
    public static final int UT_PERCENT = 9;  //Percent - relative width

    float value = 0f;
    int unitType = UT_UNITLESS;

    /** Creates a new instance of NumberWithUnits */
    public NumberWithUnits()
    {
    }

    public NumberWithUnits(String value)
    {
        set(value);
    }

    public NumberWithUnits(float value, int unitType)
    {
        this.value = value;
        this.unitType = unitType;
    }

    public float getValue() { return value; }
    public int getUnits() { return unitType; }

    public void set(String value)
    {
        this.value = XMLParseUtil.findFloat(value);
        unitType = UT_UNITLESS;

        if (value.indexOf("px") != -1) { unitType = UT_PX; return; }
        if (value.indexOf("cm") != -1) { unitType = UT_CM; return; }
        if (value.indexOf("mm") != -1) { unitType = UT_MM; return; }
        if (value.indexOf("in") != -1) { unitType = UT_IN; return; }
        if (value.indexOf("em") != -1) { unitType = UT_EM; return; }
        if (value.indexOf("ex") != -1) { unitType = UT_EX; return; }
        if (value.indexOf("pt") != -1) { unitType = UT_PT; return; }
        if (value.indexOf("pc") != -1) { unitType = UT_PC; return; }
        if (value.indexOf("%") != -1) { unitType = UT_PERCENT; return; }
    }

    public static String unitsAsString(int unitIdx)
    {
        switch (unitIdx)
        {
            default:
                return "";
            case UT_PX:
                return "px";
            case UT_CM:
                return "cm";
            case UT_MM:
                return "mm";
            case UT_IN:
                return "in";
            case UT_EM:
                return "em";
            case UT_EX:
                return "ex";
            case UT_PT:
                return "pt";
            case UT_PC:
                return "pc";
            case UT_PERCENT:
                return "%";
        }
    }

    public String toString()
    {
        return "" + value + unitsAsString(unitType);
    }

    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumberWithUnits other = (NumberWithUnits) obj;
        if (Float.floatToIntBits(this.value) != Float.floatToIntBits(other.value)) {
            return false;
        }
        if (this.unitType != other.unitType) {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int hash = 5;
        hash = 37 * hash + Float.floatToIntBits(this.value);
        hash = 37 * hash + this.unitType;
        return hash;
    }


}
