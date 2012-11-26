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

/**
 * Transition view with 3 direction animations
 * @author stephenody
 * 
 * based on transitionview.java from Nathan Scandella
 * Copyright 2011 Enscand, Inc.
 */

//import org.t2.pr.R;
import com.t2.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * A transition view provides animated switching of 
 * a predefined set of image resources.
 */
public class TransitionView extends RelativeLayout {

   /** One of the two in-memory art images */
   private ImageView _artView1;
   /** The other of the two in-memory art images */
   private ImageView _artView2;
   /** Length of art view transition animation, in milliseconds */
   private final int ANIMATION_DURATION_MSEC = 600;
   /** The underlying ImageSwitcher that performs transitions */
   private ImageSwitcher _imageSwitcher;

   private Animation animRIn;
   private Animation animROut;
   private Animation animLIn;
   private Animation animLOut;
   private Animation animFIn;
   private Animation animFOut;

   
   /**
    * Create a new instance.
    * @param context The parent context
    */
   public TransitionView(Context context) {
      super(context); 
      customInit(context);
   }
   
   /**
    * Initialize a new instance.
    * @param context The parent context
    */
   private void customInit(Context context) {

      _imageSwitcher = new ImageSwitcher(context);
      animRIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
      animRIn.setDuration(ANIMATION_DURATION_MSEC);
      animROut = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
      animROut.setDuration(ANIMATION_DURATION_MSEC);
      animLIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
      animLIn.setDuration(ANIMATION_DURATION_MSEC);
      animLOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
      animLOut.setDuration(ANIMATION_DURATION_MSEC);
      
      animFIn = AnimationUtils.loadAnimation(context, R.anim.grow_from_middle);
      animFIn.setDuration(ANIMATION_DURATION_MSEC);
      animFOut = AnimationUtils.loadAnimation(context, R.anim.shrink_to_middle);
      animFOut.setDuration(ANIMATION_DURATION_MSEC);
      
      _imageSwitcher.setInAnimation(animLIn);
      _imageSwitcher.setOutAnimation(animLOut);

      _artView1 = new ImageView(context);
      //_artView1.setImageResource(_imageIds[_currentImage]);

      _artView2 = new ImageView(context);
      //_artView2.setImageResource(_imageIds[_currentImage + 1]);

      LayoutParams fullScreenLayout = new LayoutParams(LayoutParams.FILL_PARENT, 
            LayoutParams.FILL_PARENT);
      _imageSwitcher.addView(_artView1, 0, fullScreenLayout);
      _imageSwitcher.addView(_artView2, 1, fullScreenLayout);
      _imageSwitcher.setDisplayedChild(0);
      addView(_imageSwitcher, fullScreenLayout);
   }

   /** @see android.view.View#View(Context, AttributeSet) */
   public TransitionView(Context context, AttributeSet attrs) {
      super(context, attrs);
      customInit(context);
   }

   /** @see android.view.View#View(Context, AttributeSet, int) */
   public TransitionView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
      customInit(context);
   }

   /**
    * Change the currently displayed image
    * @param pageRight if true, the next image will be shown, else the previous image will appear
    */
   public void changePage(int newRESID, int dir) {
	   
	   if(dir == 0)
	   {
		   _imageSwitcher.setInAnimation(animRIn);
		      _imageSwitcher.setOutAnimation(animROut);
	   }
	   else if(dir == 1)
	   {
		   _imageSwitcher.setInAnimation(animLIn);
		      _imageSwitcher.setOutAnimation(animLOut);
	   }
	   else 
	   {
		   _imageSwitcher.setInAnimation(animFIn);
		      _imageSwitcher.setOutAnimation(animFOut);
	   }
	   
      if (_imageSwitcher.getCurrentView() == _artView1) {
         _artView2.setImageResource(newRESID);
         _imageSwitcher.showNext();
      } else {
         _artView1.setImageResource(newRESID);
         _imageSwitcher.showPrevious();
      }
   }
}
