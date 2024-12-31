package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamController.PamFolders;
import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import pambatch.BatchControl;
import pambatch.SourceSubFolderInfo;
import pambatch.config.BatchJobInfo;

public class NormalSetDialog extends BatchSetDialog {
		
	
	private JLabel typicalSource;
	
	
	private JLabel typicalBinary;
	
	private JLabel typicalDatabase;
		

	private BatchControl batchControl;

	private ArrayList<SourceSubFolderInfo> sourceSubFolders;
	
	private ArrayList<BatchJobInfo> returnList;


	private NormalSetDialog(Window parentFrame, BatchControl batchControl) {
		super(parentFrame, "Generate a jobs set", false);
		this.batchControl = batchControl;
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Root folders for multiple datasets"));
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 4;
		addJobSet(SOURCES, mainPanel, c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		mainPanel.add(new JLabel("Typical source: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 1;
		mainPanel.add(typicalSource = new JLabel(" ..."), c);

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		addJobSet(BINARY, mainPanel, c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		mainPanel.add(new JLabel("Typical output: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 1;
		mainPanel.add(typicalBinary = new JLabel(), c);

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 4;
		addJobSet(DATABASE, mainPanel, c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		mainPanel.add(new JLabel("Typical output: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 1;
		mainPanel.add(typicalDatabase = new JLabel(), c);
		
		
		setResizable(true);
		setDialogComponent(mainPanel);
		
		setHelpPoint("docs.batchjobs");
	}
	
	public static ArrayList<BatchJobInfo> showDialog(Window parentFrame, BatchControl batchControl) {
		NormalSetDialog dialog = new NormalSetDialog(parentFrame, batchControl);
		dialog.setVisible(true);
		
		return dialog.returnList;
	}

	protected void selectButton(int iJobSet) {
		switch (iJobSet) {
		case 0:
			searchSourceFolder();
			break;
		case BINARY:
			inventDestBinaryFolders();
			break;
		case DATABASE:
			inventDatabaseNames();
			break;
		}
	}

	private void inventDatabaseNames() {
		PamFileChooser fc = getSharedChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		File startLoc = null;
		if (jobSets[DATABASE].getText() != null) {
			startLoc = new File(jobSets[BINARY].getText());
			startLoc = PamFolders.getFileChooserPath(startLoc);
			fc.setCurrentDirectory(startLoc);
		}

		int ans = fc.showDialog(this, "Storage folder for databases");

		if (ans == JFileChooser.APPROVE_OPTION) {
			jobSets[DATABASE].setText(fc.getSelectedFile().toString());
			showBinaryName();
			showDatabaseName();
		}		
		
	}
	
	private void inventDestBinaryFolders() {
		PamFileChooser fc = getSharedChooser();
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
			showBinaryName();
		}		
	}

	private void showBinaryName() {
		String binaryRoot = jobSets[BINARY].getText();
		if (binaryRoot == null) {
			typicalBinary.setText("Define an output root folder for binary data");
			return;
		}
		if (sourceSubFolders == null || sourceSubFolders.size() == 0) {
			typicalBinary.setText("Output folders will be defined when the input root folder is selected");
			return;
		}
		File binRoot = new File(binaryRoot);
		if (binRoot.exists() == false || binRoot.isDirectory() == false) {
			typicalBinary.setText("Root folder for binary output must be an existing folder");
			return;
		}
		File firstSource = sourceSubFolders.get(0).getSourceSubFolder();
		
		String firstOut = inventBinaryName(sourceSubFolders.get(0).getSourceSubFolder(), binRoot);
		firstOut = trimFolderName(binRoot, new File(firstOut));
		typicalBinary.setText(firstOut);
	}
	
	/**
	 * Make a binary folder name based on the source folder and the root for binary output
	 * @param sourceFolder
	 * @param binaryRoot
	 * @return
	 */
	private String inventBinaryName(File sourceFolder, File binaryRoot) {
		if (sourceFolder == null || binaryRoot == null) {
			return null;
		}
		String lastName = sourceFolder.getName();
		String binName = binaryRoot.getAbsolutePath() + File.separator + lastName + "binary";
		return binName;
	}

	private void showDatabaseName() {
		String databaseRoot = jobSets[DATABASE].getText();
		if (databaseRoot == null) {
			typicalBinary.setText("Define an output root folder for binary data");
			return;
		}
		if (sourceSubFolders == null || sourceSubFolders.size() == 0) {
			typicalBinary.setText("Output databse names will be defined when the input root folder is selected");
			return;
		}
		File binRoot = new File(databaseRoot);
		if (binRoot.exists() == false || binRoot.isDirectory() == false) {
			typicalBinary.setText("Root folder for database output must be an existing folder");
			return;
		}
		String dbName = inventDatabaseName(sourceSubFolders.get(0).getSourceSubFolder(), binRoot);
		if (dbName == null) {
			typicalDatabase.setText(null);
		}
		else {
			File temp = new File(dbName);
			dbName = trimFolderName(binRoot, temp);
			String firstOut = dbName + "  (file end will be corrected at run time if needed)";
			typicalDatabase.setText(firstOut);
		}
	}

	/**
	 * Get a default name path for a database file. 
	 * @param sourceFolder
	 * @param databaseRoot
	 * @return
	 */
	private String inventDatabaseName(File sourceFolder, File databaseRoot) {
		if (sourceFolder == null) {
			return null;
		}
		if (databaseRoot == null) {
			return null;
		}
		String lastName = sourceFolder.getName();
		String dbName = databaseRoot.getAbsolutePath() + File.separator + lastName + "database";
		
		return dbName;
	}

	/**
	 * Look for source data. 
	 */
	private void searchSourceFolder() {
		PamFileChooser fc = getSharedChooser();
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
			findSubFolderData(fc.getSelectedFile());
		}
	}

	
	
	private void findSubFolderData(File sourceFolder) {
		sourceSubFolders = batchControl.findSourceSubFolders(sourceFolder);
		if (sourceSubFolders == null || sourceSubFolders.size() == 0) {
			typicalSource.setText("No sub folders found with audio data files");
		}
		else {
			String sub = trimFolderName(sourceFolder, sourceSubFolders.get(0).getSourceSubFolder());
			String str = String.format("%d sub folders contain audio files, e.g. %s", sourceSubFolders.size(), sub);
			typicalSource.setText(str);
			showBinaryName();
			showDatabaseName();
		}
	}
	
	private String trimFolderName(File rootFolder, File subFolder) {
		if (rootFolder == null || subFolder == null) {
			if (subFolder == null) {
				return null;
			}
			else {
				return subFolder.getAbsolutePath();
			}
		}
		String root = rootFolder.getAbsolutePath();
		String sub = subFolder.getAbsolutePath();
		if (sub.startsWith(root)) {
			sub = "." + sub.substring(root.length());
		}
		return sub;
	}


	@Override
	public boolean getParams() {
		if (sourceSubFolders == null || sourceSubFolders.size() == 0) {
			return showWarning("No data source folder root selected");
		}
		String binSource = jobSets[BINARY].getText();
		if (binSource == null) {
			return showWarning("No data output folder for binary data selected");
		}
		File binRoot = new File(binSource);
		if (binRoot.exists() == false || binRoot.isDirectory() == false) {
			return showWarning("Data output folder for binary data must be an existing directory on this computer");
		}
		String dbSource = jobSets[DATABASE].getText();
		if (dbSource == null) {
			return showWarning("No data output folder for databases selected");
		}
		File dbRoot = new File(dbSource) ;
		if (dbRoot.exists() == false || dbRoot.isDirectory() == false) {
			return showWarning("Data output folder for databases must be an existing directory on this computer");
		}
		
		// should now be OK to generate Jobs 
		returnList = new ArrayList<>();
		for (SourceSubFolderInfo sfInf : sourceSubFolders) {
			File source = sfInf.getSourceSubFolder();
			String binary = inventBinaryName(source, binRoot);
			String database = inventDatabaseName(source, dbRoot);
			if (binary == null || database == null) {
				return showWarning("Can't generate output names for " + source.getAbsolutePath());
			}
			
			System.out.printf("Output set Source %s, binary %s, database %s\n", source.getAbsolutePath(), binary, database);
			BatchJobInfo jobInfo = new BatchJobInfo(source.getAbsolutePath(), binary, database);
			returnList.add(jobInfo);
		}
		
		
		return returnList.size() > 0;
	}

	@Override
	public void cancelButtonPressed() {
		returnList = null;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
