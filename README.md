BioZen
===============

[Official Site: http://www.t2health.org](http://t2health.org)

**Note for cloning project:
	BioZen used to contain three sub-modules. Checking it out put the sub-modules as children to BioZen when they should be siblings (because other projects use them)
	To obtain all of the code for BioZen you must clone the following repositories:
		https://github.com/t2health/BioZen-Android (This module)
		https://github.com/t2health/BSPAN---Bluetooth-Sensor-Processing-for-Android
		https://github.com/t2health/T2AndroidLib-SG		
		https://github.com/t2health/T2SensorLib-Android	

BioZen is one of the first mobile applications to provide users with live biofeedback data from multiple wearable body sensors covering a range of biophysiological signals, including electroencephalogram (EEG), electromyography (EMG), galvanic skin response (GSR), electrocardiogram (ECG or EKG), respiratory rate, and temperature biofeedback data and display it on a mobile phone.

Using BioZen requires compatible biosensor devices (see listing below). These devices and BioZen are not designed or intended for psychological therapy or medical treatments.

BioZen can display several brain wave bands (Delta, Theta, Alpha, Beta, and Gamma) separately, as well as combinations of several bands using algorithms that may indicate relevant cognitive states, such as meditation and attention. BioZen features a meditation module that represents biometric information with user-selectable graphics that change in response to the user's biometric data.

Biofeedback Android Apps using BSPAN:
    - [BioZen](http://www.t2health.org/apps/biozen)

Sensors Supported
==============
NeuroSky:

- [MindSet](http://neurosky.com/Products/MindSet.aspx)
- [MindBand](http://neurosky.com/Products/MindBand.aspx)
- [BrainAthlete](http://neurosky.com/Products/BrainAthlete.aspx)
	
Zephyr:

- [BioHarness](http://www.zephyr-technology.com/bioharness-bt)
	
[Shimmer](http://www.shimmer-research.com/):

- GSR
- Accelerometer
- Timestamp
- Range

Submitting bugs
===============
If you think you've found a bug, please report it by following these instructions:  

1. Visit the [Issue tracker: https://github.com/t2health/BioZen-Android/issues](https://github.com/t2health/BioZen-Android/issues)
2. Create an issue explaining the problem and expected result
    - Be sure to include any relevant information for reproducing the issue
    - Include information such as:
        * Device (with version #)
        * The version of code you're running
        * If you are running from a git version, include the date and/or hash number
3. Submit the issue.

Submitting patches
==================
To contribute code and bug fixes to BioZen: fork this project on Github, make changes to the code in your fork, 
and then send a "pull request" to notify the team of updates that are ready to be reviewed for inclusion.

Detailed instructions can be found at [Patching](https://gist.github.com/1507418)

Getting Started with BioZen
==============================================
Clone the repo on your computer

Instructions for using Eclipse:

1. Click Help > Eclipse Marketplace
2. Download Egit
3. Window > Show View > Other
4. Open Git Highlight Git Repositories
5. Click OK
6. Copy our Git url https://github.com/t2health/BioZen-Android.git
7. Right click in the Git Repositories view in Eclipse
8. Click Paste Repository Path or URI and follow the steps
9. Right click on the project and Import it into your workspace.

License
==============================================
Copyright 2012 The National Center for Telehealth and Technology

BioZen is Licensed under the EPLv1: [http://www.opensource.org/licenses/EPL-1.0](http://www.opensource.org/licenses/EPL-1.0)

SPINE is Licensed under the LGPLv2.1: [http://opensource.org/licenses/LGPL-2.1](http://opensource.org/licenses/LGPL-2.1)
