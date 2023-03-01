package pambatch.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import PamView.dialog.PamGridBagContraints;
import pambatch.BatchControl;
import pambatch.config.BatchParameters;

public class JobControlPanel extends BatchPanel {

	private BatchControl batchControl;
	
//	private JTextField maxJobs;
	
	private JSpinner jobsSpinner;
	
	private JButton addButton;

	private NumberEditor editor;

	private SpinnerNumberModel model;

private JButton setButton;

private BatchParameters batchParams;

	public JobControlPanel(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		setBorder(new TitledBorder("Job control"));
//		maxJobs = new JTextField(3);
		jobsSpinner = new JSpinner();
		editor = new JSpinner.NumberEditor(jobsSpinner);
		jobsSpinner.setEditor(editor);
//		jobsSpinner.setEditor(maxJobs);
		model = new SpinnerNumberModel(2, 1, 10, 1);
		jobsSpinner.setModel(model);
//		SpinnerModel spinnerModel = jobsSpinner.getModel();
//		if (spinnerModel instanceof SpinnerNumberModel) {
//			SpinnerNumberModel numberModel = (SpinnerNumberModel) spinnerModel;
//			numberModel.setMinimum(1);
//			numberModel.setMaximum(10);
//		}
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 2;
		this.add(new JLabel("Max concurrent processes ", JLabel.RIGHT), c);
		c.gridx+=c.gridwidth;
		c.gridwidth = 1;
		this.add(jobsSpinner, c);
		
		c.gridx = 0;
		c.gridy++;
		this.add(addButton = new JButton("Create job"), c);
		c.gridx+=c.gridwidth;
		c.gridwidth = 2;
		this.add(setButton = new JButton("Create set"), c);
		
		jobsSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				jobCountChange();
			}
		});
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
		jobsSpinner.setToolTipText("Set the maximum number of jobs that can run concurrently on this computer");
	}

	protected void jobCountChange() {
		Object jobValue = jobsSpinner.getValue();
		if (jobValue instanceof Integer) {
			if (batchParams != null) {
				batchParams.setMaxConcurrentJobs((Integer) jobValue);
			}
		}
	}

	protected void createJob() {
		batchControl.createJob();
	}
	
	private void createSet() {
		batchControl.createJobSet();
	}

	@Override
	public void setParams(BatchParameters batchParams) {
//		maxJobs.setText(String.format("%d", batchParams.getMaxConcurrentJobs()));
		this.batchParams = batchParams;
//		model.setValue(batchParams.getMaxConcurrentJobs());
		jobsSpinner.setValue(batchParams.getMaxConcurrentJobs());
	}
}
