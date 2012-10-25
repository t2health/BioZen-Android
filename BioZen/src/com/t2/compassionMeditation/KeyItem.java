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
package com.t2.compassionMeditation;

import java.util.HashMap;

import org.achartengine.model.XYSeries;

import com.t2.compassionUtils.TMovingAverageFilter;
import com.t2.compassionUtils.RateOfChange;


public class KeyItem {
	public long id;
	public String title1;
	public String title2;
	public int color;
	public boolean visible;
	public boolean reverseData = false; 
    private TMovingAverageFilter mMovingAverage = new TMovingAverageFilter(10);
    private RateOfChange mRateOfChange = new RateOfChange(6);
	public XYSeries xySeries;	    
    

	

	public int rawValue;
	public int scaledValue;
	public int filteredValue;

	private int maxFilteredValue = 0;
	private int minFilteredValue = 9999;
	private int numFilterSamples = 0;
	private long totalOfFilterSamples = 0;

	
	
	public int getMaxFilteredValue() {
		return maxFilteredValue;
	}


	public void setMaxFilteredValue(int maxFilteredValue) {
		this.maxFilteredValue = maxFilteredValue;
	}


	public int getMinFilteredValue() {
		return minFilteredValue;
	}


	public void setMinFilteredValue(int minFilteredValue) {
		this.minFilteredValue = minFilteredValue;
	}


	public int getAvgFilteredValue() {
		return numFilterSamples != 0 ? (int) (totalOfFilterSamples / numFilterSamples) :0;
	}



	public int getRawValue() {
		return rawValue;
	}


	public void setRawValue(int rawValue) {
		this.rawValue = rawValue;
	}


	public int getScaledValue() {
		return scaledValue;
	}

	public int getFilteredScaledValue() {
		return (int) mMovingAverage.getValue();
	}

	public int getRateOfChangeScaledValue() {
		int filteredLotusValue = (int) (mRateOfChange.getValue() * 10);

		if (filteredLotusValue > 255) filteredLotusValue = 255;
		
		return filteredLotusValue;
	}


	public void updateRateOfChange() {
		mRateOfChange.pushValue(scaledValue);
	}
	
	public void setScaledValue(int scaledValue) {
		this.scaledValue = scaledValue;
		mMovingAverage.pushValue(scaledValue);
		
		// Now do stats
		int value = (int) mMovingAverage.getValue();
		numFilterSamples++;
		totalOfFilterSamples += value;
		
		if (value >= maxFilteredValue) maxFilteredValue = value;
		if (value < minFilteredValue) minFilteredValue = value;
	}


	
	
	public KeyItem(long id, String title1, String title2) {
		this.id = id;
		this.title1 = title1;
		this.title2 = title2;
		this.visible = true;
		xySeries = new XYSeries(title1);		
		
	}
	
	
	public HashMap<String,Object> toHashMap() {
		HashMap<String,Object> data = new HashMap<String,Object>();
		data.put("id", id);
		data.put("title1", title1);
		data.put("title2", title2);
		data.put("color", color);
		data.put("visible", visible);
		return data;
	}
}	