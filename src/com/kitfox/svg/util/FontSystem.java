/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kitfox.svg.util;

import com.kitfox.svg.Font;
import com.kitfox.svg.FontFace;
import com.kitfox.svg.Glyph;
import com.kitfox.svg.MissingGlyph;
import java.awt.Canvas;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.util.HashMap;

/**
 *
 * @author kitfox
 */
public class FontSystem extends Font
{
    java.awt.Font sysFont;
    FontMetrics fm;

    HashMap glyphCache = new HashMap();
    
    public FontSystem(String fontFamily, int fontStyle, int fontWeight, int fontSize)
    {
        int style;
        switch (fontStyle)
        {
            case com.kitfox.svg.Text.TXST_ITALIC:
                style = java.awt.Font.ITALIC;
                break;
            default:
                style = java.awt.Font.PLAIN;
                break;
        }

        int weight;
        switch (fontWeight)
        {
            case com.kitfox.svg.Text.TXWE_BOLD:
            case com.kitfox.svg.Text.TXWE_BOLDER:
                weight = java.awt.Font.BOLD;
                break;
            default:
                weight = java.awt.Font.PLAIN;
                break;
        }
        sysFont = new java.awt.Font(fontFamily, style | weight, (int) fontSize);
        
        Canvas c = new Canvas();
        fm = c.getFontMetrics(sysFont);
        
        FontFace face = new FontFace();
        face.setAscent(fm.getAscent());
        face.setDescent(fm.getDescent());
        face.setUnitsPerEm(fm.charWidth('M'));
        setFontFace(face);
    }

    public MissingGlyph getGlyph(String unicode)
    {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector vec = sysFont.createGlyphVector(frc, unicode);
        
        Glyph glyph = (Glyph)glyphCache.get(unicode);
        if (glyph == null)
        {
            glyph = new Glyph();
            glyph.setPath(vec.getGlyphOutline(0));

            GlyphMetrics gm = vec.getGlyphMetrics(0);
            glyph.setHorizAdvX((int)gm.getAdvanceX());
            glyph.setVertAdvY((int)gm.getAdvanceY());
            glyph.setVertOriginX(0);
            glyph.setVertOriginY(0);
            
            glyphCache.put(unicode, glyph);
        }
        
        return glyph;
    }
    
    
}
