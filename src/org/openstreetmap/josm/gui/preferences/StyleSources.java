// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.LanguageInfo;

public class StyleSources extends JPanel {
    private JList sourcesList;
    private JList sourcesDefaults;
    private JList iconsList = null;
    private String pref;
    private String iconpref;

    public class SourceInfo {
        String version;
        String name;
        String url;
        String author;
        String link;
        String description;
        String shortdescription;
        public SourceInfo(String name, String url)
        {
            this.name = name;
            this.url = url;
            version = author = link = description = shortdescription = null;
        }
        public String getName()
        {
            return shortdescription == null ? name : shortdescription;
        }
        public String getTooltip()
        {
            String s = tr("Short Description: {0}", getName()) + "<br>" + tr("URL: {0}", url);
            if(author != null) {
                s += "<br>" + tr("Author: {0}", author);
            }
            if(link != null) {
                s += "<br>" + tr("Webpage: {0}", link);
            }
            if(description != null) {
                s += "<br>" + tr("Description: {0}", description);
            }
            if(version != null) {
                s += "<br>" + tr("Version: {0}", version);
            }
            return "<html>" + s + "</html>";
        }
        @Override
        public String toString()
        {
            return getName() + " (" + url + ")";
        }
    };

    class MyCellRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            String s = value.toString();
            setText(s);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            setToolTipText(((SourceInfo)value).getTooltip());
            return this;
        }
    }

    public StyleSources(String pref, String iconpref, final String url, boolean named, final String name)
    {
        sourcesList = new JList(new DefaultListModel());
        sourcesDefaults = new JList(new DefaultListModel());
        sourcesDefaults.setCellRenderer(new MyCellRenderer());
        getDefaults(url);

        this.pref = pref;
        this.iconpref = iconpref;

        Collection<String> sources = Main.pref.getCollection(pref, null);
        if(sources != null) {
            for(String s : sources) {
                ((DefaultListModel)sourcesList.getModel()).addElement(s);
            }
        }

        JButton iconadd = null;
        JButton iconedit = null;
        JButton icondelete = null;

        if(iconpref != null)
        {
            iconsList = new JList(new DefaultListModel());
            sources = Main.pref.getCollection(iconpref, null);
            if(sources != null) {
                for(String s : sources) {
                    ((DefaultListModel)iconsList.getModel()).addElement(s);
                }
            }

            iconadd = new JButton(tr("Add"));
            iconadd.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    String source = JOptionPane.showInputDialog(
                            Main.parent,
                            tr("Icon paths"),
                            tr("Icon paths"),
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (source != null) {
                        ((DefaultListModel)iconsList.getModel()).addElement(source);
                    }
                }
            });

            iconedit = new JButton(tr("Edit"));
            iconedit.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (sourcesList.getSelectedIndex() == -1) {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Please select the row to edit."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE
                        );
                    } else {
                        String source = (String)JOptionPane.showInputDialog(
                                Main.parent,
                                tr("Icon paths"),
                                tr("Icon paths"),
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                null,
                                iconsList.getSelectedValue()
                        );
                        if (source != null) {
                            ((DefaultListModel)iconsList.getModel()).setElementAt(source, iconsList.getSelectedIndex());
                        }
                    }
                }
            });

            icondelete = new JButton(tr("Delete"));
            icondelete.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (iconsList.getSelectedIndex() == -1) {
                        JOptionPane.showMessageDialog(
                                Main.parent, tr("Please select the row to delete."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        ((DefaultListModel)iconsList.getModel()).remove(iconsList.getSelectedIndex());
                    }
                }
            });
        }

        JButton add = new JButton(tr("Add"));
        add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String source = JOptionPane.showInputDialog(
                        Main.parent,
                        name,
                        name,
                        JOptionPane.QUESTION_MESSAGE);
                if (source != null) {
                    ((DefaultListModel)sourcesList.getModel()).addElement(source);
                }
            }
        });

        JButton edit = new JButton(tr("Edit"));
        edit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (sourcesList.getSelectedIndex() == -1) {
                    JOptionPane.showMessageDialog(
                            Main.parent, tr("Please select the row to edit."),
                            tr("Warning"), JOptionPane.WARNING_MESSAGE);
                } else {
                    String source = (String)JOptionPane.showInputDialog(
                            Main.parent,
                            name,
                            name,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            sourcesList.getSelectedValue()
                    );
                    if (source != null) {
                        ((DefaultListModel)sourcesList.getModel()).setElementAt(source, sourcesList.getSelectedIndex());
                    }
                }
            }
        });

        JButton delete = new JButton(tr("Delete"));
        delete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (sourcesList.getSelectedIndex() == -1) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."),
                            tr("Warning"), JOptionPane.WARNING_MESSAGE);
                } else {
                    ((DefaultListModel)sourcesList.getModel()).remove(sourcesList.getSelectedIndex());
                }
            }
        });

        JButton copy = new JButton(tr("Copy defaults"));
        copy.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (sourcesDefaults.getSelectedIndex() == -1) {
                    JOptionPane.showMessageDialog(
                            Main.parent, tr("Please select the row to copy."),
                            tr("Warning"), JOptionPane.WARNING_MESSAGE);
                } else {
                    ((DefaultListModel)sourcesList.getModel()).addElement(
                            ((SourceInfo)sourcesDefaults.getSelectedValue()).url);
                }
            }
        });

        JButton update = new JButton(tr("Update"));
        update.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                MirroredInputStream.cleanup(url);
                getDefaults(url);
                int num = sourcesList.getModel().getSize();
                if (num > 0)
                {
                    ArrayList<String> l = new ArrayList<String>();
                    for (int i = 0; i < num; ++i) {
                        MirroredInputStream.cleanup((String)sourcesList.getModel().getElementAt(i));
                    }
                }
            }
        });

        sourcesList.setToolTipText(tr("The XML source (URL or filename) for {0} definition files.", name));
        add.setToolTipText(tr("Add a new XML source to the list."));
        delete.setToolTipText(tr("Delete the selected source from the list."));

        setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        setLayout(new GridBagLayout());
        add(new JLabel(name), GBC.eol().insets(5,5,5,0));
        add(new JScrollPane(sourcesList), GBC.eol().insets(5,0,5,0).fill(GBC.BOTH));
        add(new JLabel(tr("Defaults (See tooltip for detailed information)")), GBC.eol().insets(5,5,5,0));
        add(new JScrollPane(sourcesDefaults), GBC.eol().insets(5,0,5,0).fill(GBC.BOTH));
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        add(buttonPanel, GBC.eol().insets(5,0,5,5).fill(GBC.HORIZONTAL));
        buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
        buttonPanel.add(add, GBC.std().insets(0,5,0,0));
        buttonPanel.add(edit, GBC.std().insets(5,5,5,0));
        buttonPanel.add(delete, GBC.std().insets(0,5,5,0));
        buttonPanel.add(copy, GBC.std().insets(0,5,5,0));
        buttonPanel.add(update, GBC.std().insets(0,5,0,0));
        if(iconsList != null)
        {
            add(new JLabel(tr("Icon paths")), GBC.eol().insets(5,-5,5,0));
            add(new JScrollPane(iconsList), GBC.eol().insets(5,0,5,0).fill(GBC.BOTH));
            buttonPanel = new JPanel(new GridBagLayout());
            add(buttonPanel, GBC.eol().insets(5,0,5,5).fill(GBC.HORIZONTAL));
            buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
            buttonPanel.add(iconadd, GBC.std().insets(0,5,0,0));
            buttonPanel.add(iconedit, GBC.std().insets(5,5,5,0));
            buttonPanel.add(icondelete, GBC.std().insets(0,5,0,0));
        }
    }

    public boolean finish() {
        boolean changed = false;
        int num = sourcesList.getModel().getSize();
        if (num > 0)
        {
            ArrayList<String> l = new ArrayList<String>();
            for (int i = 0; i < num; ++i) {
                l.add((String)sourcesList.getModel().getElementAt(i));
            }
            if(Main.pref.putCollection(pref, l)) {
                changed = true;
            }
        }
        else if(Main.pref.putCollection(pref, null)) {
            changed = true;
        }
        if(iconsList != null)
        {
            num = iconsList.getModel().getSize();
            if (num > 0)
            {
                ArrayList<String> l = new ArrayList<String>();
                for (int i = 0; i < num; ++i) {
                    l.add((String)iconsList.getModel().getElementAt(i));
                }
                if(Main.pref.putCollection(iconpref, l)) {
                    changed = true;
                }
            }
            else if(Main.pref.putCollection(iconpref, null)) {
                changed = true;
            }
        }
        return changed;
    }

    public void getDefaults(String name)
    {
        ((DefaultListModel)sourcesDefaults.getModel()).removeAllElements();
        String lang = LanguageInfo.getLanguageCodeXML();
        try
        {
            MirroredInputStream stream = new MirroredInputStream(name);
            InputStreamReader r;
            try
            {
                r = new InputStreamReader(stream, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                r = new InputStreamReader(stream);
            }
            BufferedReader reader = new BufferedReader(r);

            String line;
            SourceInfo last = null;

            while((line = reader.readLine()) != null)
            {
                if(line.startsWith("\t"))
                {
                    Matcher m = Pattern.compile("^\t([^:]+): *(.+)$").matcher(line);
                    m.matches();
                    try
                    {
                        if(last != null)
                        {
                            String key = m.group(1);
                            String value = m.group(2);
                            if("author".equals(key) && last.author == null) {
                                last.author = value;
                            } else if("version".equals(key)) {
                                last.version = value;
                            } else if("link".equals(key) && last.link == null) {
                                last.link = value;
                            } else if("description".equals(key) && last.description == null) {
                                last.description = value;
                            } else if("shortdescription".equals(key) && last.shortdescription == null) {
                                last.shortdescription = value;
                            } else if((lang+"author").equals(key)) {
                                last.author = value;
                            } else if((lang+"link").equals(key)) {
                                last.link = value;
                            } else if((lang+"description").equals(key)) {
                                last.description = value;
                            } else if((lang+"shortdescription").equals(key)) {
                                last.shortdescription = value;
                            }
                        }
                    }
                    catch (IllegalStateException e)
                    { e.printStackTrace(); }
                }
                else
                {
                    last = null;
                    Matcher m = Pattern.compile("^(.+);(.+)$").matcher(line);
                    m.matches();
                    try
                    {
                        last = new SourceInfo(m.group(1),m.group(2));
                        ((DefaultListModel)sourcesDefaults.getModel()).addElement(last);
                    }
                    catch (IllegalStateException e)
                    { e.printStackTrace(); }
                }
            }
        }
        catch(Exception e)
        { e.printStackTrace(); }
    }
}
