// License: GPL. Copyright 2007 by Tim Haussmann
package org.openstreetmap.josm.gui.bbox;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * @author Tim Haussmann
 */
public class SizeButton{

    private int x = 0;
    private int y = 0;

    private ImageIcon enlargeImage;
    private ImageIcon shrinkImage;
    private boolean isEnlarged = false;

    public SizeButton(){
        enlargeImage = ImageProvider.get("view-fullscreen.png");
        shrinkImage = ImageProvider.get("view-fullscreen-revert.png");
    }

    public void paint(Graphics g){
        if(isEnlarged){
            if(shrinkImage != null)
                g.drawImage(shrinkImage.getImage(),x,y, null);
        }else{
            if(enlargeImage != null)
                g.drawImage(enlargeImage.getImage(),x,y, null);
        }
    }

    public void toggle(){
        isEnlarged = !isEnlarged;
    }

    public boolean hit(Point point){
        if(x < point.x && point.x < x + enlargeImage.getIconWidth()){
            if(y < point.y && point.y < y + enlargeImage.getIconHeight() ){
                return true;
            }
        }
        return false;
    }

}
