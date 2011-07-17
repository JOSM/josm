/*
 * FillElement.java
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
 * Created on March 18, 2004, 6:52 AM
 */

package com.kitfox.svg;

import com.kitfox.svg.xml.StyleAttribute;
import java.awt.*;
import java.awt.geom.*;
import java.net.*;
import java.util.*;

import com.kitfox.svg.xml.*;
import org.xml.sax.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class FeSpotLight extends FeLight 
{
    float x = 0f;
    float y = 0f;
    float z = 0f;
    float pointsAtX = 0f;
    float pointsAtY = 0f;
    float pointsAtZ = 0f;
    float specularComponent = 0f;
    float limitingConeAngle = 0f;
    

    /** Creates a new instance of FillElement */
    public FeSpotLight() {
    }

    
    protected void build() throws SVGException
    {
        super.build();
        
        StyleAttribute sty = new StyleAttribute();
        String strn;
        
        if (getPres(sty.setName("x"))) x = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("y"))) y = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("z"))) z = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("pointsAtX"))) pointsAtX = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("pointsAtY"))) pointsAtY = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("pointsAtZ"))) pointsAtZ = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("specularComponent"))) specularComponent = sty.getFloatValueWithUnits();
        if (getPres(sty.setName("limitingConeAngle"))) limitingConeAngle = sty.getFloatValueWithUnits();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getPointsAtX() { return pointsAtX; }
    public float getPointsAtY() { return pointsAtY; }
    public float getPointsAtZ() { return pointsAtZ; }
    public float getSpecularComponent() { return specularComponent; }
    public float getLimitingConeAngle() { return limitingConeAngle; }
    
    public boolean updateTime(double curTime) throws SVGException
    {
//        if (trackManager.getNumTracks() == 0) return false;

        //Get current values for parameters
        StyleAttribute sty = new StyleAttribute();
        boolean stateChange = false;
        
        if (getPres(sty.setName("x")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != x)
            {
                x = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("y")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != y)
            {
                y = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("z")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != z)
            {
                z = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("pointsAtX")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != pointsAtX)
            {
                pointsAtX = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("pointsAtY")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != pointsAtY)
            {
                pointsAtY = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("pointsAtZ")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != pointsAtZ)
            {
                pointsAtZ = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("specularComponent")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != specularComponent)
            {
                specularComponent = newVal;
                stateChange = true;
            }
        }
        
        if (getPres(sty.setName("limitingConeAngle")))
        {
            float newVal = sty.getFloatValueWithUnits();
            if (newVal != limitingConeAngle)
            {
                limitingConeAngle = newVal;
                stateChange = true;
            }
        }
        
        return stateChange;
    }
}

