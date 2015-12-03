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
import com.facebook.GraphResponse;
import com.facebook.android.friendsmash.integration.GraphAPICall;
import com.facebook.android.friendsmash.integration.GraphAPICallback;
import com.facebook.login.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class ScoreboardFragment extends Fragment {
	
	private FriendSmashApplication application;
    
	private LinearLayout scoreboardContainer;
	
	private FrameLayout progressContainer;
	
	private Handler uiHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application = (FriendSmashApplication) getActivity().getApplication();

		uiHandler = new Handler();
		
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_scoreboard, parent, false);
		
		scoreboardContainer = (LinearLayout)v.findViewById(R.id.scoreboardContainer);
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);

		progressContainer.setVisibility(View.INVISIBLE);
		
		return v;
	}

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
		if (application.getScoreboardEntriesList() == null) {
			progressContainer.setVisibility(View.VISIBLE);
			fetchScoreboardEntries();
		} else {
			populateScoreboard();
		}
	}

	private void fetchScoreboardEntries () {
        GraphAPICall scoresCall = GraphAPICall.callAppScores(application.getFBAppID(), new GraphAPICallback() {
            @Override
            public void handleResponse(GraphResponse response) {
                JSONArray dataArray = GraphAPICall.getDataFromResponse(response);

                ArrayList<ScoreboardEntry> scoreboardEntriesList = new ArrayList<ScoreboardEntry>();

                for (int i = 0; i < dataArray.length(); i++) {
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

            @Override
            public void handleError(FacebookRequestError error) {
                Log.e(FriendSmashApplication.TAG, error.toString());
            }
        });
        scoresCall.executeAsync();
	}

	private void populateScoreboard() {
		scoreboardContainer.removeAllViews();

		progressContainer.setVisibility(View.INVISIBLE);

		if (application.getScoreboardEntriesList() == null || application.getScoreboardEntriesList().size() <= 0) {
			closeAndShowError(getResources().getString(R.string.error_no_scores));
		} else {
			int index = 0;
			Iterator<ScoreboardEntry> scoreboardEntriesIterator = application.getScoreboardEntriesList().iterator();
			while (scoreboardEntriesIterator.hasNext()) {
				final ScoreboardEntry currentScoreboardEntry = scoreboardEntriesIterator.next();
				FrameLayout frameLayout = new FrameLayout(getActivity());
				scoreboardContainer.addView(frameLayout);
				int topPadding = getResources().getDimensionPixelSize(R.dimen.scoreboard_entry_top_margin);
				frameLayout.setPadding(0, topPadding, 0, 0);
				{
					ImageView backgroundImageView = new ImageView(getActivity());
					frameLayout.addView(backgroundImageView);

					String uri = "drawable/scores_stub_even";
					if (index % 2 != 0) {
						uri = "drawable/scores_stub_odd";
					}

				    int imageResource = getResources().getIdentifier(uri, null, getActivity().getPackageName());
				    Drawable image = getResources().getDrawable(imageResource);
				    backgroundImageView.setImageDrawable(image);

				    FrameLayout.LayoutParams backgroundImageViewLayoutParams = new FrameLayout.LayoutParams(
				    		FrameLayout.LayoutParams.WRAP_CONTENT,
				    		FrameLayout.LayoutParams.WRAP_CONTENT);
				    int backgroundImageViewMarginTop = getResources().getDimensionPixelSize(R.dimen.scoreboard_background_imageview_margin_top);
				    int backgroundImageViewMarginSide = getResources().getDimensionPixelSize(R.dimen.scoreboard_background_imageview_margin_side);
				    
					if (index % 2 != 0) {
					    backgroundImageViewLayoutParams.setMargins(backgroundImageViewMarginSide, backgroundImageViewMarginTop, 0, 0);
						backgroundImageViewLayoutParams.gravity = Gravity.LEFT;
					} else {
					    backgroundImageViewLayoutParams.setMargins(0, backgroundImageViewMarginTop, backgroundImageViewMarginSide, 0);						
					    backgroundImageViewLayoutParams.gravity = Gravity.RIGHT;						
					}
					backgroundImageView.setLayoutParams(backgroundImageViewLayoutParams);
				}

				{
				    ProfilePictureView profilePictureView = new ProfilePictureView(getActivity());
				    frameLayout.addView(profilePictureView);

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
						profilePictureViewLayoutParams.gravity = Gravity.RIGHT;
					}
					profilePictureView.setLayoutParams(profilePictureViewLayoutParams);

				    profilePictureView.setProfileId(currentScoreboardEntry.getId());
				}

				LinearLayout textViewsLinearLayout = new LinearLayout(getActivity());
				frameLayout.addView(textViewsLinearLayout);

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

				{
					int position = index+1;
					String currentScoreboardEntryTitle = position + ". " + currentScoreboardEntry.getName();

				    TextView titleTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(titleTextView);

				    titleTextView.setText(currentScoreboardEntryTitle);
				    titleTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerNameFont);
				}

				{
				    TextView scoreTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(scoreTextView);

				    scoreTextView.setText("Score: " + currentScoreboardEntry.getScore());
				    scoreTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerScoreFont);
				}

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

				index++;
			}
		}
	}
}
