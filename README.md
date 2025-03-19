# PAMGuardBatch
PAMGuard Batch processing control

Software to control batch processing of one or more PAMGuard datasets, e.g. processing data from many similar autonomous recorders using the same congifuration file, but with each recorders data being in a different place and being written to a different database and binary store, etc. 
Will handle resets after PAMGuard crashes (if ever !) and other nice features such as Windows restarts. 

This will (for now) be a PAMGuard plug in module while I try out a few things, but the code will mostly be written to run stand alone if in the future we want to run the process control on headless platforms. 

[Installation instructions.](https://www.pamguard.org/plugins/plugins.html) 

[Built module download.](https://www.pamguard.org/plugins/batchprocessor.html) 

[Online help.](https://www.pamguard.org/batchhelp/docs/batchoverview.html)

## Version 2.0

March 2025

PAMGuard 2.02.16

Updated version with support for offline task processing, e.g. click reclassification, export to Tethys, etc. 

## Version 1.6

December 2024

PAMGuard 2.02.15

First release of PAMGuard batch processing module which allows the same configuration file to berun on multiple datasets. 

To install, download the jar file then copy the jar file into your plugins folder (e.g. C:\Program Files\Pamguard\plugins) from where PAMGuard will automatically add it to the list of available modules in the Utilities menu next time it starts.

See online help for usage instructions. 