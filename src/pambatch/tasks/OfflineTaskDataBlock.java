package pambatch.tasks;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class OfflineTaskDataBlock extends PamDataBlock<OfflineTaskDataUnit> {

	public OfflineTaskDataBlock(PamProcess parentProcess) {
		super(OfflineTaskDataUnit.class, "Offline Tasks", parentProcess, 0);
	}

	@Override
	protected int removeOldUnitsT(long currentTimeMS) {
		return 0;
	}

	@Override
	protected int removeOldUnitsS(long mastrClockSample) {
		return 0;
	}
}
