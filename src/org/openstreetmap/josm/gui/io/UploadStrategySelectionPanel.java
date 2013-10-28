// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;

/**
 * UploadStrategySelectionPanel is a panel for selecting an upload strategy.
 *
 * Clients can listen for property change events for the property
 * {@link #UPLOAD_STRATEGY_SPECIFICATION_PROP}.
 */
public class UploadStrategySelectionPanel extends JPanel implements PropertyChangeListener {

    /**
     * The property for the upload strategy
     */
    public final static String UPLOAD_STRATEGY_SPECIFICATION_PROP =
        UploadStrategySelectionPanel.class.getName() + ".uploadStrategySpecification";

    private static final Color BG_COLOR_ERROR = new Color(255,224,224);

    private Map<UploadStrategy, JRadioButton> rbStrategy;
    private Map<UploadStrategy, JLabel> lblNumRequests;
    private Map<UploadStrategy, JMultilineLabel> lblStrategies;
    private JosmTextField tfChunkSize;
    private JPanel pnlMultiChangesetPolicyPanel;
    private JRadioButton rbFillOneChangeset;
    private JRadioButton rbUseMultipleChangesets;
    private JMultilineLabel lblMultiChangesetPoliciesHeader;

    private long numUploadedObjects = 0;

    /**
     * Constructs a new {@code UploadStrategySelectionPanel}.
     */
    public UploadStrategySelectionPanel() {
        build();
    }

