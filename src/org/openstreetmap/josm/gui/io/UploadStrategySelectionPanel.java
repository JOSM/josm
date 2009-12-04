// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;

public class UploadStrategySelectionPanel extends JPanel {
    private static final Color BG_COLOR_ERROR = new Color(255,224,224);

    private ButtonGroup bgStrategies;
    private Map<UploadStrategy, JRadioButton> rbStrategy;
    private Map<UploadStrategy, JLabel> lblNumRequests;
    private JTextField tfChunkSize;

    private long numUploadedObjects = 0;

    public UploadStrategySelectionPanel() {
        build();
    }

    protected void build() {
        setLayout(new GridBagLayout());
        bgStrategies = new ButtonGroup();
        rbStrategy = new HashMap<UploadStrategy, JRadioButton>();
        lblNumRequests = new HashMap<UploadStrategy, JLabel>();
        for (UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.put(strategy, new JRadioButton());
            lblNumRequests.put(strategy, new JLabel());
            bgStrategies.add(rbStrategy.get(strategy));
        }

        // -- single request strategy
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,5,0,5);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        add(new JLabel(tr("Upload data in one request")), gc);
        gc.gridx = 3;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        add(lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY), gc);

        // -- chunked dataset strategy
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(rbStrategy.get(UploadStrategy.CHUNKED_DATASET_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        add(new JLabel(tr("Upload data in chunks of objects. Chunk size: ")), gc);
        gc.gridx = 2;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        add(tfChunkSize = new JTextField(4), gc);
        gc.gridx = 3;
        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        add(lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY), gc);

        // -- single request strategy
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        add(rbStrategy.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        add(new JLabel(tr("Upload each object individually")), gc);
        gc.gridx = 3;
        gc.gridy = 2;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        add(lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);


        tfChunkSize.addFocusListener(new TextFieldFocusHandler());
        tfChunkSize.getDocument().addDocumentListener(new ChunkSizeInputVerifier());
        StrategyChangeListener strategyChangeListener = new StrategyChangeListener();
        for(UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.get(strategy).addChangeListener(strategyChangeListener);
        }

    }

    public void setNumUploadedObjects(int numUploadedObjects) {
        this.numUploadedObjects = Math.max(numUploadedObjects,0);
        updateNumRequestsLabels();
    }

    public void setUploadStrategySpecification(UploadStrategySpecification strategy) {
        if (strategy == null) return;
        rbStrategy.get(strategy.getStrategy()).setSelected(true);
        if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
            tfChunkSize.setEnabled(strategy.equals(UploadStrategy.CHUNKED_DATASET_STRATEGY));
            if (strategy.getChunkSize() != UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                tfChunkSize.setText(Integer.toString(strategy.getChunkSize()));
            } else {
                tfChunkSize.setText("1");
            }
        }
    }

    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategy strategy = getUploadStrategy();
        int chunkSize = getChunkSize();
        switch(strategy) {
        case INDIVIDUAL_OBJECTS_STRATEGY: return UploadStrategySpecification.createIndividualObjectStrategy();
        case SINGLE_REQUEST_STRATEGY: return UploadStrategySpecification.createSingleRequestUploadStrategy();
        case CHUNKED_DATASET_STRATEGY: return UploadStrategySpecification.createChunkedUploadStrategy(chunkSize);
        }
        // should not happen
        return null;
    }

    protected UploadStrategy getUploadStrategy() {
        UploadStrategy strategy = null;
        for (UploadStrategy s: rbStrategy.keySet()) {
            if (rbStrategy.get(s).isSelected()) {
                strategy = s;
                break;
            }
        }
        return strategy;
    }


    protected int getChunkSize() {
        int chunkSize;
        try {
            chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
            return chunkSize;
        } catch(NumberFormatException e) {
            return UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE;
        }
    }

    public void initFromPreferences() {
        UploadStrategy strategy = UploadStrategy.getFromPreferences();
        rbStrategy.get(strategy).setSelected(true);
        int chunkSize = Main.pref.getInteger("osm-server.upload-strategy.chunk-size", 1);
        tfChunkSize.setText(Integer.toString(chunkSize));
        updateNumRequestsLabels();
    }

    public void saveToPreferences() {
        UploadStrategy strategy = getUploadStrategy();
        UploadStrategy.saveToPreferences(strategy);
        int chunkSize;
        try {
            chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
            Main.pref.putInteger("osm-server.upload-strategy.chunk-size", chunkSize);
        } catch(NumberFormatException e) {
            // don't save invalid value to preferences
        }
    }

    protected void updateNumRequestsLabels() {
        lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setText(tr("(1 request)"));
        if (numUploadedObjects == 0) {
            lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY).setText(tr("(# requests unknown)"));
            lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setText(tr("(# requests unknown)"));
        } else {
            lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY).setText(
                    trn("({0} request)", "({0} requests)", numUploadedObjects, numUploadedObjects)
            );
            lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setText(tr("(# requests unknown)"));
            int chunkSize = getChunkSize();
            if (chunkSize == UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setText(tr("(# requests unknown)"));
            } else {
                int chunks = (int)Math.ceil((double)numUploadedObjects / (double)chunkSize);
                lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setText(
                        trn("({0} request)", "({0} requests)", chunks, chunks)
                );
            }
        }
    }

    public void initEditingOfChunkSize() {
        tfChunkSize.requestFocusInWindow();
    }

    class TextFieldFocusHandler implements FocusListener {
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JTextField) {
                JTextField tf = (JTextField)c;
                tf.selectAll();
            }
        }
        public void focusLost(FocusEvent e) {}
    }

    class ChunkSizeInputVerifier implements DocumentListener, PropertyChangeListener {

        protected void setErrorFeedback(JTextField tf, String message) {
            tf.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
            tf.setToolTipText(message);
            tf.setBackground(BG_COLOR_ERROR);
        }

        protected void clearErrorFeedback(JTextField tf, String message) {
            tf.setBorder(UIManager.getBorder("TextField.border"));
            tf.setToolTipText(message);
            tf.setBackground(UIManager.getColor("TextField.background"));
        }

        protected void valiateChunkSize() {
            try {
                int chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
                if (chunkSize <= 0) {
                    setErrorFeedback(tfChunkSize, tr("Illegal chunk size <= 0. Please enter an integer > 1"));
                } else {
                    clearErrorFeedback(tfChunkSize, tr("Please enter an integer > 1"));
                }
            } catch(NumberFormatException e) {
                setErrorFeedback(tfChunkSize, tr("Value ''{0}'' is not a number. Please enter an integer > 1", tfChunkSize.getText().trim()));
            } finally {
                updateNumRequestsLabels();
            }
        }

        public void changedUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        public void insertUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        public void removeUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == tfChunkSize
                    && evt.getPropertyName().equals("enabled")
                    && (Boolean)evt.getNewValue()
            ) {
                valiateChunkSize();
            }
        }
    }

    class StrategyChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            UploadStrategy strategy = getUploadStrategy();
            if (strategy == null) return;
            switch(strategy) {
            case CHUNKED_DATASET_STRATEGY:
                tfChunkSize.setEnabled(true);
                tfChunkSize.requestFocusInWindow();
                break;
            default:
                tfChunkSize.setEnabled(false);
            }
        }
    }
}
