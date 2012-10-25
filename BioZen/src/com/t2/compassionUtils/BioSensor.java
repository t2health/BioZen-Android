/*****************************************************************
BioZen

Copyright (C) 2011 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.compassionUtils;


public class BioSensor {
	
	public static final int CONN_ERROR = -1;
	public static final int CONN_IDLE = 0;
	public static final int CONN_PAIRED = 1;
	public static final int CONN_CONNECTING = 2;
	public static final int CONN_CONNECTED = 3;
	
	public String mBTName;
	public String mBTAddress;
	public int mConnectionStatus;
	public Boolean mEnabled;

	/**
	 * A list of names of all of the parameters that this sensor can supply
	 */
	public String mParameterNames = "";
	
	public BioSensor(String btName, String btAddress, Boolean enabled) {
		this.mBTName = btName;
		this.mBTAddress = btAddress;
		this.mEnabled = enabled;
		
		if (btName.startsWith("BH")) {
			this.mParameterNames = "HeartRate, SkinTemp, RespRate";
		}
		if (btName.startsWith("RN42")) {
			this.mParameterNames = "HeartRate, EMG, GSR";
		}
		if (btName.startsWith("TestSensor")) {
			this.mParameterNames = "HeartRate, EMG, GSR, SkinTemp, RespRate";
		}
	}
	

}