    protected JPanel buildUploadStrategyPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());
        ButtonGroup bgStrategies = new ButtonGroup();
        rbStrategy = new HashMap<UploadStrategy, JRadioButton>();
        lblStrategies = new HashMap<UploadStrategy, JMultilineLabel>();
        lblNumRequests = new HashMap<UploadStrategy, JLabel>();
        for (UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.put(strategy, new JRadioButton());
            lblNumRequests.put(strategy, new JLabel());
            lblStrategies.put(strategy, new JMultilineLabel(""));
            bgStrategies.add(rbStrategy.get(strategy));
        }

        // -- headline
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 4;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0,0,3,0);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(new JMultilineLabel(tr("Please select the upload strategy:")), gc);

        // -- single request strategy
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        pnl.add(rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        JLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
        lbl.setText(tr("Upload data in one request"));
        pnl.add(lbl, gc);
        gc.gridx = 3;
        gc.gridy = 1;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        pnl.add(lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY), gc);

        // -- chunked dataset strategy
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        pnl.add(rbStrategy.get(UploadStrategy.CHUNKED_DATASET_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        lbl = lblStrategies.get(UploadStrategy.CHUNKED_DATASET_STRATEGY);
        lbl.setText(tr("Upload data in chunks of objects. Chunk size: "));
        pnl.add(lbl, gc);
        gc.gridx = 2;
        gc.gridy = 2;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        pnl.add(tfChunkSize = new JosmTextField(4), gc);
        gc.gridx = 3;
        gc.gridy = 2;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        pnl.add(lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY), gc);

        // -- single request strategy
        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        pnl.add(rbStrategy.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);
        gc.gridx = 1;
        gc.gridy = 3;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        lbl = lblStrategies.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY);
        lbl.setText(tr("Upload each object individually"));
        pnl.add(lbl, gc);
        gc.gridx = 3;
        gc.gridy = 3;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        pnl.add(lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);

        tfChunkSize.addFocusListener(new TextFieldFocusHandler());
        tfChunkSize.getDocument().addDocumentListener(new ChunkSizeInputVerifier());

        StrategyChangeListener strategyChangeListener = new StrategyChangeListener();
        tfChunkSize.addFocusListener(strategyChangeListener);
        tfChunkSize.addActionListener(strategyChangeListener);
        for(UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.get(strategy).addItemListener(strategyChangeListener);
        }

        return pnl;
    }

    protected JPanel buildMultiChangesetPolicyPanel() {
        pnlMultiChangesetPolicyPanel = new JPanel();
        pnlMultiChangesetPolicyPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        pnlMultiChangesetPolicyPanel.add(lblMultiChangesetPoliciesHeader = new JMultilineLabel(tr("<html>There are <strong>multiple changesets</strong> necessary in order to upload {0} objects. Which strategy do you want to use?</html>", numUploadedObjects)), gc);
        gc.gridy = 1;
        pnlMultiChangesetPolicyPanel.add(rbFillOneChangeset = new JRadioButton(tr("Fill up one changeset and return to the Upload Dialog")),gc);
        gc.gridy = 2;
        pnlMultiChangesetPolicyPanel.add(rbUseMultipleChangesets = new JRadioButton(tr("Open and use as many new changesets as necessary")),gc);

        ButtonGroup bgMultiChangesetPolicies = new ButtonGroup();
        bgMultiChangesetPolicies.add(rbFillOneChangeset);
        bgMultiChangesetPolicies.add(rbUseMultipleChangesets);
        return pnlMultiChangesetPolicyPanel;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(3,3,3,3);

        add(buildUploadStrategyPanel(), gc);
        gc.gridy = 1;
        add(buildMultiChangesetPolicyPanel(), gc);

        // consume remaining space
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(new JPanel(), gc);

        int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
        pnlMultiChangesetPolicyPanel.setVisible(
                maxChunkSize > 0 && numUploadedObjects > maxChunkSize
        );
    }

    public void setNumUploadedObjects(int numUploadedObjects) {
        this.numUploadedObjects = Math.max(numUploadedObjects,0);
        updateNumRequestsLabels();
    }

    public void setUploadStrategySpecification(UploadStrategySpecification strategy) {
        if (strategy == null) return;
        rbStrategy.get(strategy.getStrategy()).setSelected(true);
        tfChunkSize.setEnabled(strategy.getStrategy() == UploadStrategy.CHUNKED_DATASET_STRATEGY);
        if (strategy.getStrategy().equals(UploadStrategy.CHUNKED_DATASET_STRATEGY)) {
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
        UploadStrategySpecification spec = new UploadStrategySpecification();
        switch(strategy) {
        case INDIVIDUAL_OBJECTS_STRATEGY:
            spec.setStrategy(strategy);
            break;
        case SINGLE_REQUEST_STRATEGY:
            spec.setStrategy(strategy);
            break;
        case CHUNKED_DATASET_STRATEGY:
            spec.setStrategy(strategy).setChunkSize(chunkSize);
            break;
        }
        if(pnlMultiChangesetPolicyPanel.isVisible()) {
            if (rbFillOneChangeset.isSelected()) {
                spec.setPolicy(MaxChangesetSizeExceededPolicy.FILL_ONE_CHANGESET_AND_RETURN_TO_UPLOAD_DIALOG);
            } else if (rbUseMultipleChangesets.isSelected()) {
                spec.setPolicy(MaxChangesetSizeExceededPolicy.AUTOMATICALLY_OPEN_NEW_CHANGESETS);
            } else {
                spec.setPolicy(null); // unknown policy
            }
        } else {
            spec.setPolicy(null);
        }
        return spec;
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

    public void rememberUserInput() {
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
        int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
        if (maxChunkSize > 0 && numUploadedObjects > maxChunkSize) {
            rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setEnabled(false);
            JLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setIcon(ImageProvider.get("warning-small.png"));
            lbl.setText(tr("Upload in one request not possible (too many objects to upload)"));
            lbl.setToolTipText(tr("<html>Cannot upload {0} objects in one request because the<br>"
                    + "max. changeset size {1} on server ''{2}'' is exceeded.</html>",
                    numUploadedObjects,
                    maxChunkSize,
                    OsmApi.getOsmApi().getBaseUrl()
            )
            );
            rbStrategy.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setSelected(true);
            lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setVisible(false);

            lblMultiChangesetPoliciesHeader.setText(tr("<html>There are <strong>multiple changesets</strong> necessary in order to upload {0} objects. Which strategy do you want to use?</html>", numUploadedObjects));
            if (!rbFillOneChangeset.isSelected() && ! rbUseMultipleChangesets.isSelected()) {
                rbUseMultipleChangesets.setSelected(true);
            }
            pnlMultiChangesetPolicyPanel.setVisible(true);

        } else {
            rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setEnabled(true);
            JLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setText(tr("Upload data in one request"));
            lbl.setIcon(null);
            lbl.setToolTipText("");
            lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setVisible(true);

            pnlMultiChangesetPolicyPanel.setVisible(false);
        }

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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(UploadedObjectsSummaryPanel.NUM_OBJECTS_TO_UPLOAD_PROP)) {
            setNumUploadedObjects((Integer)evt.getNewValue());
        }
    }

    static class TextFieldFocusHandler implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JosmTextField) {
                JosmTextField tf = (JosmTextField)c;
                tf.selectAll();
            }
        }
        @Override
        public void focusLost(FocusEvent e) {}
    }

    class ChunkSizeInputVerifier implements DocumentListener, PropertyChangeListener {
        protected void setErrorFeedback(JosmTextField tf, String message) {
            tf.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
            tf.setToolTipText(message);
            tf.setBackground(BG_COLOR_ERROR);
        }

        protected void clearErrorFeedback(JosmTextField tf, String message) {
            tf.setBorder(UIManager.getBorder("TextField.border"));
            tf.setToolTipText(message);
            tf.setBackground(UIManager.getColor("TextField.background"));
        }

        protected void valiateChunkSize() {
            try {
                int chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
                int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
                if (chunkSize <= 0) {
                    setErrorFeedback(tfChunkSize, tr("Illegal chunk size <= 0. Please enter an integer > 1"));
                } else if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    setErrorFeedback(tfChunkSize, tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''", chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                } else {
                    clearErrorFeedback(tfChunkSize, tr("Please enter an integer > 1"));
                }

                if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    setErrorFeedback(tfChunkSize, tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''", chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                }
            } catch(NumberFormatException e) {
                setErrorFeedback(tfChunkSize, tr("Value ''{0}'' is not a number. Please enter an integer > 1", tfChunkSize.getText().trim()));
            } finally {
                updateNumRequestsLabels();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            valiateChunkSize();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == tfChunkSize
                    && evt.getPropertyName().equals("enabled")
                    && (Boolean)evt.getNewValue()
            ) {
                valiateChunkSize();
            }
        }
    }

    class StrategyChangeListener implements ItemListener, FocusListener, ActionListener {

        protected void notifyStrategy() {
            firePropertyChange(UPLOAD_STRATEGY_SPECIFICATION_PROP, null, getUploadStrategySpecification());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
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
            notifyStrategy();
        }

        @Override
        public void focusGained(FocusEvent arg0) {}

        @Override
        public void focusLost(FocusEvent arg0) {
            notifyStrategy();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            notifyStrategy();
        }
    }
}
