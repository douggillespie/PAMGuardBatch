package pambatch.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.config.BatchJobInfo;

public class SwingMenus {
	
	private BatchControl batchControl;

	public SwingMenus(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
	}

	public JPopupMenu getSwingPopupMenu(BatchDataUnit dataUnit) {
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
		BatchJobInfo jobInfo = dataUnit.getBatchJobInfo();
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
		
		return popMenu;
	};

}
