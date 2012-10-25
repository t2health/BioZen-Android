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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class ToggledButton extends Button {

	private boolean isChecked = false;
	private int[] initialState;
	
	public ToggledButton(Context context) {
		super(context);
		this.init();
	}

	public ToggledButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.init();
	}

	public ToggledButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.init();
	}

	private void init() {
		initialState = super.getDrawableState();
	}
	
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
		this.refreshDrawableState();
	}

	public boolean isChecked() {
		return isChecked;
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		int[] states;
		
		if(this.isChecked()) {
			states = Button.PRESSED_WINDOW_FOCUSED_STATE_SET;
		} else {
			if(super.hasFocus()) {
				states = super.onCreateDrawableState(extraSpace);
			} else {
				states = initialState;
			}
		}
		
		return states;
	}
	
}
