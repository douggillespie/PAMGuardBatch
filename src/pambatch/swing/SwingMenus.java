package pambatch.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;

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
		
		return popMenu;
	};

}
