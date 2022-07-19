# PAMGuardBatch
PAMGuard Batch processing control

Software to control batch processing of one or more PAMGuard datasets, e.g. processing data from many similar autonomous recorders using the same congifuration file, but with each recorders data being in a different place and being written to a different database and binary store, etc. 

Will handle resets after PAMGuard crashes (if ever !) and other nice features such as Windows restarts. 

This will (for now) be a PAMGuard plug in module while I try out a few things, but the code will mostly be written to run stand alone if in the future we want to run the process control on headless platforms. 
