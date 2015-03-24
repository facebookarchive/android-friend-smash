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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 *  Fragment shown once a user opens the scoreboard
 */
public class ScoreboardFragment extends Fragment {
	
	// Tag used when logging messages
	private static final String TAG = ScoreboardFragment.class.getSimpleName();
	
    // Store the Application (as you can't always get to it when you can't access the Activity - e.g. during rotations)
	private FriendSmashApplication application;
    
	// LinearLayout as the container for the scoreboard entries
	private LinearLayout scoreboardContainer;
	
	// FrameLayout of the progress container to show the spinner
	private FrameLayout progressContainer;
	
	// Handler for putting messages on Main UI thread from background thread after fetching the scores
	private Handler uiHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		application = (FriendSmashApplication) getActivity().getApplication();
		
		// Instantiate the handler
		uiHandler = new Handler();
		
		setRetainInstance(true);
	}
	
	@TargetApi(13)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_scoreboard, parent, false);
		
		scoreboardContainer = (LinearLayout)v.findViewById(R.id.scoreboardContainer);
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);

		// Set the progressContainer as invisible by default
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Note: Scoreboard is populated during onResume below
		
		return v;
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
	
	@Override
	public void onResume() {
		super.onResume();

		// Populate scoreboard - fetch information if necessary ...
		if (application.getScoreboardEntriesList() == null) {
			// scoreboardEntriesList is null, so fetch the information from Facebook (scoreboard will be updated in
			// the scoreboardEntriesFetched callback) and show the progress spinner while doing so
			progressContainer.setVisibility(View.VISIBLE);
			fetchScoreboardEntries();
		} else {
			// Information has already been fetched, so populate the scoreboard
			populateScoreboard();
		}
	}
	
	// Fetch a List of ScoreboardEntry objects with the scores and details
	// of the user and their friends' scores who have played FriendSmash
	private void fetchScoreboardEntries () {
		String fbAppID = application.getFBAppID();		
		final Session session = Session.getActiveSession();				

		Request scoresGraphPathRequest = Request.newGraphPathRequest(session, 
				fbAppID + "/scores" , 
				new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(TAG, error.toString());
					// TODO: Show an error or handle it better.
					//((ScoreboardActivity)getActivity()).handleError(error, false);
				} else if (session == Session.getActiveSession()) {
					if (response != null) {						
						GraphObject graphObject = response.getGraphObject();
						JSONArray dataArray = (JSONArray)graphObject.getProperty("data");

						ArrayList<ScoreboardEntry> scoreboardEntriesList = new ArrayList<ScoreboardEntry>();

						for (int i=0; i< dataArray.length(); i++) {
							JSONObject oneData = dataArray.optJSONObject(i);							
							int score = oneData.optInt("score");
							
							JSONObject userObj = oneData.optJSONObject("user");							
							String userID = userObj.optString("id");
							String userName = userObj.optString("name");
							
							scoreboardEntriesList.add(new ScoreboardEntry(userID, userName, score));
						}
						
						Comparator<ScoreboardEntry> comparator = Collections.reverseOrder();
						Collections.sort(scoreboardEntriesList, comparator);
						application.setScoreboardEntriesList(scoreboardEntriesList);
						
						// Populate the scoreboard on the UI thread
						uiHandler.post(new Runnable() {
							@Override
							public void run() {
								populateScoreboard();
							}
						});
					}
				}
			}
		});
		Request.executeBatchAsync(scoresGraphPathRequest);
	}

	private void populateScoreboard() {
		// Ensure all components are firstly removed from scoreboardContainer
		scoreboardContainer.removeAllViews();
		
		// Ensure the progress spinner is hidden
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Ensure scoreboardEntriesList is not null and not empty first
		if (application.getScoreboardEntriesList() == null || application.getScoreboardEntriesList().size() <= 0) {
			closeAndShowError(getResources().getString(R.string.error_no_scores));
		} else {
			// Iterate through scoreboardEntriesList, creating new UI elements for each entry
			int index = 0;
			Iterator<ScoreboardEntry> scoreboardEntriesIterator = application.getScoreboardEntriesList().iterator();
			while (scoreboardEntriesIterator.hasNext()) {
				// Get the current scoreboard entry
				final ScoreboardEntry currentScoreboardEntry = scoreboardEntriesIterator.next();
				
				// FrameLayout Container for the currentScoreboardEntry ...
				
				// Create and add a new FrameLayout to display the details of this entry
				FrameLayout frameLayout = new FrameLayout(getActivity());
				scoreboardContainer.addView(frameLayout);
				
				// Set the attributes for this frameLayout
				int topPadding = getResources().getDimensionPixelSize(R.dimen.scoreboard_entry_top_margin);
				frameLayout.setPadding(0, topPadding, 0, 0);
				
				// ImageView background image ...
				{
					// Create and add an ImageView for the background image to this entry
					ImageView backgroundImageView = new ImageView(getActivity());
					frameLayout.addView(backgroundImageView);
					
					// Set the image of the backgroundImageView
					String uri = "drawable/scores_stub_even";
					if (index % 2 != 0) {
						// Odd entry
						uri = "drawable/scores_stub_odd";
					}
				    int imageResource = getResources().getIdentifier(uri, null, getActivity().getPackageName());
				    Drawable image = getResources().getDrawable(imageResource);
				    backgroundImageView.setImageDrawable(image);
					
				    // Other attributes of backgroundImageView to modify
				    FrameLayout.LayoutParams backgroundImageViewLayoutParams = new FrameLayout.LayoutParams(
				    		FrameLayout.LayoutParams.WRAP_CONTENT,
				    		FrameLayout.LayoutParams.WRAP_CONTENT);
				    int backgroundImageViewMarginTop = getResources().getDimensionPixelSize(R.dimen.scoreboard_background_imageview_margin_top);
				    int backgroundImageViewMarginSide = getResources().getDimensionPixelSize(R.dimen.scoreboard_background_imageview_margin_side);
				    
					if (index % 2 != 0) {
						// Odd entry
					    backgroundImageViewLayoutParams.setMargins(backgroundImageViewMarginSide, backgroundImageViewMarginTop, 0, 0);
						backgroundImageViewLayoutParams.gravity = Gravity.LEFT;
					} else {
						// Even entry
					    backgroundImageViewLayoutParams.setMargins(0, backgroundImageViewMarginTop, backgroundImageViewMarginSide, 0);						
					    backgroundImageViewLayoutParams.gravity = Gravity.RIGHT;						
					}
					backgroundImageView.setLayoutParams(backgroundImageViewLayoutParams);
				}
				
			    // ProfilePictureView of the current user ...
				{
				    // Create and add a ProfilePictureView for the current user entry's profile picture
				    ProfilePictureView profilePictureView = new ProfilePictureView(getActivity());
				    frameLayout.addView(profilePictureView);
				    
				    // Set the attributes of the profilePictureView
				    int profilePictureViewWidth = getResources().getDimensionPixelSize(R.dimen.scoreboard_profile_picture_view_width);
				    FrameLayout.LayoutParams profilePictureViewLayoutParams = new FrameLayout.LayoutParams(profilePictureViewWidth, profilePictureViewWidth);
				    int profilePictureViewMarginLeft = 0;
				    int profilePictureViewMarginTop = getResources().getDimensionPixelSize(R.dimen.scoreboard_profile_picture_view_margin_top);
				    int profilePictureViewMarginRight = 0;
				    int profilePictureViewMarginBottom = 0;
				    if (index % 2 == 0) {
				    	profilePictureViewMarginLeft = getResources().getDimensionPixelSize(R.dimen.scoreboard_profile_picture_view_margin_left);
					} else {
						profilePictureViewMarginRight = getResources().getDimensionPixelSize(R.dimen.scoreboard_profile_picture_view_margin_right);
					}
				    profilePictureViewLayoutParams.setMargins(profilePictureViewMarginLeft, profilePictureViewMarginTop,
				    		profilePictureViewMarginRight, profilePictureViewMarginBottom);
				    profilePictureViewLayoutParams.gravity = Gravity.LEFT;
					if (index % 2 != 0) {
						// Odd entry
						profilePictureViewLayoutParams.gravity = Gravity.RIGHT;
					}
					profilePictureView.setLayoutParams(profilePictureViewLayoutParams);
				    
				    // Finally set the id of the user to show their profile pic
				    profilePictureView.setProfileId(currentScoreboardEntry.getId());
				}
				
				// LinearLayout to hold the text in this entry
				
				// Create and add a LinearLayout to hold the TextViews
				LinearLayout textViewsLinearLayout = new LinearLayout(getActivity());
				frameLayout.addView(textViewsLinearLayout);
				
				// Set the attributes for this textViewsLinearLayout
				FrameLayout.LayoutParams textViewsLinearLayoutLayoutParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT,
						FrameLayout.LayoutParams.WRAP_CONTENT);
				int textViewsLinearLayoutMarginLeft = 0;
			    int textViewsLinearLayoutMarginTop = getResources().getDimensionPixelSize(R.dimen.scoreboard_textviews_linearlayout_margin_top);
			    int textViewsLinearLayoutMarginRight = 0;
			    int textViewsLinearLayoutMarginBottom = 0;
			    if (index % 2 == 0) {
			    	textViewsLinearLayoutMarginLeft = getResources().getDimensionPixelSize(R.dimen.scoreboard_textviews_linearlayout_margin_left);
				} else {
					textViewsLinearLayoutMarginRight = getResources().getDimensionPixelSize(R.dimen.scoreboard_textviews_linearlayout_margin_right);
				}
			    textViewsLinearLayoutLayoutParams.setMargins(textViewsLinearLayoutMarginLeft, textViewsLinearLayoutMarginTop,
			    		textViewsLinearLayoutMarginRight, textViewsLinearLayoutMarginBottom);
			    textViewsLinearLayoutLayoutParams.gravity = Gravity.LEFT;
				if (index % 2 != 0) {
					// Odd entry
					textViewsLinearLayoutLayoutParams.gravity = Gravity.RIGHT;
				}
				textViewsLinearLayout.setLayoutParams(textViewsLinearLayoutLayoutParams);
				textViewsLinearLayout.setOrientation(LinearLayout.VERTICAL);
				
				// TextView with the position and name of the current user
				{
					// Set the text that should go in this TextView first
					int position = index+1;
					String currentScoreboardEntryTitle = position + ". " + currentScoreboardEntry.getName();
					
					// Create and add a TextView for the current user position and first name
				    TextView titleTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(titleTextView);
				    
				    // Set the text and other attributes for this TextView
				    titleTextView.setText(currentScoreboardEntryTitle);
				    titleTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerNameFont);
				}
				
				// TextView with the score of the current user
				{
					// Create and add a TextView for the current user score
				    TextView scoreTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(scoreTextView);
				    
				    // Set the text and other attributes for this TextView
				    scoreTextView.setText("Score: " + currentScoreboardEntry.getScore());
				    scoreTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerScoreFont);
				}
				
				// Finally make this frameLayout clickable so that a game starts with the user smashing
				// the user represented by this frameLayout in the scoreContainer
				frameLayout.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (event.getAction() == MotionEvent.ACTION_UP) {
							Bundle bundle = new Bundle();
							bundle.putString("user_id", currentScoreboardEntry.getId());

							Intent i = new Intent();
							i.putExtras(bundle);
						
							getActivity().setResult(Activity.RESULT_FIRST_USER , i);
							getActivity().finish();
							return false;
						} else {
							return true;
						}
					}
					
				});
			    
			    // Increment the index before looping back
				index++;
			}
		}
	}
}
