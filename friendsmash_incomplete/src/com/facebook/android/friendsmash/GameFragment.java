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

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

/**
 *  Fragment shown once a user starts playing a game
 */
public class GameFragment extends Fragment {
	
	private static final Pair [] CELEBS = {
		Pair.create("Einstein", "drawable/nonfriend_1"),
		Pair.create("Xzibit", "drawable/nonfriend_2"),
		Pair.create("Goldsmith", "drawable/nonfriend_3"),
		Pair.create("Sinatra", "drawable/nonfriend_4"),
		Pair.create("George", "drawable/nonfriend_5"),
		Pair.create("Jacko", "drawable/nonfriend_6"),
		Pair.create("Rick", "drawable/nonfriend_7"),
		Pair.create("Keanu", "drawable/nonfriend_8"),
		Pair.create("Arnie", "drawable/nonfriend_9"),
		Pair.create("Jean-Luc", "drawable/nonfriend_10"),
	};
	
	// Tag used when logging messages
    private static final String TAG = GameFragment.class.getSimpleName();
	
    
	// FrameLayout as the container for the game
	private FrameLayout gameFrame;

	// FrameLayout of the progress container to show the spinner
	private FrameLayout progressContainer;
	
	// TextView for the Smash Player title
	private TextView smashPlayerNameTextView;
	
	// TextView for the score
	private TextView scoreTextView;
	
	// LinearyLayout containing the lives images
	private LinearLayout livesContainer;
	
	// LinearyLayout containing the bombs images
	private LinearLayout bombsContainer;
	
	// ImageView acting as the button for exploding a bomb
	private ImageView bombButton;

	// Icon width for the friend images to smash
	private int iconWidth;

	// Screen Dimensions
	private int screenWidth;
	private int screenHeight;
	
	
	// Handler for putting messages on Main UI thread from background threads periodically
	private Handler timerHandler;
	
	// Handler for putting messages on Main UI thread from background thread after fetching images
	private Handler uiHandler;
	
	// Runnable task used to produce images to fly across the screen
	private Runnable fireImageTask = null;
	
	// Boolean indicating whether images have started firing
	private boolean imagesStartedFiring = false;
	
	
	// Index of the friend to smash (in the social game)
	private int friendToSmashIndex = -1;
	
	// Index of the celeb to smash (in the non-social game)
	private int celebToSmashIndex = -1;
	
	// ID of the friend to smash (if passed in as an attribute)
	private String friendToSmashIDProvided = null;
	
	// Name of the friend to smash
	private String friendToSmashFirstName = null;
	
	// Bitmap of the friend to smash
	private Bitmap friendToSmashBitmap;
	
	
	// Score for the user
	private int score = 0;
	
	// Lives the user has remaining
	private int lives = 3;
	
	// Bombs the user has remaining
	private int bombsRemaining = FriendSmashApplication.NUM_BOMBS_ALLOWED_IN_GAME;
	
	// Bombs the user has used
	private int bombsUsed = 0;
	
	// Coins the user has collected
	private int coinsCollected = 0;

	// Boolean set to true if first image has been fired
	private boolean firstImageFired = false;
	
	// Boolean indicating that the first image to be fired is pending (i.e. a Request is
	// in the process of executing in a background thread to fetch the images / information)
	private boolean firstImagePendingFiring = false;
	
	
	// List of UserImageView objects created and visible
	private ArrayList<UserImageView> userImageViews = new ArrayList<UserImageView>();
	

	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		// Instantiate the handlers
		timerHandler = new Handler();
		uiHandler = new Handler();
		
