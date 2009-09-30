/* Copyright (c) 2008, Henrik Niehaus
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

public class ComboBoxHistory extends DefaultComboBoxModel implements Iterable<String> {

    private int maxSize = 10;

    public ComboBoxHistory(int size) {
        maxSize = size;
    }

    /**
     * Adds or moves an element to the top of the history
     */
    @Override
    public void addElement(Object o) {
        String newEntry = (String)o;

        // if history contains this object already, delete it,
        // so that it looks like a move to the top
        for (int i = 0; i < getSize(); i++) {
            String oldEntry = (String) getElementAt(i);
            if(oldEntry.equals(newEntry)) {
                removeElementAt(i);
            }
        }

        // insert element at the top
        insertElementAt(o, 0);

        // remove an element, if the history gets too large
        if(getSize()> maxSize) {
            removeElementAt(getSize()-1);
        }

        // set selected item to the one just added
        setSelectedItem(o);
    }

    public Iterator<String> iterator() {
        return new Iterator<String>() {

            private int position = -1;

            public void remove() {
                removeElementAt(position);
            }

            public boolean hasNext() {
                if(position < getSize()-1 && getSize()>0)
                    return true;
                return false;
            }

            public String next() {
                position++;
                return getElementAt(position).toString();
            }

        };
    }

    public void setItems(List<String> items) {
        removeAllElements();
        Collections.reverse(items);
        for (String item : items) {
            addElement(item);
        }
        Collections.reverse(items);
    }

    public List<String> asList() {
        List<String> list = new ArrayList<String>(maxSize);
        for (String item : this) {
            list.add(item);
        }
        return list;
    }
}
