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
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;

public class JobDialog extends PamDialog {

	private static JobDialog singleInstance;

	private boolean isOk;

	private BatchDataUnit batchDataUnit;

	private JTextField databaseId;

	private JTextField sourceFolder;

	//	private JTextField destBinary;
	//	
	//	private JTextField destDatabase;
	//	
	//	private JButton browseSource, browseBinary, browseDatabase;
	//	
	private JobSet[] jobSets;

	private static final int SOURCES = 0;
	private static final int BINARY = 1;
	private static final int DATABASE = 2;


	private String[] sectionNames = {"Source folder", "Binary folder", "Database file"};
	private String[] tipName = {"Source folder or URI for soundn files to process",
			"Binary folder for output data", "Output database"};

	private BatchControl batchControl;

	private BatchJobInfo jobInfo;


	private JobDialog(Window parentFrame, BatchControl batchControl) {
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
		jobSets = new JobSet[3];
		addJobSet(0, mainPanel, c);
		c.gridy++;
		addJobSet(1, mainPanel, c);
		c.gridy++;
		addJobSet(2, mainPanel, c);
		
		setDialogComponent(mainPanel);
	}

	private void addJobSet(int i, JPanel mainPanel, GridBagConstraints c) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(BorderLayout.WEST, new JLabel(sectionNames[i]));
		JButton selButton;
		topPanel.add(BorderLayout.EAST, selButton = new JButton("Select"));
		selButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectButton(i);
			}
		});
		panel.add(topPanel);
		JTextField mainField = new JTextField(60);
		mainField.setToolTipText(tipName[i]);
		panel.add(mainField);
		mainPanel.add(panel, c);
		jobSets[i] = new JobSet(mainField, selButton);
	}

	protected void selectButton(int iSet) {
		switch(iSet) {
		case SOURCES:
			selectInputSource();
			break;
		case BINARY:
			selectBinary();
			break;
		case DATABASE:
			selectDatabase();
		}

	}

	private void selectInputSource() {
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

		}

	}

	private void selectBinary() {
		PamFileChooser fc = new PamFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		File startLoc = null;
		if (jobSets[BINARY].getText() != null) {
			startLoc = new File(jobSets[BINARY].getText());
			startLoc = PamFolders.getFileChooserPath(startLoc);
			fc.setCurrentDirectory(startLoc);
		}

		int ans = fc.showDialog(this, "Storage folder for binary data");

		if (ans == JFileChooser.APPROVE_OPTION) {
			jobSets[BINARY].setText(fc.getSelectedFile().toString());

		}
	}

	private void selectDatabase() {
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
				return pathname.isDirectory() == false;
			}

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
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

		int state = fileChooser.showOpenDialog(getOwner());
		if (state == JFileChooser.APPROVE_OPTION) {
			File currFile = fileChooser.getSelectedFile();
			if (currFile == null) {
				jobSets[DATABASE].setText(null);
			}
			else {
				jobSets[DATABASE].setText(currFile.getAbsolutePath());
			}
		}

	}

	private class JobSet {

		private JTextField mainField;

		private JButton selectbutton;

		private JobSet(JTextField mainField, JButton selButton) {
			this.mainField = mainField;
			this.selectbutton = selButton;
		}

		private String getText() {
			return mainField.getText();
		}

		private void setText(String text) {
			mainField.setText(text);
		}

	}

	public static boolean showDialog(Window parentFrame, BatchControl batchControl, BatchDataUnit batchDataUnit) {
		//		if (singleInstance == null) {
		singleInstance = new JobDialog(parentFrame, batchControl);
		//		}
		singleInstance.setParams(batchDataUnit);
		singleInstance.setVisible(true);
		return singleInstance.isOk;
	}

	private void setParams(BatchDataUnit batchDataUnit) {
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
		// could do a test to check it exists ? 
		File source = new File(jobInfo.soundFileFolder);
		if (source.exists() == false || source.isDirectory() == false) {
			return showWarning("you must select an existing folder of source audio files");
		}
		if (batchControl.findBinaryStore() != null) {
			jobInfo.outputBinaryFolder = jobSets[BINARY].getText();
			if (jobInfo.outputBinaryFolder == null) {
				return showWarning("you must select a folder for binary output");
			}
		}
		if (batchControl.findDatabaseControl() != null) {
			jobInfo.outputDatabaseName = jobSets[DATABASE].getText();
			if (jobInfo.outputDatabaseName == null) {
				 showWarning("you must select an output database name");
			}
		}
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

}
