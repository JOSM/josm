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
package org.openstreetmap.josm.gui.historycombobox;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;

/**
 * Extends the standard JComboBox with a history. Works with Strings only. 
 * @author henni
 *
 */
public class JHistoryComboBox extends JComboBox implements ActionListener {

    public static String DELIM = "§§§";
    
    protected ComboBoxHistory model;
    
    /**
     * Default constructor for GUI editors. Don't use this!!!
     */
    public JHistoryComboBox() {}
    
    /**
     * @param history the history as a list of strings
     */
    public JHistoryComboBox(List<String> history) {
        model = new ComboBoxHistory(15);
        setModel(model);
        getEditor().addActionListener(this);
        setEditable(true);
        setHistory(history);
    }
    
    public void actionPerformed(ActionEvent e) {
        addCurrentItemToHistory();
    }

    public void addCurrentItemToHistory() {
        String regex = (String)getEditor().getItem();
        model.addElement(regex);
    }
    
    public void setText(String text) {
    	getEditor().setItem(text);
    }
    
    public String getText() {
    	return getEditor().getItem().toString();
    }
    
    public void addHistoryChangedListener(HistoryChangedListener l) {
        model.addHistoryChangedListener(l);
    }
    
    public void removeHistoryChangedListener(HistoryChangedListener l) {
        model.removeHistoryChangedListener(l);
    }
    
    public void setHistory(List<String> history) {
        model.setItems(history);
    }
    
    public List<String> getHistory() {
        return model.asList();
    }
}