		// Get the friend to smash bitmap and name
		if (FriendSmashApplication.IS_SOCIAL) {
			// User is logged into FB, so choose a random FB friend to smash
			friendToSmashIndex = getRandomFriendIndex();
		} else {
			// User is not logged into FB, so choose a random celebrity to smash
			celebToSmashIndex = getRandomCelebIndex();
		}
	}
	
	@SuppressWarnings({ "deprecation" })
	@TargetApi(13)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_game, parent, false);
		
		gameFrame = (FrameLayout)v.findViewById(R.id.gameFrame);
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);
		smashPlayerNameTextView = (TextView)v.findViewById(R.id.smashPlayerNameTextView);
		scoreTextView = (TextView)v.findViewById(R.id.scoreTextView);
		livesContainer = (LinearLayout)v.findViewById(R.id.livesContainer);
		bombsContainer = (LinearLayout)v.findViewById(R.id.bombsContainer);
		bombButton = (ImageView)v.findViewById(R.id.bombButton);
		bombButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
			public boolean onTouch(View v, MotionEvent event) {
            	onBombButtonTouched();
				return false;
			}
        });
		
		// Set the progressContainer as invisible by default
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Set the icon width (for the images to be smashed)
		setIconWidth(getResources().getDimensionPixelSize(R.dimen.icon_width));
		
		// Set the screen dimensions
		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= 13) {
			Point size = new Point();
			display.getSize(size);
			setScreenWidth(size.x);
			setScreenHeight(size.y);
		}
		else {
			setScreenWidth(display.getWidth());
			setScreenHeight(display.getHeight());
		}
				
		// Instantiate the fireImageTask for future fired images
		fireImageTask = new Runnable()
		{
			public void run()
			{
				spawnImage(false);
			}
		};
		
		// Refresh the score board
		setScore(getScore());
		
		// Refresh the lives
		setLives(getLives());

		// Refresh the bombs
		setBombsRemaining(getBombsRemaining());

		// Note: Images will start firing in the onResume method below
		
		return v;
	}
	
	// Called when the Bombs button is touched
	private void onBombButtonTouched() {
		if (getBombsRemaining() > 0) {
			// Hide all ImageViews and invalidate them
			hideAllUserImageViews();
			markAllUserImageViewsAsVoid();
			
			// Increment the bombsUsed integer
			bombsUsed++;
			
			// Subtract a bomb
			setBombsRemaining(getBombsRemaining() - 1);
		} else {
			// No bombs are remaining, so make sure they are all removed from the UI
			setBombsRemaining(0);
		}
	}

	// Sets the name of the player to smash in the top left TextView
	@SuppressWarnings("unused")
	private void setSmashPlayerNameTextView() {
		// Set the Smash Player Name title
        if (FriendSmashApplication.IS_SOCIAL) {
			// User is logged into FB ...
        	if (friendToSmashFirstName == null) {
        		// A name hasn't been set yet (i.e. it hasn't been fetched through a passed in id, so
        		// a random friend needs to be used instead, so fetch this name
        		String friendToSmashName = ((FriendSmashApplication) getActivity().getApplication()).getFriend(friendToSmashIndex).getName();
            	friendToSmashFirstName = friendToSmashName.split(" ")[0];
        	}
        	smashPlayerNameTextView.setText("Smash " + friendToSmashFirstName + " !");
		} else {
			// User is not logged into FB ...
			smashPlayerNameTextView.setText("Smash " + CELEBS[celebToSmashIndex].first + " !");
		}
	}
	
	// Select a random friend to smash
	private int getRandomFriendIndex() {
		Random randomGenerator = new Random(System.currentTimeMillis());
		int friendIndex = randomGenerator.nextInt(((FriendSmashApplication) getActivity().getApplication()).getFriends().size());
		return friendIndex;
	}
	
	// Select a random celebrity to smash (in the non-social game) or avoid smashing (in the social game)
	private int getRandomCelebIndex() {
		Random randomGenerator = new Random(System.currentTimeMillis());
		int celebIndex = randomGenerator.nextInt(CELEBS.length);
		return celebIndex;
	}
	
	// Set the image on the UserImageView to the specified bitmap of the user's friend and fire it
	private void setFriendImageAndFire(UserImageView imageView, Bitmap friendBitmap, boolean extraImage) {
		imageView.setImageBitmap(friendBitmap);
		
		if (extraImage) {
			// If this is an extra image, give it an extra point when smashed
			imageView.setExtraPoints(1);
		}
		
		fireImage(imageView, extraImage);
	}
	
	// Set the image on the UserImageView to the celebrity and fire it
	private void setCelebImageAndFire(UserImageView imageView, int celebIndex, boolean extraImage) {
	    int imageResource = getResources().getIdentifier((String) CELEBS[celebIndex].second, null, getActivity().getPackageName());

	    Drawable image = getResources().getDrawable(imageResource);
	    imageView.setImageDrawable(image);
	    
	    fireImage(imageView, extraImage);
	}
	
	// Set the image on the UserImageView to the coin and fire it
	private void setCoinImageAndFire(UserImageView imageView, boolean extraImage) {
	    imageView.setImageResource(R.drawable.coin);
	    fireImage(imageView, extraImage);
	}

	// Fire the UserImageView and setup the timer to start another image shortly (as long as the image that
	// is fired isn't an extra image)
	private void fireImage(final UserImageView imageView, boolean extraImage) {
		// Fire image
	    imageView.setupAndStartAnimations(iconWidth, iconWidth, screenWidth, screenHeight, new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!imageView.isWrongImageSmashed()) {
					if (imageView.getVisibility() == View.VISIBLE && imageView.shouldSmash() && !imageView.isVoid() && !imageView.isCoin()) {
						// Image is still visible, so user didn't smash it and they should have done (and it isn't void), so decrement the lives by one
						setLives(getLives() - 1);
					}
					
					// Only hide this if the wrong image has not been smashed (otherwise, other logic will be run and image still needs to be shown)
					hideAndRemove(imageView);
				}
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
	    
	    if (!extraImage) {
	    	// If this isn't an extra image spawned, fire another image shortly
	    	fireAnotherImage();
	    }
	    
		// By this point, all network calls would have executed and the first image has fired with the next lined up
		// , so set firstImagePendingFiring to false
		firstImagePendingFiring = false;
	}
	
	// If this UserImageView is currently visible, hide it and remove it from the GameFragment view
	// and the list storing all UserImageViews
	private void hideAndRemove(UserImageView userImageView) {
		if (userImageView.getVisibility() == View.VISIBLE) {
			// Ensure it is hidden
			userImageView.setVisibility(View.GONE);
		}
		
		// Remove the userImageView from the gameFrame
		getGameFrame().removeView(userImageView);
		
		// Remove it from the userImageViews List in the gameFragment
		getUserImageViews().remove(userImageView);
	}
	
	// Fire another image shortly
	private void fireAnotherImage() {
		// Fire another image shortly ...
 		if (fireImageTask != null)
 		{
 			timerHandler.postDelayed(fireImageTask, 700);
 		}
	}

	// Called when the first image should be fired (only called during onResume)
	// If the game has been deep linked into (i.e. a user has clicked on a feed post or request in
	// Facebook), then fetch the specific user that should be smashed
	@SuppressWarnings("unused")
	private void fireFirstImage() {
		if (FriendSmashApplication.IS_SOCIAL) {
			// Get any bundle parameters there are
			Bundle bundle = getActivity().getIntent().getExtras();
			
			String requestID = null;
			String userID = null;
			int numBombsRemaining = 0;
			if (bundle != null) {
				requestID = bundle.getString("request_id");
				userID = bundle.getString("user_id");
				numBombsRemaining = bundle.getInt("num_bombs") <= FriendSmashApplication.NUM_BOMBS_ALLOWED_IN_GAME ?
						bundle.getInt("num_bombs") : FriendSmashApplication.NUM_BOMBS_ALLOWED_IN_GAME;
				setBombsRemaining(numBombsRemaining);
			}
			
			if (requestID != null && friendToSmashIDProvided == null) {
				// Deep linked from request
				// Make a request to get a specific user to smash if they haven't been fetched already
				
				// Show the spinner for this part
				progressContainer.setVisibility(View.VISIBLE);
				
				// Get and set the id of the friend to smash and start firing the image
				fireFirstImageWithRequestID(requestID);
			} else if (userID != null && friendToSmashIDProvided == null) {
				// Deep linked from feed post
				// Make a request to get a specific user to smash if they haven't been fetched already
				
				// Show the spinner for this part
				progressContainer.setVisibility(View.VISIBLE);
				
				// Get and set the id of the friend to smash and start firing the image
				fireFirstImageWithUserID(userID);
			} else {
				// requestID is null, userID is null or friendToSmashIDProvided is already set,
				// so use the randomly generated friend of the user or the already set friendToSmashIDProvided
				// So set the smashPlayerNameTextView text and hide the progress spinner as there is nothing to fetch
				progressContainer.setVisibility(View.INVISIBLE);			
				setSmashPlayerNameTextView();
				
				// Now you're ready to fire the first image
				spawnImage(false);
			}
		} else {
			// Non-social, so set the smashPlayerNameTextView text and hide the progress spinner as there is nothing to fetch
			progressContainer.setVisibility(View.INVISIBLE);			
			setSmashPlayerNameTextView();
			
			// Now you're ready to fire the first image
			spawnImage(false);
		}
	}
	
	// Fires the first image in a game with a given request id  (from a user deep linking by clicking
	// on a request from a specific user)
	private void fireFirstImageWithRequestID(String requestID) {
		final Session session = Session.getActiveSession();
		Request requestIDGraphPathRequest = Request.newGraphPathRequest(session, requestID, new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					closeAndHandleError(error);
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Extract the user id from the response
						GraphObject graphObject = response.getGraphObject();
						JSONObject fromObject = (JSONObject)graphObject.getProperty("from");
						try {
							friendToSmashIDProvided = fromObject.getString("id");
						} catch (JSONException e) {
							Log.e(FriendSmashApplication.TAG, e.toString());
							closeAndShowError(getResources().getString(R.string.network_error));
						}
						
						// With the user id, fetch and set their name
						Request userGraphPathRequest = Request.newGraphPathRequest(session, friendToSmashIDProvided, new Request.Callback() {

							@Override
							public void onCompleted(Response response) {
								FacebookRequestError error = response.getError();
								if (error != null) {
									Log.e(FriendSmashApplication.TAG, error.toString());
									closeAndHandleError(error);
								} else if (session == Session.getActiveSession()) {
									if (response != null) {
										// Extract the user name from the response
										friendToSmashFirstName = response.getGraphObjectAs(GraphUser.class).getFirstName();
									}
									if (friendToSmashFirstName != null) {
										// If the first name of the friend to smash has been set, set the text in the smashPlayerNameTextView
										// and hide the progress spinner now that the user's details have been fetched
										progressContainer.setVisibility(View.INVISIBLE);
										setSmashPlayerNameTextView();
										
										// Now you're ready to fire the first image
										spawnImage(false);
									}
								}
							}
						});
						Request.executeBatchAsync(userGraphPathRequest);
					}
				}
			}
		});
		Request.executeBatchAsync(requestIDGraphPathRequest);
	}
	
	// Fires the first image in a game with a given user id  (from a user deep linking by clicking
	// on a feed post from a specific user)
	private void fireFirstImageWithUserID(String userID) {
		final Session session = Session.getActiveSession();
		
		// With the user id, fetch and set their name, then start the firing of images
		friendToSmashIDProvided = userID;
		Request userGraphPathRequest = Request.newGraphPathRequest(session, friendToSmashIDProvided, new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					closeAndHandleError(error);
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Extract the user name from the response
						friendToSmashFirstName = response.getGraphObjectAs(GraphUser.class).getFirstName();
					}
					if (friendToSmashFirstName != null) {
						// If the first name of the friend to smash has been set, set the text in the smashPlayerNameTextView
						// and hide the progress spinner now that the user's details have been fetched
						progressContainer.setVisibility(View.INVISIBLE);
						setSmashPlayerNameTextView();
						
						// Now you're ready to fire the first image
						spawnImage(false);
					}
				}
			}
		});
		Request.executeBatchAsync(userGraphPathRequest);
	}
	
	// Spawn a new UserImageView, set its bitmap (fetch it from Facebook if it hasn't already been fetched)
	// and fire it once the image has been set (and fetched if appropriate)
	@SuppressWarnings("unused")
	private void spawnImage(final boolean extraImage) {
		// Instantiate Random Generator
        Random randomGenerator = new Random(System.currentTimeMillis());
        
        // 1 in every 5 images should be a celebrity the user should not smash - calculate that here
        // Unless it is the first image fired, in which case it should always be the smashable image
        boolean shouldSmash = true;
        boolean isCoin = false;
        if (firstImageFired) {
        	if (randomGenerator.nextInt(5) == 4 && firstImageFired) {
            	shouldSmash = false;
            } else if (randomGenerator.nextInt(8) == 7 && firstImageFired) {
            	isCoin = true;
            }
        } else if (!firstImageFired) {
        	shouldSmash = true;
        	firstImageFired = true;
        }
		
		// Create a new ImageView with a user to smash
        final UserImageView userImageView = (new UserImageView(getActivity(), shouldSmash, isCoin));
        userImageView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (userImageView.shouldSmash()) {
					// Smashed the right image ...
					
					if (userImageView.isCoin()) {
						coinsCollected++;
					} else {
						// Increment the score
						setScore(getScore() + 1 + userImageView.getExtraPoints());
					}
					
					// Hide the userImageView
					v.setVisibility(View.GONE);
					
					// Remove it from the userImageViews List in this GameFragment
					getUserImageViews().remove(v);
				} else {
					// Smashed the wrong image ...
					wrongImageSmashed(userImageView);
				}
				return false;
			}
		});
        userImageView.setLayoutParams(new LinearLayout.LayoutParams(iconWidth, iconWidth));
        gameFrame.addView(userImageView);
        userImageViews.add(userImageView);
        
        // Set the bitmap of the userImageView ...
        if (userImageView.shouldSmash()) {
        	// The user should smash this image, so set the correct image
        	if (userImageView.isCoin()) {
        		setCoinImageAndFire(userImageView, extraImage);
        	} else {
		        if (FriendSmashApplication.IS_SOCIAL) {
					// User is logged into FB ...
					if (friendToSmashBitmap != null) {
						// Bitmap for the friend to smash has already been retrieved, so use this
						setFriendImageAndFire(userImageView, friendToSmashBitmap, extraImage);
					} else {
						// Otherwise, the Bitmap for the friend to smash hasn't been retrieved, so retrieve it and set it
						
						// Show the spinner while retrieving
						progressContainer.setVisibility(View.VISIBLE);
						
						// If a friend has been passed in, use that attribute, otherwise use the random friend that has been selected
						final String friendToSmashID = friendToSmashIDProvided != null ? friendToSmashIDProvided :
							((FriendSmashApplication) getActivity().getApplication()).getFriend(friendToSmashIndex).getId();
						
						// Fetch the bitmap and fire the image
						fetchFriendBitmapAndFireImages(userImageView, friendToSmashID, extraImage);
					}
				} else {
					// User is not logged into FB ...
					setCelebImageAndFire(userImageView, celebToSmashIndex, extraImage);
				}
        	}
        } else {
        	// The user should not smash this image, so set it to a random celebrity (but not the one being shown if it's the non-social game)
        	int randomCelebToSmashIndex;
        	do {
        		randomCelebToSmashIndex = randomGenerator.nextInt(CELEBS.length);
        	} while (randomCelebToSmashIndex == celebToSmashIndex);
        	setCelebImageAndFire(userImageView, randomCelebToSmashIndex, extraImage);
        }
	}
	
	// Logic when the user smashes this image, but it turns out to be the wrong image - i.e.
	// it's shouldSmash boolean is false
	private void wrongImageSmashed(final UserImageView userImageView) {
		// Set this flag for checking in the animation ended logic
		userImageView.setWrongImageSmashed(true);
		
		// Stop all movement (not rotation) animations for this UserImageView
		userImageView.stopMovementAnimations();
		
		// Stop all animations for all other visible UserImageViews (and therefore hide them)
		hideAllUserImageViewsExcept(userImageView);
		
		// Scale the image up
		userImageView.scaleUp(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				// Cancel rotation and exit to home screen
				userImageView.stopRotationAnimation();
				setLives(0);
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
		
		// Ensure this UserImageView is in front
		getGameFrame().bringChildToFront(userImageView);
	}
	
	private void fetchFriendBitmapAndFireImages(final UserImageView userImageView, final String friendToSmashID, final boolean extraImage) {
		 closeAndShowError("Complete the Personalize Chapter of the Friend Smash Tutorial first!");
	}
	
	// Close the game and show the specified error to the user
	private void closeAndShowError(String error) {
		Bundle bundle = new Bundle();
		bundle.putString("error", error);
		
		Intent i = new Intent();
		i.putExtras(bundle);
		
		getActivity().setResult(Activity.RESULT_CANCELED, i);
		getActivity().finish();
	}
	
	// Close the game and show the specified FacebookRequestException to the user
	private void closeAndHandleError(FacebookRequestError error) {
		// Store the FacebookRequestError in the FacebookApplication before closing out this GameFragment so that
		// it is shown to the user once exited
		((FriendSmashApplication) getActivity().getApplication()).setGameFragmentFBRequestError(error);
		
		getActivity().setResult(Activity.RESULT_CANCELED);
		getActivity().finish();
	}
	
	// Hides all the UserImageViews currently on display - called when a bomb is detonated
	void hideAllUserImageViews() {
		Iterator<UserImageView> userImageViewsIterator = userImageViews.iterator();
		while (userImageViewsIterator.hasNext()) {
			UserImageView currentUserImageView = (UserImageView) userImageViewsIterator.next();
			currentUserImageView.setVisibility(View.GONE);
		}
	}
	
	// Hide all the UserImageViews currently on display except the one specified
	// Called when the user has smashed the wrong image so that this is displayed large
	void hideAllUserImageViewsExcept(UserImageView userImageView) {
		// Stop new animations
		timerHandler.removeCallbacks(fireImageTask);
		
		// Stop animations on all existing visible UserImageViews (which will hide them automatically)
		Iterator<UserImageView> userImageViewsIterator = userImageViews.iterator();
		while (userImageViewsIterator.hasNext()) {
			UserImageView currentUserImageView = (UserImageView) userImageViewsIterator.next();
			if (!currentUserImageView.equals(userImageView)) {
				currentUserImageView.setVisibility(View.GONE);
			}
		}
	}
	
	// Mark all the existing visible UserImageViews as void (called when the game is paused)
	private void markAllUserImageViewsAsVoid() {
		Iterator<UserImageView> userImageViewsIterator = userImageViews.iterator();
		while (userImageViewsIterator.hasNext()) {
			UserImageView currentUserImageView = (UserImageView) userImageViewsIterator.next();
			currentUserImageView.setVoid(true);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();

		// Stop the firing images
		stopTheFiringImages();
	}
	
	@SuppressWarnings("unused")
	@Override
	public void onResume() {
		super.onResume();

		// Stop any firing images (even though this is called in onPause, there might be new firing images
		// if they were pending while onPause was called
		stopTheFiringImages();
		
		if (!imagesStartedFiring) {
			// Fire first image
			if (FriendSmashApplication.IS_SOCIAL) {
				// Only fire for the social game if there isn't a first image pending firing
				if (!firstImagePendingFiring) {
					// ... and also set the firstImagePendingFiring to true - will be set back
					// to false later once the images have actually started firing (i.e. all network
					// calls have executed) - note, this is only relevant for the social version
					firstImagePendingFiring = true;
					imagesStartedFiring = true;
					fireFirstImage();
				}
			} else {
				imagesStartedFiring = true;
				fireFirstImage();
			}
		}
	}
	
	// Stop the firing of all images (and mark the existing ones as void) - called when the game is paused
	private void stopTheFiringImages() {
		// Mark all existing in flight UserImageViews as void (so they don't affect the user's lives once landed)
		markAllUserImageViewsAsVoid();
		
		// Stop new animations and indicate that images have not started firing
		timerHandler.removeCallbacks(fireImageTask);
		imagesStartedFiring = false;
	}
	
	// Get the current score
	int getScore() {
		return score;
	}

	// Set the score and if the score is divisible by 10, spawn more images ...
	// ... the higher the score, the more images that will be spawned
	void setScore(int score) {
		this.score = score;
		
		// Update the scoreTextView
		scoreTextView.setText("Score: " + score);
		
		// If they start scoring well, spawn more images
		if (score > 0 && score % 10 == 0) {
			// Every multiple of 10, spawn extra images ...
			for (int i=0; i<score/20; i++) {
				spawnImage(true);
			}
		}
	}

	// Get the user's number of lives they have remaining
	int getLives() {
		return lives;
	}

	// Set the number of lives that the user has, update the display appropriately and
	// end the game if they have run out of lives
	void setLives(int lives) {
		this.lives = lives;
		
		if (getActivity() != null) {
			// Update the livesContainer
			livesContainer.removeAllViews();
			for (int i=0; i<lives; i++) {
				ImageView heartImageView = new ImageView(getActivity());
				heartImageView.setImageResource(R.drawable.heart_red);
			    livesContainer.addView(heartImageView);
			}
			
			if (lives <= 0) {
				// User has no lives left, so end the game, passing back the score
				Bundle bundle = new Bundle();
				bundle.putInt("score", getScore());
				bundle.putInt("coins_collected", coinsCollected);
				bundle.putInt("bombs_used", bombsUsed);
				
				Intent i = new Intent();
				i.putExtras(bundle);
			
				getActivity().setResult(Activity.RESULT_OK, i);
				getActivity().finish();
			}
		}
	}
	
	// Get the user's number of bombs they have remaining
	int getBombsRemaining() {
		return bombsRemaining;
	}

	// Set the number of bombs that the user has remaining for use in this game,
	// update the display appropriately and hide the bomb button if they have none left
	void setBombsRemaining(int bombsRemaining) {
		this.bombsRemaining = bombsRemaining;
		
		if (getActivity() != null) {
			// Update the bombsContainer
			bombsContainer.removeAllViews();
			for (int i=0; i<bombsRemaining; i++) {
				ImageView bombImageView = new ImageView(getActivity());
				bombImageView.setImageResource(R.drawable.bomb_in_game);
				bombsContainer.addView(bombImageView);
			}
			
			if (bombsRemaining <= 0) {
				// User has no bombs left, so hide the bomb button
				getGameFrame().removeView(bombButton);
			}
		}
	}
	
	/* Standard Getters & Setters */
	
	public int getIconWidth() {
		return iconWidth;
	}

	public void setIconWidth(int iconWidth) {
		this.iconWidth = iconWidth;
	}
	
	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}
	
	public FrameLayout getGameFrame() {
		return gameFrame;
	}
	
	public ArrayList<UserImageView> getUserImageViews() {
		return userImageViews;
	}
}
