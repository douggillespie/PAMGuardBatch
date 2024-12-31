package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;

import PamController.PamFolders;
import PamUtils.PamFileChooser;
import PamUtils.PamFileFilter;
import PamUtils.worker.PamWorkDialog;
import PamUtils.worker.PamWorkProgressMessage;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.warn.WarnOnce;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import pambatch.BatchControl;
import pambatch.config.BatchJobInfo;
import pambatch.ctrl.ViewerDatabase;

public class ViewerSetDialog extends BatchSetDialog {

	private static ViewerSetDialog singleInstance;
	private JLabel typicalDatabase;
	private JLabel typicalSoundFile, typicalBinary;
	
	private String[] dbEnds = {".sqlite", ".sqlite3"};
	private ArrayList<BatchJobInfo> foundSets;

	private ViewerSetDialog(Window guiFrame, BatchControl batchControl) {
		super(guiFrame, "Generate multiple jobs", false);
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Root folder for multiple Viewer databases"));
		GridBagConstraints c = new PamGridBagContraints();

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		addJobSet(DATABASE, mainPanel, c);
		c.gridy++;
		c.gridx = 0;
//		c.gridwidth = 1;
//		mainPanel.add(new JLabel("Example database: ", JLabel.LEFT), c);
//		c.gridx++;
		c.gridwidth = 4;
		mainPanel.add(typicalDatabase = new JLabel("Example: "), c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		mainPanel.add(new JLabel("Binary folders and source file locations will be extracted from the databases"), c);
		c.gridy++;
		c.gridx = 0;
		mainPanel.add(typicalBinary = new JLabel(" "), c); 
		c.gridy++;
		c.gridx = 0;
		mainPanel.add(typicalSoundFile = new JLabel(" "), c); 

		setDialogComponent(mainPanel);
	}


	public static ArrayList<BatchJobInfo> showDialog(Frame guiFrame, BatchControl batchControl) {
		//		if (singleInstance == null) {
		singleInstance = new ViewerSetDialog(guiFrame, batchControl);
		//		}
		singleInstance.setVisible(true);
		return singleInstance.foundSets;
	}


	@Override
	public boolean getParams() {
		return foundSets != null && foundSets.size() > 0;
	}

	@Override
	public void cancelButtonPressed() {
		foundSets = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}


	@Override
	protected void selectButton(int i) {		
		PamFileChooser fc = getSharedChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		File startLoc = null;
		if (jobSets[DATABASE].getText() != null) {
			startLoc = new File(jobSets[DATABASE].getText());
			startLoc = PamFolders.getFileChooserPath(startLoc);
			fc.setCurrentDirectory(startLoc);
		}

		int ans = fc.showDialog(this, "folder of Viewer databases");

		if (ans == JFileChooser.APPROVE_OPTION) {
			jobSets[DATABASE].setText(fc.getSelectedFile().toString());
			findDatabaseFiles(fc.getSelectedFile());
		}
	}
	
	private void findDatabaseFiles(File selectedFolder) {
		PamFileFilter fileFilter = new PamFileFilter("Database files", dbEnds[0]);
		fileFilter.setAcceptFolders(false);
		for (int i = 1; i < dbEnds.length; i++) {
			fileFilter.addFileType(dbEnds[i]);
		}
		File[] dbFiles = selectedFolder.listFiles(fileFilter);
		dbFiles = removeBatchDatabase(dbFiles);
		if (dbFiles == null || dbFiles.length == 0) {
			String warn = "No database files found in folder " + selectedFolder.getAbsolutePath();
			WarnOnce.showWarning("No database files found", warn, WarnOnce.OK_OPTION);
			typicalDatabase.setToolTipText(null);
			typicalDatabase.setText("No databases found in folder");
			return;
		}

		extractJobSets(dbFiles);
	}

	/**
	 * Don't include the batch database if it was in the same folder. 
	 * @param dbFiles
	 * @return
	 */
	private File[] removeBatchDatabase(File[] dbFiles) {
		DBControl dbControl = DBControlUnit.findDatabaseControl();
		if (dbControl == null) {
			return dbFiles;
		}
		String dbName = dbControl.getDatabaseName();
		if (dbName == null) {
			return null;
		}
		File[] newList = new File[dbFiles.length]; 
		int used = 0;
		int remove = -1;
		for (int i = 0; i < dbFiles.length; i++) {
			String absFile = dbFiles[i].getAbsolutePath();
			if (absFile.endsWith(dbName)) {
				remove = i;
				continue;
			}
			else {
				newList[used++] = dbFiles[i];
			}
		}
		if (remove >= 0) {
			newList = Arrays.copyOf(newList, newList.length-1);
		}
		
		return newList;
	}
	
	/*
	 *  pull out all the batch job sets. Might take a while 
	 *  so do in a worker thread.  
	 */
	private void extractJobSets(File[] dbFiles) {
		ArrayList<BatchJobInfo> jobSets = new ArrayList<>();
		JobWorker jobWorker = new JobWorker(dbFiles, jobSets);
		jobWorker.execute();
		
	}
	
	public void jobsListUpdated(ArrayList<BatchJobInfo> jobSets) {
		this.foundSets = jobSets;
		if (jobSets == null || jobSets.size() == 0) {
			typicalDatabase.setToolTipText(null);
			typicalDatabase.setText("No databases found in folder");
			return;			
		}
		// and update the information in the dialog now.		
		BatchJobInfo firstJob = jobSets.get(0);
		String firstName = firstJob.outputDatabaseName;
		if (firstName != null) {
			File firstDB = new File(firstName);
			firstName = firstDB.getName();
		}
		String txt = String.format("Found %d databsaes, e.g. %s", jobSets.size(), firstName);
		String tip = "<html>All databases:";
		for (int i = 0; i < jobSets.size(); i++) {
			tip += "<br>" + jobSets.get(i).outputDatabaseName;
		}
		tip += "</html>";
		typicalDatabase.setToolTipText(tip);
		typicalDatabase.setText(txt);
		// try to get a valid set from at least one database. 
		if (firstJob != null) {
			String folder = firstJob.soundFileFolder;
			if (folder == null) {
				typicalSoundFile.setText("No sound files for this dataset (this may be OK)");
			}
			else {
				typicalSoundFile.setText("Sound file folder: " + folder);
			}
			folder = firstJob.outputBinaryFolder;
			if (folder == null) {
				typicalBinary.setText("No binary file data for this dataset (this may be OK)");
			}
			else {
				typicalBinary.setText("Example Binary folder: " + folder + " ");
			}
		}
		pack();
	}

	private class JobWorker extends SwingWorker<Integer, Integer> {

		private ArrayList<BatchJobInfo> jobSets;
		private File[] dbFiles;
		PamWorkDialog workDialog;

		public JobWorker(File[] dbFiles, ArrayList<BatchJobInfo> jobSets) {
			this.dbFiles = dbFiles;
			this.jobSets = jobSets;
		}

		@Override
		protected Integer doInBackground() throws Exception {
			int done = 0;
			for (int i = 0; i < dbFiles.length; i++) {
				this.publish(done);
				BatchJobInfo jobSet = ViewerDatabase.extractJobInfo(dbFiles[i].getAbsolutePath());
				if (jobSet != null) {
					jobSets.add(jobSet);
				}
				done++;
			}
			this.publish(done);
			return done;
		}

		@Override
		protected void process(List<Integer> chunks) {
			for (Integer d : chunks) {
				if (workDialog == null) {
					workDialog = new PamWorkDialog(singleInstance, 1, "Extracting information from databases");
					workDialog.setVisible(true);
				}
				String msg = null;
				if (d < dbFiles.length) {
					msg = "Processing " + dbFiles[d].getName();
				}
				else {
					msg = "Processing complete";
				}
				PamWorkProgressMessage pMsg = new PamWorkProgressMessage(d*100/dbFiles.length, msg);
				workDialog.update(pMsg);
				if (d == dbFiles.length) {
					workDialog.setVisible(false);
				}
			}
		}

		@Override
		protected void done() {
			super.done();
			workDialog.setVisible(false);
			jobsListUpdated(jobSets);
		}
		
	}

}
