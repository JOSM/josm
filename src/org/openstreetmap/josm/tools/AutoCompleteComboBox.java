// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.util.Collection;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * @author guilhem.bonnefille@gmail.com
 */
public class AutoCompleteComboBox extends JComboBox {

	/**
	 * Auto-complete a JComboBox.
	 * 
	 * Inspired by http://www.orbital-computer.de/JComboBox/
	 */
	private class AutoCompleteComboBoxDocument extends PlainDocument {
		private JComboBox comboBox;
		private boolean selecting = false;

		public AutoCompleteComboBoxDocument(final JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override public void remove(int offs, int len) throws BadLocationException {
			if (selecting)
				return;
			super.remove(offs, len);
		}

		@Override public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);

			// return immediately when selecting an item
			// Nota: this is done after calling super method because we need
			// ActionListener informed
			if (selecting)
				return;

			// lookup and select a matching item
			Object item = lookupItem(getText(0, getLength()));
			if (item != null) {
				// remove all text and insert the completed string
				super.remove(0, getLength());
				super.insertString(0, item.toString(), a);

			}
			
			// select the completed part
			JTextComponent editor = (JTextComponent)comboBox.getEditor().getEditorComponent();
			editor.setSelectionStart(offs + str.length());
			editor.setSelectionEnd(getLength());
			
			setSelectedItem(item);
		}

		private void setSelectedItem(Object item) {
			selecting = true;
			comboBox.setSelectedItem(item);
			selecting = false;
		}

		private Object lookupItem(String pattern) {
			ComboBoxModel model = comboBox.getModel();
			for (int i = 0, n = model.getSize(); i < n; i++) {
				Object currentItem = model.getElementAt(i);
				if (currentItem.toString().startsWith(pattern))
					return currentItem;
			}
			return null;
		}
	}

	public AutoCompleteComboBox() {
		JTextComponent editor = (JTextComponent) this.getEditor().getEditorComponent();
		editor.setDocument(new AutoCompleteComboBoxDocument(this));
	}

	public void setPossibleItems(Collection<String> elems) {
		DefaultComboBoxModel model = (DefaultComboBoxModel)this.getModel();
		Object oldValue = this.getEditor().getItem();
		model.removeAllElements();
		for (String elem : elems) model.addElement(elem);
		this.getEditor().setItem(oldValue);
		this.getEditor().selectAll();
	}
}
