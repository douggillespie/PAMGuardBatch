package pambatch;

import PamguardMVC.PamDataBlock;

public class BatchDataBlock extends PamDataBlock<BatchDataUnit> {

	public BatchDataBlock(BatchProcess parentProcess) {
		super(BatchDataUnit.class, "Batch Jobs", parentProcess, 0);
	}

	@Override
	public void clearAll() {
//		super.clearAll();
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
