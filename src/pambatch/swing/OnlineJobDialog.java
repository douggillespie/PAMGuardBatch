package pambatch.swing;

import java.awt.Window;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;

public class OnlineJobDialog extends JobDialog {

	private static JobDialog singleInstance;
	
	private static int[] jobOrder = {JobDialog.SOURCES, JobDialog.BINARY, JobDialog.DATABASE};
	
	protected OnlineJobDialog(Window parentFrame, BatchControl batchControl) {
		super(parentFrame, batchControl);
		// TODO Auto-generated constructor stub
	}

	public static boolean showDialog(Window parentFrame, BatchControl batchControl, BatchDataUnit batchDataUnit) {
		//		if (singleInstance == null) {
		singleInstance = new OnlineJobDialog(parentFrame, batchControl);
		//		}
		singleInstance.setParams(batchDataUnit);
		singleInstance.setVisible(true);
		return singleInstance.isOk;
	}

	@Override
	public int[] getSelectionOrder() {
		return jobOrder;
	}

	@Override
	protected void selectionChanged(int iSet) {
		// TODO Auto-generated method stub
		
	}
}
