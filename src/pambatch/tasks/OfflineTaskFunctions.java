package pambatch.tasks;

import java.util.ListIterator;

import Array.PamArray;
import PamController.PamControlledUnit;
import PamUtils.PamUtils;
import offlineProcessing.OfflineTask;
import pambatch.BatchControl;
import pambatch.BatchDataBlock;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;
import pambatch.config.ExternalConfiguration;
import tethys.TethysControl;
import tethys.tasks.TethysTask;

/**
 * Useful functions for offline task control which might get called 
 * from the GUI, or batch controller. 
 * @author dg50
 *
 */
public class OfflineTaskFunctions {
	
	private BatchControl batchControl;

	public OfflineTaskFunctions(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
	}

	/**
	 * Can a task run ? Returns null if OK, or a string saying why not. 
	 * @param taskDataUnit
	 * @return null if task is good to go, or a String saying why not. 
	 */
	public String canRun(OfflineTaskDataUnit taskDataUnit) {
		OfflineTask aTask = taskDataUnit.getOfflineTask();
		if (aTask == null) {
			return "No task set";
		}
	
		String moduleProblem = moduleSpecificProblems(taskDataUnit);
		if (moduleProblem != null) {
			return moduleProblem;
		}
		
		if (aTask.canRun() == false) {
			String whyNot = aTask.whyNot();
			if (PamUtils.emptyString(whyNot)) {
				return "Unknown reason";
			}
			else {
				return whyNot;
			}
		}	
		
		return null;
	}

	/**
	 * Module specific stuff. Should probably move Batch into core, then can more easily have tasks 
	 * do batch processor specific stuff within their own classes. Maybe
	 * @param taskDataUnit
	 * @return null if OK, useful error otherwise. 
	 */
	private String moduleSpecificProblems(OfflineTaskDataUnit taskDataUnit) {
		OfflineTask aTask = taskDataUnit.getOfflineTask();
		PamControlledUnit module = aTask.getTaskControlledUnit();
		if (module instanceof TethysControl && aTask instanceof TethysTask) {
			return tethysChecks(taskDataUnit);
		}
		return null;
	}

	private String tethysChecks(OfflineTaskDataUnit taskDataUnit) {
		TethysTask aTask = (TethysTask) taskDataUnit.getOfflineTask();
		PamControlledUnit module = aTask.getTaskControlledUnit();
		// check the project information (think this is OK, already handled for deployments)
//		ExternalConfiguration extConfig = batchControl.getExternalConfiguration();
		
		// check the instrument type and id is set for everything
		BatchDataBlock jobBlock = batchControl.getBatchProcess().getBatchDataBlock();
		ListIterator<BatchDataUnit> it = jobBlock.getListIterator(0);
		while (it.hasNext()) {
			BatchDataUnit jobUnit = it.next();
			BatchJobInfo jobInfo = jobUnit.getBatchJobInfo();
			PamArray array = batchControl.findJobArray(jobUnit);
			if (array == null) {
				return String.format("Job %d does not have a defined array", jobUnit.getDatabaseIndex());
			}
			String instType = array.getInstrumentType();
			String instId = array.getInstrumentId();
			if (PamUtils.emptyString(instType) || PamUtils.emptyString(instId)) {
				String err = String.format("Job %d does not have an Instrument Type of Instrument Id set in the array Manager", jobUnit.getDatabaseIndex());
				err = String.format("<html>%s<br>This information must be set in the external configuration, or individually for each job</html>", err);
				return err;
			}
		}
		
		return null;
	}
}
