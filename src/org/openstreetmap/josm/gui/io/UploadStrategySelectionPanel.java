// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

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
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
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
public class UploadStrategySelectionPanel extends JPanel {

    /**
     * The property for the upload strategy
     */
    public static final String UPLOAD_STRATEGY_SPECIFICATION_PROP =
        UploadStrategySelectionPanel.class.getName() + ".uploadStrategySpecification";

    private transient Map<UploadStrategy, JRadioButton> rbStrategy;
    private transient Map<UploadStrategy, JLabel> lblNumRequests;
    private final JosmTextField tfChunkSize = new JosmTextField(4);
    private final JPanel pnlMultiChangesetPolicyPanel = new JPanel(new GridBagLayout());
    private final JRadioButton rbFillOneChangeset = new JRadioButton();
    private final JRadioButton rbUseMultipleChangesets = new JRadioButton();
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
        pnl.setBorder(BorderFactory.createTitledBorder(tr("Please select the upload strategy:")));
        ButtonGroup bgStrategies = new ButtonGroup();
        rbStrategy = new EnumMap<>(UploadStrategy.class);
        lblNumRequests = new EnumMap<>(UploadStrategy.class);
        for (UploadStrategy strategy: UploadStrategy.values()) {
            rbStrategy.put(strategy, new JRadioButton());
            lblNumRequests.put(strategy, new JLabel());
            bgStrategies.add(rbStrategy.get(strategy));
        }

        // -- single request strategy
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 3, 3, 3);
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        JRadioButton radioButton = rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
        radioButton.setText(tr("Upload all objects in one request"));
        pnl.add(radioButton, gc);
        gc.gridx = 2;
        gc.weightx = 1.0;
        pnl.add(lblNumRequests.get(UploadStrategy.SINGLE_REQUEST_STRATEGY), gc);

        // -- chunked dataset strategy
        gc.gridy++;
        gc.gridx = 0;
        gc.weightx = 0.0;
        radioButton = rbStrategy.get(UploadStrategy.CHUNKED_DATASET_STRATEGY);
        radioButton.setText(tr("Upload objects in chunks of size: "));
        pnl.add(radioButton, gc);
        gc.gridx = 1;
        pnl.add(tfChunkSize, gc);
        gc.gridx = 2;
        pnl.add(lblNumRequests.get(UploadStrategy.CHUNKED_DATASET_STRATEGY), gc);

        // -- single request strategy
        gc.gridy++;
        gc.gridx = 0;
        radioButton = rbStrategy.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY);
        radioButton.setText(tr("Upload each object individually"));
        pnl.add(radioButton, gc);
        gc.gridx = 2;
        pnl.add(lblNumRequests.get(UploadStrategy.INDIVIDUAL_OBJECTS_STRATEGY), gc);

        new ChunkSizeValidator(tfChunkSize);

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
        gc.insets = new Insets(3, 3, 3, 3);
        gc.weightx = 1.0;
        lblMultiChangesetPoliciesHeader = new JMultilineLabel(
                tr("<html><strong>Multiple changesets</strong> are necessary to upload {0} objects. " +
                   "Please select a strategy:</html>",
                        numUploadedObjects));
        pnlMultiChangesetPolicyPanel.add(lblMultiChangesetPoliciesHeader, gc);
        gc.gridy++;
        rbFillOneChangeset.setText(tr("Fill up one changeset and return to the Upload Dialog"));
        pnlMultiChangesetPolicyPanel.add(rbFillOneChangeset, gc);
        gc.gridy++;
        rbUseMultipleChangesets.setText(tr("Open and use as many new changesets as necessary"));
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

        add(buildUploadStrategyPanel(), gc);
        gc.gridy = 1;
        add(buildMultiChangesetPolicyPanel(), gc);

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
            JRadioButton lbl = rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setEnabled(false);
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
            JRadioButton lbl = rbStrategy.get(UploadStrategy.SINGLE_REQUEST_STRATEGY);
            lbl.setEnabled(true);
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

    class ChunkSizeValidator extends AbstractTextComponentValidator {
        ChunkSizeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public void validate() {
            try {
                int chunkSize = Integer.parseInt(tfChunkSize.getText().trim());
                int maxChunkSize = OsmApi.getOsmApi().getCapabilities().getMaxChangesetSize();
                if (chunkSize <= 0) {
                    feedbackInvalid(tr("Illegal chunk size <= 0. Please enter an integer > 1"));
                } else if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    feedbackInvalid(tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''",
                            chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                } else {
                    feedbackValid(null);
                }

                if (maxChunkSize > 0 && chunkSize > maxChunkSize) {
                    feedbackInvalid(tr("Chunk size {0} exceeds max. changeset size {1} for server ''{2}''",
                            chunkSize, maxChunkSize, OsmApi.getOsmApi().getBaseUrl()));
                }
            } catch (NumberFormatException e) {
                feedbackInvalid(tr("Value ''{0}'' is not a number. Please enter an integer > 1",
                        tfChunkSize.getText().trim()));
            } finally {
                updateNumRequestsLabels();
            }
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException();
        }
    }

    class StrategyChangeListener implements FocusListener, ItemListener, ActionListener {

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
        public void focusGained(FocusEvent e) {
            Component c = e.getComponent();
            if (c instanceof JosmTextField) {
                JosmTextField tf = (JosmTextField) c;
                tf.selectAll();
            }
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
