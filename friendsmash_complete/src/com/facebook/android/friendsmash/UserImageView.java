/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android.friendsmash;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import java.util.Random;

/**
 *  ImageViews of the users that the playing user has to smash.  These can contain images of one
 *  of the user's friends (in the social version only) or images of celebrities
 */
public class UserImageView extends ImageView {

	private boolean shouldSmash;
	private boolean isCoin;
	private boolean wrongImageSmashed = false;
	private boolean isVoid = false;
	private int extraPoints = 0;
	private AnimatorSet upMovementAnimatorSet;
	private AnimatorSet downMovementAnimatorSet;
	private ValueAnimator rotationAnimation;
	
	// Default Constructor - not used
	public UserImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// Constructor used by GameFragment to pass in an instance of itself
	public UserImageView(Context context, boolean shouldSmash, boolean isCoin) {
		super(context);
		
		setShouldSmash(shouldSmash);
		setIsCoin(isCoin);
		
		upMovementAnimatorSet = new AnimatorSet();
		downMovementAnimatorSet = new AnimatorSet();
	}
	
	// Stop movement (not rotation) animations
	void stopMovementAnimations() {
		upMovementAnimatorSet.cancel();
		downMovementAnimatorSet.cancel();
	}
	
	// Scale image up
	void scaleUp(AnimatorListener animatorListener) {
		// Create the scaling animations
		ValueAnimator scaleAnimationX = ObjectAnimator.ofFloat(this, "scaleX", 25f);
		ValueAnimator scaleAnimationY = ObjectAnimator.ofFloat(this, "scaleY", 25f);
		scaleAnimationX.setDuration(1000);
		scaleAnimationY.setDuration(1000);
		scaleAnimationX.setInterpolator(new LinearInterpolator());
		scaleAnimationY.setInterpolator(new LinearInterpolator());
		
		// Start the animations together
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(scaleAnimationX, scaleAnimationY);
		animatorSet.start();
		
		// Set the listener on this animation
		animatorSet.addListener(animatorListener);
	}
	
	// Stop rotation animation
	void stopRotationAnimation() {
		rotationAnimation.cancel();
	}
	
	// Start firing this UserImageView across the GameFragment view
	void setupAndStartAnimations(int iconWidth, int iconHeight, int screenWidth, int screenHeight, AnimatorListener downAnimatorListener) {
		// Animations with Property Animator - Android 3.0 onwards only ...
		
		// Instantiate Random Generator
        Random randomGenerator = new Random(System.currentTimeMillis());
        
        // Declare Animators
        ValueAnimator upAnimationX;
        ValueAnimator upAnimationY;
        final ValueAnimator downAnimationX;
        final ValueAnimator downAnimationY;
        
		// Calculate coordinates ...
        
        // X Range:
        int leftXExtreme = -iconWidth*3;
        int rightXExtreme = screenWidth+(iconWidth*2);
        
        // Y Range:
        int bottomY = screenHeight+iconHeight;
        int topYLowerExtreme = (int) (screenHeight*0.3);
        int topYUpperExtreme = 0;
        
        // Generate random centerX value
        int centerX = (screenWidth-iconWidth)/2 + iconWidth - randomGenerator.nextInt(iconWidth*2);
        
        // Generate random leftX and rightX values
        int leftX = randomGenerator.nextInt(centerX-leftXExtreme) + leftXExtreme;
        int rightX = rightXExtreme - randomGenerator.nextInt(rightXExtreme-centerX);
        
        // Generate random topY value
        int topY = randomGenerator.nextInt(topYLowerExtreme-topYUpperExtreme) + topYUpperExtreme;
        
        // Generate random time taken to rotate fully (in ms)
        int rotationTime = randomGenerator.nextInt(2500) + 500;
        
        if (randomGenerator.nextInt(2) == 0) {
        	upAnimationX = ObjectAnimator.ofFloat(this, "x", leftX, centerX);
        	upAnimationY = ObjectAnimator.ofFloat(this, "y", bottomY, topY);
        	downAnimationX = ObjectAnimator.ofFloat(this, "x", centerX, centerX+(centerX-leftX));
        	downAnimationY = ObjectAnimator.ofFloat(this, "y", topY, bottomY);
        } else {
        	upAnimationX = ObjectAnimator.ofFloat(this, "x", rightX, centerX);
        	upAnimationY = ObjectAnimator.ofFloat(this, "y", bottomY, topY);
        	downAnimationX = ObjectAnimator.ofFloat(this, "x", centerX, centerX-(rightX-centerX));
        	downAnimationY = ObjectAnimator.ofFloat(this, "y", topY, bottomY);
        }
        
        upAnimationX.setDuration(1500);
        upAnimationY.setDuration(1500);
        upAnimationX.setInterpolator(new LinearInterpolator());
        upAnimationY.setInterpolator(new DecelerateInterpolator());
        
        downAnimationX.setDuration(1500);
        downAnimationY.setDuration(1500);
        downAnimationX.setInterpolator(new LinearInterpolator());
        downAnimationY.setInterpolator(new AccelerateInterpolator());
        
        upMovementAnimatorSet.playTogether(upAnimationX, upAnimationY);
        
        // Rotation animations
        if (randomGenerator.nextInt(2) == 0) {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        }
        else {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, -360f);
        }
        rotationAnimation.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimation.setDuration(rotationTime);
        rotationAnimation.setInterpolator(new LinearInterpolator());
        
        // Create a callback after the up animation has ended to start the down animation
        upAnimationY.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				downMovementAnimatorSet.playTogether(downAnimationX, downAnimationY);
				downMovementAnimatorSet.start();
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
        
        // Create a callback after the animation has ended
        downAnimationY.addListener(downAnimatorListener);
    	
        // Play the animations
        upMovementAnimatorSet.start();
        rotationAnimation.start();
	}

	
	/* Standard Getters & Setters */
	
	public boolean shouldSmash() {
		return shouldSmash;
	}

	public void setShouldSmash(boolean shouldSmash) {
		this.shouldSmash = shouldSmash;
	}
	
	public boolean isCoin() {
		return isCoin;
	}

	public void setIsCoin(boolean isCoin) {
		this.isCoin = isCoin;
	}

	public boolean isVoid() {
		return isVoid;
	}

	public void setVoid(boolean isVoid) {
		this.isVoid = isVoid;
	}

	public int getExtraPoints() {
		return extraPoints;
	}

	public void setExtraPoints(int extraPoints) {
		this.extraPoints = extraPoints;
	}
	
	public boolean isWrongImageSmashed() {
		return wrongImageSmashed;
	}

	public void setWrongImageSmashed(boolean wrongImageSmashed) {
		this.wrongImageSmashed = wrongImageSmashed;
	}

}
