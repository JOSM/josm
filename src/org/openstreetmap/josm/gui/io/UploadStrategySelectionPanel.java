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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.Capabilities;
import org.openstreetmap.josm.io.MaxChangesetSizeExceededPolicy;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.UploadStrategy;
import org.openstreetmap.josm.io.UploadStrategySpecification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

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
    public static final String UPLOAD_STRATEGY_SPECIFICATION_PROP =
        UploadStrategySelectionPanel.class.getName() + ".uploadStrategySpecification";

    private static final Color BG_COLOR_ERROR = new Color(255, 224, 224);

    private transient Map<UploadStrategy, JRadioButton> rbStrategy;
    private transient Map<UploadStrategy, JLabel> lblNumRequests;
    private transient Map<UploadStrategy, JMultilineLabel> lblStrategies;
    private final JosmTextField tfChunkSize = new JosmTextField(4);
    private final JPanel pnlMultiChangesetPolicyPanel = new JPanel(new GridBagLayout());
    private final JRadioButton rbFillOneChangeset = new JRadioButton(
            tr("Fill up one changeset and return to the Upload Dialog"));
    private final JRadioButton rbUseMultipleChangesets = new JRadioButton(
            tr("Open and use as many new changesets as necessary"));
    private JMultilineLabel lblMultiChangesetPoliciesHeader;

    private long numUploadedObjects;

    /**
     * Constructs a new {@code UploadStrategySelectionPanel}.
     */
    public UploadStrategySelectionPanel() {
        build();
    }

    protected JPanel buildUploadStrategyPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        ButtonGroup bgStrategies = new ButtonGroup();
        rbStrategy = new EnumMap<>(UploadStrategy.class);
        lblStrategies = new EnumMap<>(UploadStrategy.class);
        lblNumRequests = new EnumMap<>(UploadStrategy.class);
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
        gc.insets = new Insets(0, 0, 3, 0);
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
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        JMultilineLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
        lbl.setText(tr("Upload data in one request"));
        pnl.add(lbl, gc);
        gc.gridx = 3;
        gc.gridy = 1;
        gc.weightx = 0.0;
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
        gc.weightx = 1.0;
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
        pnl.add(tfChunkSize, gc);
        gc.gridx = 3;
        gc.gridy = 2;
        gc.weightx = 0.0;
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
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.gridwidth = 2;
        lbl = lblStrategies.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY);
        lbl.setText(tr("Upload each object individually"));
        pnl.add(lbl, gc);
        gc.gridx = 3;
        gc.gridy = 3;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        pnl.add(lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);

        tfChunkSize.addFocusListener(new TextFieldFocusHandler());
        tfChunkSize.getDocument().addDocumentListener(new ChunkSizeInputVerifier());

        StrategyChangeListener strategyChangeListener = new StrategyChangeListener();
        tfChunkSize.addFocusListener(strategyChangeListener);
        tfChunkSize.addActionListener(strategyChangeListener);
        for (UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.get(strategy).addItemListener(strategyChangeListener);
        }

        return pnl;
    }

    protected JPanel buildMultiChangesetPolicyPanel() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 1.0;
        lblMultiChangesetPoliciesHeader = new JMultilineLabel(
                tr("<html>There are <strong>multiple changesets</strong> necessary in order to upload {0} objects. " +
                   "Which strategy do you want to use?</html>",
                        numUploadedObjects));
        pnlMultiChangesetPolicyPanel.add(lblMultiChangesetPoliciesHeader, gc);
        gc.gridy = 1;
        pnlMultiChangesetPolicyPanel.add(rbFillOneChangeset, gc);
        gc.gridy = 2;
        pnlMultiChangesetPolicyPanel.add(rbUseMultipleChangesets, gc);

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
        gc.insets = new Insets(3, 3, 3, 3);

        add(buildUploadStrategyPanel(), gc);
        gc.gridy = 1;
        add(buildMultiChangesetPolicyPanel(), gc);

        // consume remaining space
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(new JPanel(), gc);

        Capabilities capabilities = OsmApi.getOsmApi().getCapabilities();
        int maxChunkSize = capabilities != null ? capabilities.getMaxChangesetSize() : -1;
        pnlMultiChangesetPolicyPanel.setVisible(
                maxChunkSize > 0 && numUploadedObjects > maxChunkSize
        );
    }

    /**
     * Sets the number of uploaded objects to display
     * @param numUploadedObjects The number of objects
     */
    public void setNumUploadedObjects(int numUploadedObjects) {
        this.numUploadedObjects = Math.max(numUploadedObjects, 0);
        updateNumRequestsLabels();
    }

    /**
     * Fills the inputs using a {@link UploadStrategySpecification}
     * @param strategy The strategy
     */
    public void setUploadStrategySpecification(UploadStrategySpecification strategy) {
        if (strategy == null)
            return;
        rbStrategy.get(strategy.getStrategy()).setSelected(true);
        tfChunkSize.setEnabled(strategy.getStrategy() == UploadStrategy.CHUNKED_DATASET_STRATEGY);
        if (strategy.getStrategy() == UploadStrategy.CHUNKED_DATASET_STRATEGY) {
            if (strategy.getChunkSize() != UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE) {
                tfChunkSize.setText(Integer.toString(strategy.getChunkSize()));
            } else {
                tfChunkSize.setText("1");
            }
        }
    }

    /**
     * Gets the upload strategy the user chose
     * @return The strategy
     */
    public UploadStrategySpecification getUploadStrategySpecification() {
        UploadStrategy strategy = getUploadStrategy();
        UploadStrategySpecification spec = new UploadStrategySpecification();
        if (strategy != null) {
            switch(strategy) {
            case CHUNKED_DATASET_STRATEGY:
                spec.setStrategy(strategy).setChunkSize(getChunkSize());
                break;
            case INDIVIDUAL_OBJECTS_STRATEGY:
            case SINGLE_REQUEST_STRATEGY:
            default:
                spec.setStrategy(strategy);
                break;
            }
        }
        if (pnlMultiChangesetPolicyPanel.isVisible()) {
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
        return rbStrategy.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .findFirst()
                .map(Entry::getKey)
                .orElse(null);
    }

    protected int getChunkSize() {
        try {
            return Integer.parseInt(tfChunkSize.getText().trim());
        } catch (NumberFormatException e) {
            return UploadStrategySpecification.UNSPECIFIED_CHUNK_SIZE;
        }
    }

    /**
     * Load the panel contents from preferences
     */
    public void initFromPreferences() {
        UploadStrategy strategy = UploadStrategy.getFromPreferences();
        rbStrategy.get(strategy).setSelected(true);
        int chunkSize = Config.getPref().getInt("osm-server.upload-strategy.chunk-size", 1000);
        tfChunkSize.setText(Integer.toString(chunkSize));
        updateNumRequestsLabels();
    }

    /**
     * Stores the values that the user has input into the preferences
     */
    public void rememberUserInput() {
        UploadStrategy strategy = getUploadStrategy();
        UploadStrategy.saveToPreferences(strategy);
        int chunkSize;
        try {
            chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
            Config.getPref().putInt("osm-server.upload-strategy.chunk-size", chunkSize);
        } catch (NumberFormatException e) {
            // don't save invalid value to preferences
            Logging.trace(e);
        }
    }

    protected void updateNumRequestsLabels() {
        int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
        if (maxChunkSize > 0 && numUploadedObjects > maxChunkSize) {
            rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setEnabled(false);
            JMultilineLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setText(tr("Upload in one request not possible (too many objects to upload)"));
            lbl.setToolTipText(tr("<html>Cannot upload {0} objects in one request because the<br>"
                    + "max. changeset size {1} on server ''{2}'' is exceeded.</html>",
                    numUploadedObjects, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()
            )
            );
            rbStrategy.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setSelected(true);
            lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setVisible(false);

            lblMultiChangesetPoliciesHeader.setText(
                    tr("<html>There are <strong>multiple changesets</strong> necessary in order to upload {0} objects. " +
                       "Which strategy do you want to use?</html>",
                            numUploadedObjects));
            if (!rbFillOneChangeset.isSelected() && !rbUseMultipleChangesets.isSelected()) {
                rbUseMultipleChangesets.setSelected(true);
            }
            pnlMultiChangesetPolicyPanel.setVisible(true);

        } else {
            rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY).setEnabled(true);
            JMultilineLabel lbl = lblStrategies.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setText(tr("Upload data in one request"));
            lbl.setToolTipText(null);
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
                int chunks = (int) Math.ceil((double) numUploadedObjects / (double) chunkSize);
                lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY).setText(
                        trn("({0} request)", "({0} requests)", chunks, chunks)
                );
            }
        }
    }

    /**
     * Sets the focus on the chunk size field
     */
    public void initEditingOfChunkSize() {
        tfChunkSize.requestFocusInWindow();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(UploadedObjectsSummaryPanel.NUM_OBJECTS_TO_UPLOAD_PROP)) {
            setNumUploadedObjects((Integer) evt.getNewValue());
        }
    }

    static class TextFieldFocusHandler extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JosmTextField) {
                JosmTextField tf = (JosmTextField) c;
                tf.selectAll();
            }
        }
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

        protected void validateChunkSize() {
            try {
                int chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
                int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
                if (chunkSize <= 0) {
                    setErrorFeedback(tfChunkSize, tr("Illegal chunk size <= 0. Please enter an integer > 1"));
                } else if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    setErrorFeedback(tfChunkSize, tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''",
                            chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                } else {
                    clearErrorFeedback(tfChunkSize, tr("Please enter an integer > 1"));
                }

                if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    setErrorFeedback(tfChunkSize, tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''",
                            chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                }
            } catch (NumberFormatException e) {
                setErrorFeedback(tfChunkSize, tr("Value ''{0}'' is not a number. Please enter an integer > 1",
                        tfChunkSize.getText().trim()));
            } finally {
                updateNumRequestsLabels();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            validateChunkSize();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            validateChunkSize();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            validateChunkSize();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == tfChunkSize
                    && "enabled".equals(evt.getPropertyName())
                    && (Boolean) evt.getNewValue()
            ) {
                validateChunkSize();
            }
        }
    }

    class StrategyChangeListener extends FocusAdapter implements ItemListener, ActionListener {

        protected void notifyStrategy() {
            firePropertyChange(UPLOAD_STRATEGY_SPECIFICATION_PROP, null, getUploadStrategySpecification());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            UploadStrategy strategy = getUploadStrategy();
            if (strategy == null)
                return;
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
        public void focusLost(FocusEvent e) {
            notifyStrategy();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            notifyStrategy();
        }
    }
}
