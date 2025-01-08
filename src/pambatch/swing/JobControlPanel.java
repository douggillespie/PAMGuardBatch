package pambatch.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import PamController.fileprocessing.ReprocessStoreChoice;
import PamModel.SMRUEnable;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.warn.WarnOnce;
import pambatch.BatchControl;
import pambatch.config.BatchMode;
import pambatch.config.BatchParameters;
import pambatch.config.SettingsObservers;

/**
 * Small panel in top left of main display for chosing run mode and creating
 * jobs and job sets. 
 * @author dg50
 *
 */
public class JobControlPanel extends BatchPanel {

	private BatchControl batchControl;

	private JButton addButton;

	private JComboBox<BatchMode> batchMode;

	private JButton setButton;

	private BatchParameters batchParams;
	
	private JComboBox<ReprocessStoreChoice> reprocessChoices;

	public JobControlPanel(BatchControl batchControl) {
		super(batchControl);
		this.batchControl = batchControl;
		setBorder(new TitledBorder("Job control"));
		
		batchMode = new JComboBox<BatchMode>();
		BatchMode[] modes = BatchMode.values();
		for (int i = 0; i < modes.length; i++) {
			batchMode.addItem(modes[i]);
		}
		batchMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				batchModeChange();
			}
		});
		
		reprocessChoices = new JComboBox<>();
		ReprocessStoreChoice[] choices = ReprocessStoreChoice.values();
		for (int i = 0; i < choices.length; i++) {
			reprocessChoices.addItem(choices[i]);
		}
		reprocessChoices.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newReprocessChoice();
			}
		});
		
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		
		this.add(new JLabel("Mode: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 3;
		this.add(batchMode, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		this.add(new JLabel("Jobs: ", JLabel.RIGHT), c);
		c.gridx++;
		this.add(addButton = new JButton("Create job"), c);
		c.gridx++;
		this.add(setButton = new JButton("Create set"), c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		this.add(new JLabel("Incomplete sets ..."), c);
		c.gridy++;
		c.gridwidth = 4;
		this.add(reprocessChoices, c);

		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createJob();
			}
		});
		setButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createSet();
			}

		});

		addButton.setToolTipText("Create a single batch processing job");
		setButton.setToolTipText("Create a set of batch jobs from within a common folder structure");
		reprocessChoices.setToolTipText("Options of what to do if there are existing output data (e.g. after a crash or computer restart)");
	}


	protected void createJob() {
		batchControl.createJob();
	}

	private void createSet() {
		batchControl.createJobSet();
	}

	protected void batchModeChange() {
		BatchMode mode = (BatchMode) batchMode.getSelectedItem();
//		if (SMRUEnable.isDevEnable() == false && mode == BatchMode.VIEWER) {
//			WarnOnce.showWarning("Offline tasks not yet supported", 
//					"Currently only supporting raw data processing. Viewer offline tasks will follow shortly", 
//					WarnOnce.WARNING_MESSAGE);
//			batchMode.setSelectedIndex(0);
//			return;
//		}
		batchParams = batchControl.getBatchParameters();
		batchParams.setBatchMode(mode);
		batchControl.settingsChange(SettingsObservers.CHANGE_RUNMODE);
	}


	protected void newReprocessChoice() {
		ReprocessStoreChoice choice = (ReprocessStoreChoice) reprocessChoices.getSelectedItem();
		if (choice != null && batchParams != null) {
			batchParams.setReprocessChoice(choice);
		}
	}

	@Override
	public void setParams(BatchParameters batchParams) {
		this.batchParams = batchParams;
		reprocessChoices.setSelectedItem(batchParams.getReprocessChoice());
		batchMode.setSelectedItem(batchParams.getBatchMode());
	}
}
