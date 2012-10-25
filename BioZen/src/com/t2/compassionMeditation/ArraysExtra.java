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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArraysExtra {
	public static final double[] toArray(Double[] vals) {
		if(vals == null) {
			return null;
		}
		
		List<Double> arrList = Arrays.asList(vals);
		arrList.remove(null);
		
		double[] out = new double[arrList.size()];
		for(int i = 0; i < arrList.size(); ++i) {
			Double val = arrList.get(i);
			if(val != null) {
				out[i] = val;
			}
		}
		return out;
	}
	
	public static final String[] toStringArray(Object[] vals) {
		if(vals == null) {
			return null;
		}
		
		String[] out = new String[vals.length];
		for(int i = 0; i < vals.length; ++i) {
			out[i] = vals[i].toString();
		}
		return out;
	}
	
	public static final Long[] toLongArray(String[] vals) {
		if(vals == null) {
			return null;
		}
		
		ArrayList<Long> newVals = new ArrayList<Long>();
		for(int i = 0; i < vals.length; ++i) {
			try {
				Long val = Long.parseLong(vals[i]);
				newVals.add(val);
			} catch (Exception e) {}
		}
		newVals.remove(null);
		
		Long[] out = new Long[newVals.size()];
		for(int i = 0; i < newVals.size(); ++i) {
			out[i] = newVals.get(i);
		}
		
		return out;
	}
	
	public static final Integer[] toIntegerArray(String[] vals) {
		if(vals == null) {
			return null;
		}
		
		ArrayList<Integer> newVals = new ArrayList<Integer>();
		for(int i = 0; i < vals.length; ++i) {
			try {
				Integer val = Integer.parseInt(vals[i]);
				newVals.add(val);
			} catch (Exception e) {}
		}
		newVals.remove(null);
		
		Integer[] out = new Integer[newVals.size()];
		for(int i = 0; i < newVals.size(); ++i) {
			out[i] = newVals.get(i);
		}
		
		return out;
	}
	
	public static final Double[] toDoubleArray(String[] vals) {
		if(vals == null) {
			return null;
		}
		
		ArrayList<Double> newVals = new ArrayList<Double>();
		for(int i = 0; i < vals.length; ++i) {
			try {
				Double val = Double.parseDouble(vals[i]);
				newVals.add(val);
			} catch (Exception e) {}
		}
		newVals.remove(null);
		
		Double[] out = new Double[newVals.size()];
		for(int i = 0; i < newVals.size(); ++i) {
			out[i] = newVals.get(i);
		}
		
		return out;
	}
}
