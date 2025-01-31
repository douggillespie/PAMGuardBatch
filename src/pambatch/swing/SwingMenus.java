package pambatch.swing;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchMode;
import pambatch.ctrl.JobController;

public class SwingMenus {
	
	private BatchControl batchControl;

	public SwingMenus(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
	}

	/**
	 * Get a swing menu for the given job and possibly also column name clicked in the
	 * jobs table. 
	 * @param dataUnit job data unit
	 * @param colName job column name. 
	 * @return
	 */
	public JPopupMenu getJobsPopupMenu(BatchDataUnit dataUnit, String colName) {
		
		BatchMode batchMode = batchControl.getBatchParameters().getBatchMode();
		BatchJobInfo jobInfo = dataUnit.getBatchJobInfo();
		
		JPopupMenu popMenu = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Delete Job");
		popMenu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.deleteJob(dataUnit);
			}
		});
		menuItem = new JMenuItem("Edit Job ...");
		popMenu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.editJob(dataUnit);
			}
		});

		boolean haveCal = jobInfo.arrayData != null;
		if (haveCal == false) {
			menuItem = new JMenuItem("Add calibration / array data");
		}
		else {
			menuItem = new JMenuItem("Edit calibration / array data");
		}
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.editJobCalibration(dataUnit);
			}
		});
		popMenu.add(menuItem);
		menuItem = new JMenuItem("Delete job specific calibration / array data");
		menuItem.setEnabled(haveCal);
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.deleteJobCalibration(dataUnit);
			}
		});
		popMenu.add(menuItem);
		
		if (jobInfo.jobStatus == BatchJobStatus.RUNNING) {
			menuItem = new JMenuItem("Stop and close ...");
			popMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					batchControl.cancelJob(dataUnit);
				}
			});
		}
		
		menuItem = new JMenuItem("Open with PAMGuard Viewer");
		popMenu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.launchViewer(jobInfo.outputDatabaseName);
			}
		});
		menuItem.setEnabled((jobInfo.jobStatus == BatchJobStatus.COMPLETE || batchMode == BatchMode.VIEWER) && jobInfo.outputDatabaseName != null);
		menuItem.setToolTipText("Open this dataset with the PAMGuard Viewer");
		
		if (colName != null) {
			String openPath = null;
			String locType = null;
			switch (colName) {
			case "Database":
				openPath = jobInfo.outputDatabaseName;
				locType = "output";
				break;
			case "Binary":
				openPath = jobInfo.outputBinaryFolder;
				locType = "output";
				break;
			case "Source":
				openPath = jobInfo.soundFileFolder;
				locType = "data source";
				break;
			}
			if (openPath != null) {
				File path = new File(openPath);
				while (path != null) {
					// we want the folder, not the file
					if (path.isFile() || path.exists() == false) {
						path = path.getParentFile();
					}
					else {
						break;
					}
				}
				if (path != null && path.isDirectory()) {
					// set up a menu to open this with Explorer. 
					menuItem = new JMenuItem(String.format("Open %s location \"%s\"", locType, path.getName()));
					menuItem.setToolTipText(String.format("Open %s folder ", locType,  path.getAbsolutePath()));
					menuItem.addActionListener(new OpenFolder(path));
					popMenu.add(menuItem);
				}
			}
		}

		if (batchMode == BatchMode.VIEWER && jobInfo.outputDatabaseName != null) {

			menuItem = new JMenuItem("Extract configuration from database ...");
			popMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					batchControl.extractDatabaseConfiguration(jobInfo.outputDatabaseName);
				}
			});
			menuItem.setToolTipText("Extract configuration from database and use as master PSFX");
		}
		//		if (batchMode == BatchMode.NORMAL) {
		popMenu.addSeparator();
//		}
//		JobController jobController = dataUnit.getJobController();
//		if (jobController != null) {
//			menuItem = new JMenuItem("Stop / Kill job");
//			popMenu.add(menuItem);
//			menuItem.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					jobController.killJob();
//				}
//			});
//		}
		if (jobInfo.jobStatus == BatchJobStatus.COMPLETE || jobInfo.jobStatus == BatchJobStatus.CANCELLED || jobInfo.jobStatus == BatchJobStatus.STARTING) {
			menuItem = new JMenuItem("Reprocess job");
			popMenu.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					batchControl.reprocessJob(dataUnit);
				}
			});
		}
		menuItem = new JMenuItem("Reprocess ALL jobs");
		popMenu.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				batchControl.reprocessAllJobs();
			}
		});
		
		
		return popMenu;
	};

	private class OpenFolder implements ActionListener {
		 
		private File folder;

		public OpenFolder(File folder) {
			super();
			this.folder = folder;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Desktop.getDesktop().open(folder);
			} catch (Exception ex) {
				// TODO Auto-generated catch block
//				e1.printStackTrace();
			}
		}
	}
}
