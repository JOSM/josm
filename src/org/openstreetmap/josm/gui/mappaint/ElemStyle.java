package org.openstreetmap.josm.gui.mappaint;

abstract public class ElemStyle
{
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public int priority;
    public String code;

    public Boolean equals(ElemStyle s)
    {
        return s != null && s.code.equals(code);
    }
}



