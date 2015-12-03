/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

	public UserImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public UserImageView(Context context, boolean shouldSmash, boolean isCoin) {
		super(context);
		
		setShouldSmash(shouldSmash);
		setIsCoin(isCoin);
		
		upMovementAnimatorSet = new AnimatorSet();
		downMovementAnimatorSet = new AnimatorSet();
	}

	void stopMovementAnimations() {
		upMovementAnimatorSet.cancel();
		downMovementAnimatorSet.cancel();
	}

	void scaleUp(AnimatorListener animatorListener) {
		ValueAnimator scaleAnimationX = ObjectAnimator.ofFloat(this, "scaleX", 25f);
		ValueAnimator scaleAnimationY = ObjectAnimator.ofFloat(this, "scaleY", 25f);
		scaleAnimationX.setDuration(1000);
		scaleAnimationY.setDuration(1000);
		scaleAnimationX.setInterpolator(new LinearInterpolator());
		scaleAnimationY.setInterpolator(new LinearInterpolator());

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(scaleAnimationX, scaleAnimationY);
		animatorSet.start();

		animatorSet.addListener(animatorListener);
	}

	void stopRotationAnimation() {
		rotationAnimation.cancel();
	}

	void setupAndStartAnimations(int iconWidth, int iconHeight, int screenWidth, int screenHeight, AnimatorListener downAnimatorListener) {
	    Random randomGenerator = new Random(System.currentTimeMillis());

        ValueAnimator upAnimationX;
        ValueAnimator upAnimationY;
        final ValueAnimator downAnimationX;
        final ValueAnimator downAnimationY;

        int leftXExtreme = -iconWidth*3;
        int rightXExtreme = screenWidth+(iconWidth*2);

        int bottomY = screenHeight+iconHeight;
        int topYLowerExtreme = (int) (screenHeight*0.3);
        int topYUpperExtreme = 0;
        
        int centerX = (screenWidth-iconWidth)/2 + iconWidth - randomGenerator.nextInt(iconWidth*2);
        
        int leftX = randomGenerator.nextInt(centerX-leftXExtreme) + leftXExtreme;
        int rightX = rightXExtreme - randomGenerator.nextInt(rightXExtreme-centerX);
        
        int topY = randomGenerator.nextInt(topYLowerExtreme-topYUpperExtreme) + topYUpperExtreme;
        
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
        
        if (randomGenerator.nextInt(2) == 0) {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        }
        else {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, -360f);
        }
        rotationAnimation.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimation.setDuration(rotationTime);
        rotationAnimation.setInterpolator(new LinearInterpolator());
        
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
        
        downAnimationY.addListener(downAnimatorListener);
    	
        upMovementAnimatorSet.start();
        rotationAnimation.start();
	}

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
