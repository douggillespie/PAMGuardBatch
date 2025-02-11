package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import PamController.PamFolders;
import PamUtils.PamFileChooser;
import PamUtils.SelectFolder;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.warn.WarnOnce;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchMode;

abstract public class JobDialog extends PamDialog {


	protected boolean isOk;

	private BatchDataUnit batchDataUnit;

	private JTextField databaseId;

	private JTextField sourceFolder;
	
	private JButton similarButton;

	//	private JTextField destBinary;
	//	
	//	private JTextField destDatabase;
	//	
	//	private JButton browseSource, browseBinary, browseDatabase;
	//	
	private JobSet[] jobSets;

	protected static final int SOURCES = 0;
	protected static final int BINARY = 1;
	protected static final int DATABASE = 2;

	private String[] sectionNames = {"Source folder", "Binary folder", "Database file"};
	private String[] tipName = {"Source folder or URI for raw data files to process",
			"Binary folder for output data", "Output database"};

	protected BatchControl batchControl;

	private BatchJobInfo jobInfo;


	protected JobDialog(Window parentFrame, BatchControl batchControl) {
		super(parentFrame, "Create new job", false);
		this.batchControl = batchControl;
		databaseId = new JTextField(4);
		databaseId.setEnabled(false);
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Job information"));
		GridBagConstraints c = new PamGridBagContraints();
		mainPanel.add(new JLabel(" Job Id ", JLabel.RIGHT), c);
		c.gridx++;
		mainPanel.add(databaseId, c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		int[] itemOrder = getSelectionOrder();
		jobSets = new JobSet[itemOrder.length];
		
		BatchMode mode = batchControl.getBatchParameters().getBatchMode();
		
		for (int i = 0; i < jobSets.length; i++) {
			boolean showClear = itemOrder[i] != DATABASE && mode == BatchMode.VIEWER; 
			addJobSet(itemOrder[i], mainPanel, showClear, c);
			c.gridy++;
		}
		setResizable(false);
		setDialogComponent(mainPanel);

		setHelpPoint("docs.batchjobs");
	}
	
	/**
	 * Get the order to display the controls in. This can be 
	 * {0,1,2} or {2,1,0}, etc for SOURCES, BINARY and DATABASE
	 * @return
	 */
	public abstract int[] getSelectionOrder();

	private void addJobSet(int i, JPanel mainPanel, boolean showClear, GridBagConstraints c) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel topEast = new JPanel(new GridBagLayout());
		GridBagConstraints c2 = new PamGridBagContraints();
		topPanel.add(BorderLayout.WEST, new JLabel(sectionNames[i]));
		topPanel.add(BorderLayout.EAST, topEast);
		
		JButton selButton, clearButton = null;;
		topEast.add(selButton = new JButton("Select"), c2);
		selButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectButton(i);
			}
		});
		if (showClear) {
			clearButton = new JButton("Clear");
			c2.gridx++;
			topEast.add(clearButton, c2);
			clearButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					clearField(i);
				}
			});
		}
		
		panel.add(topPanel);
		JTextField mainField = new JTextField(60);
		mainField.setToolTipText(tipName[i]);
		panel.add(mainField);
		mainPanel.add(panel, c);
		jobSets[i] = new JobSet(mainField, selButton, clearButton);
	}
	
	protected JobSet getJobSet(int type) {
		return jobSets[type];
	}

	protected void selectButton(int iSet) {
		boolean change = false;
		switch(iSet) {
		case SOURCES:
			change = selectInputSource();
			break;
		case BINARY:
			change = selectBinary();
			break;
		case DATABASE:
			change = selectDatabase();
		}
		if (change) {
			selectionChanged(iSet);
		}
	}

	private void clearField(int i) {
		jobSets[i].mainField.setText(null);
	}

	protected abstract void selectionChanged(int iSet);

	private boolean selectInputSource() {
		PamFileChooser fc = new PamFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		File startLoc = null;
		if (jobSets[SOURCES].getText() != null) {
			startLoc = new File(jobSets[SOURCES].getText());
			startLoc = PamFolders.getFileChooserPath(startLoc);
			fc.setCurrentDirectory(startLoc);
		}

		int ans = fc.showDialog(this, "Audio recording source folder");

		if (ans == JFileChooser.APPROVE_OPTION) {
			jobSets[SOURCES].setText(fc.getSelectedFile().toString());
			return true;
		}
		else {
			return false;
		}
		

	}

	private boolean selectBinary() {
		PamFileChooser fc = new PamFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		File startLoc = null;
		if (jobSets[BINARY].getText() != null && jobSets[BINARY].getText().length() != 0) {
			startLoc = new File(jobSets[BINARY].getText());
			startLoc = PamFolders.getFileChooserPath(startLoc);
		}
		else {
			startLoc = new File(PamFolders.getDefaultProjectFolder());
		}
		fc.setCurrentDirectory(startLoc);

		int ans = fc.showDialog(this, "Storage folder for binary data");

		if (ans == JFileChooser.APPROVE_OPTION) {
			jobSets[BINARY].setText(fc.getSelectedFile().toString());

			return true;
		}
		else {
			return false;
		}
	}

	private boolean selectDatabase() {
		/*
		 *  this one is a bit trickier since the database may / need not actually exist.
		 *  Further, we don't really know what type of database is going to be used in the output. 
		 *  for now, it's pretty much got to be sqlite, but we should really support other types. 
		 *  one problem we have is that we've no idea what type of database is actually used in 
		 *  the external psfx file, though could open up the configuration to find out if 
		 *  we have to. for now, assume sqlite, though don't create it and allow no name. 
		 */
		FileFilter anyFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				String dbPath = pathname.getName();
				if (dbPath.endsWith("sqlite3")) {
					return true;
				}
				return true;
			}

			@Override
			public String getDescription() {
				return "Database Files";
			}
		};
		String current = jobSets[DATABASE].getText();

		JFileChooser fileChooser = new PamFileChooser();
		fileChooser.setFileFilter(anyFilter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		if (current != null && current.length() > 0) {
			File currFile = new File(current);
			fileChooser.setSelectedFile(currFile);
		}
		else {
			fileChooser.setCurrentDirectory(new File(PamFolders.getDefaultProjectFolder()));
		}

		int state = fileChooser.showOpenDialog(getOwner());
		if (state == JFileChooser.APPROVE_OPTION) {
			File currFile = fileChooser.getSelectedFile();
			if (currFile == null) {
				jobSets[DATABASE].setText(null);
			}
			else {
				jobSets[DATABASE].setText(currFile.getAbsolutePath());
			}
			return true;
		}
		else {
			return false;
		}

	}

	protected class JobSet {

		private JTextField mainField;

		private JButton selectbutton;
		
		private JButton clearButton;

		private JobSet(JTextField mainField, JButton selButton, JButton clearButton) {
			this.mainField = mainField;
			this.selectbutton = selButton;
			this.clearButton = clearButton;
			mainField.setEditable(false);
		}

		protected String getText() {
			return mainField.getText();
		}

		protected void setText(String text) {
			mainField.setText(text);
		}

	}


	protected void setParams(BatchDataUnit batchDataUnit) {
		this.batchDataUnit = batchDataUnit;
		int dbId = batchDataUnit.getDatabaseIndex();
		databaseId.setText(String.format("%d", dbId));
		jobInfo = batchDataUnit.getBatchJobInfo();
		if (jobInfo != null) {
			jobSets[SOURCES].setText(jobInfo.soundFileFolder);
			jobSets[BINARY].setText(jobInfo.outputBinaryFolder);
			jobSets[DATABASE].setText(jobInfo.outputDatabaseName);
		}
	}

	@Override
	public boolean getParams() {
		jobInfo = batchDataUnit.getBatchJobInfo();
		if (jobInfo == null) {
			jobInfo = new BatchJobInfo();
		}
		else {
			jobInfo = jobInfo.clone();
		}
		jobInfo.soundFileFolder = jobSets[SOURCES].getText();
		BatchMode batchMode = batchControl.getBatchParameters().getBatchMode();
		// could do a test to check it exists ? 
		File source = new File(jobInfo.soundFileFolder);
		if (source.exists() == false || source.isDirectory() == false) {
			if (batchMode == BatchMode.NORMAL) {
				return showWarning("you must select an existing folder of source audio files");
			}
			else {
				int ans = WarnOnce.showWarning("No source sound files", 
						"Are you sure you want to proceed without any source sound files ?", WarnOnce.OK_CANCEL_OPTION);
				if (ans == WarnOnce.CANCEL_OPTION) {
					return false;
				}
			}
		}
//		if (batchControl.findBinaryStore() != null) {
			jobInfo.outputBinaryFolder = jobSets[BINARY].getText();
			if (jobInfo.outputBinaryFolder == null) {
				return showWarning("you must select a folder for binary output");
			}
//		}
//		if (batchControl.findDatabaseControl() != null) {
			jobInfo.outputDatabaseName = jobSets[DATABASE].getText();
			if (jobInfo.outputDatabaseName == null) {
				 showWarning("you must select an output database name");
			}
//		}
		batchDataUnit.setBatchJobInfo(jobInfo);
		isOk = true;
		return true;
	}

	void enableControls() {
		boolean hasDatabase = batchControl.findDatabaseControl() != null;
		boolean hasBinary = batchControl.findBinaryStore() != null;
		jobSets[BINARY].selectbutton.setEnabled(hasBinary);
		jobSets[DATABASE].selectbutton.setEnabled(hasDatabase);
	}

	@Override
	public void cancelButtonPressed() {
		isOk = false;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHelpPoint() {
		return super.getHelpPoint();
	}

}
