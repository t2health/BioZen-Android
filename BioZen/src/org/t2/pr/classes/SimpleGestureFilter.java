/*
 * 
 * Provider Resilience
 * 
 * Copyright © 2009-2012 United States Government as represented by 
 * the Chief Information Officer of the National Center for Telehealth 
 * and Technology. All Rights Reserved.
 * 
 * Copyright © 2009-2012 Contributors. All Rights Reserved. 
 * 
 * THIS OPEN SOURCE AGREEMENT ("AGREEMENT") DEFINES THE RIGHTS OF USE, 
 * REPRODUCTION, DISTRIBUTION, MODIFICATION AND REDISTRIBUTION OF CERTAIN 
 * COMPUTER SOFTWARE ORIGINALLY RELEASED BY THE UNITED STATES GOVERNMENT 
 * AS REPRESENTED BY THE GOVERNMENT AGENCY LISTED BELOW ("GOVERNMENT AGENCY"). 
 * THE UNITED STATES GOVERNMENT, AS REPRESENTED BY GOVERNMENT AGENCY, IS AN 
 * INTENDED THIRD-PARTY BENEFICIARY OF ALL SUBSEQUENT DISTRIBUTIONS OR 
 * REDISTRIBUTIONS OF THE SUBJECT SOFTWARE. ANYONE WHO USES, REPRODUCES, 
 * DISTRIBUTES, MODIFIES OR REDISTRIBUTES THE SUBJECT SOFTWARE, AS DEFINED 
 * HEREIN, OR ANY PART THEREOF, IS, BY THAT ACTION, ACCEPTING IN FULL THE 
 * RESPONSIBILITIES AND OBLIGATIONS CONTAINED IN THIS AGREEMENT.
 * 
 * Government Agency: The National Center for Telehealth and Technology
 * Government Agency Original Software Designation: Provider Resilience001
 * Government Agency Original Software Title: Provider Resilience
 * User Registration Requested. Please send email 
 * with your contact information to: robert.kayl2@us.army.mil
 * Government Agency Point of Contact for Original Software: robert.kayl2@us.army.mil
 * 
 */
package org.t2.pr.classes;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class SimpleGestureFilter extends SimpleOnGestureListener{

	public final static int SWIPE_UP    = 1;
	public final static int SWIPE_DOWN  = 2;
	public final static int SWIPE_LEFT  = 3;
	public final static int SWIPE_RIGHT = 4;

	public final static int MODE_TRANSPARENT = 0;
	public final static int MODE_SOLID       = 1;
	public final static int MODE_DYNAMIC     = 2;

	private final static int ACTION_FAKE = -13; //just an unlikely number
	private int swipe_Min_Distance = 50; //100
	private int swipe_Max_Distance = 1350; //350
	private int swipe_Min_Velocity = 50; //100

	private int mode      = MODE_DYNAMIC;
	private boolean running = true;
	private boolean tapIndicator = false;

	private Activity context;
	private GestureDetector detector;
	private SimpleGestureListener listener;

	public SimpleGestureFilter(Activity context,SimpleGestureListener sgl) {

		this.context = context;
		this.detector = new GestureDetector(context, this);
		this.listener = sgl;
	}

	public void onTouchEvent(MotionEvent event){

		if(!this.running)
			return;  

		boolean result = this.detector.onTouchEvent(event); 

		if(this.mode == MODE_SOLID)
			event.setAction(MotionEvent.ACTION_CANCEL);
		else if (this.mode == MODE_DYNAMIC) {

			if(event.getAction() == ACTION_FAKE)
				event.setAction(MotionEvent.ACTION_UP);
			else if (result)
				event.setAction(MotionEvent.ACTION_CANCEL);
			else if(this.tapIndicator){
				event.setAction(MotionEvent.ACTION_DOWN);
				this.tapIndicator = false;
			}

		}
		//else just do nothing, it's Transparent
	}

	public void setMode(int m){
		this.mode = m;
	}

	public int getMode(){
		return this.mode;
	}

	public void setEnabled(boolean status){
		this.running = status;
	}

	public void setSwipeMaxDistance(int distance){
		this.swipe_Max_Distance = distance;
	}

	public void setSwipeMinDistance(int distance){
		this.swipe_Min_Distance = distance;
	}

	public void setSwipeMinVelocity(int distance){
		this.swipe_Min_Velocity = distance;
	}

	public int getSwipeMaxDistance(){
		return this.swipe_Max_Distance;
	}

	public int getSwipeMinDistance(){
		return this.swipe_Min_Distance;
	}

	public int getSwipeMinVelocity(){
		return this.swipe_Min_Velocity;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		final float xDistance = Math.abs(e1.getX() - e2.getX());
		final float yDistance = Math.abs(e1.getY() - e2.getY());

		if(xDistance > this.swipe_Max_Distance || yDistance > this.swipe_Max_Distance)
			return false;

		velocityX = Math.abs(velocityX);
		velocityY = Math.abs(velocityY);
		boolean result = false;

		if(velocityX > this.swipe_Min_Velocity && xDistance > this.swipe_Min_Distance){
			if(e1.getX() > e2.getX()) // right to left
			this.listener.onSwipe(SWIPE_LEFT);
			else
				this.listener.onSwipe(SWIPE_RIGHT);

			result = true;
		}
		else if(velocityY > this.swipe_Min_Velocity && yDistance > this.swipe_Min_Distance){
			if(e1.getY() > e2.getY()) // bottom to up
				this.listener.onSwipe(SWIPE_UP);
			else
				this.listener.onSwipe(SWIPE_DOWN);

			result = true;
		}

		return result;
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		this.tapIndicator = true;
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent arg0) {
		this.listener.onDoubleTap();;
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent arg0) {
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent arg0) {

		if(this.mode == MODE_DYNAMIC){        // we owe an ACTION_UP, so we fake an
			arg0.setAction(ACTION_FAKE);      //action which will be converted to an ACTION_UP later.
			this.context.dispatchTouchEvent(arg0);
		}   

		return false;
	}

	public static interface SimpleGestureListener{
		void onSwipe(int direction);
		void onDoubleTap();
	}

}
