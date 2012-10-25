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

 public class RateOfChange {
	 private float circularBuffer[];
	 	private float mean;
	 	private float instantChange;
        private int circularIndex;
        private int count;

        public RateOfChange(int size) {
            circularBuffer = new float[size];
            reset();
        }

        public float getValue() {
        	float total = 0;
        	int len = count < circularBuffer.length  ? count: circularBuffer.length;
        	for (int i = 0; i < len - 1; ++i) {
        		float v1 = circularBuffer[i];
        		float v2 = circularBuffer[nextIndex(i)];
        		float diff = Math.abs(v2 - v1);
        		total += diff;
            }

           float  roc = total / (float) len;
           return roc;
        }


        public void pushValue(float x) {
            if (count++ == 0) {
                primeBuffer(0);
            }
            float lastValue = circularBuffer[circularIndex];
            instantChange = x - lastValue;
            circularBuffer[circularIndex] = x;
            circularIndex = nextIndex(circularIndex);
        }

        public void reset() {
            count = 0;
            circularIndex = 0;
            mean = 0;
        }

        public long getCount() {
            return count;
        }

        private void primeBuffer(float val) {
            for (int i = 0; i < circularBuffer.length; ++i) {
                circularBuffer[i] = val;
            }
            mean = val;
        }

        private int nextIndex(int curIndex) {
            if (curIndex + 1 >= circularBuffer.length) {
                return 0;
            }
            return curIndex + 1;
        }
    }