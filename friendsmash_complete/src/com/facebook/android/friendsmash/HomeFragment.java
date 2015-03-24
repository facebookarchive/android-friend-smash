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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestBatch;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.ProfilePictureView;
import com.facebook.widget.WebDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Fragment to be shown once the user is logged in on the social version of the game or
 *  the start screen for the non-social version of the game
 */
public class HomeFragment extends Fragment {
	
    interface FriendsLoadedCallback {
        void afterFriendsLoaded();
    }

	// Tag used when logging errors
    private static final String TAG = HomeFragment.class.getSimpleName();
	
	// Store the Application (as you can't always get to it when you can't access the Activity - e.g. during rotations)
    private FriendSmashApplication application;
    
	// LinearLayout of the mainButtonsContainer
    private LinearLayout mainButtonsContainer;
    
	// LinearLayout of the gameOverContainer
    private RelativeLayout challengeContainer;

	// LinearLayout of the gameOverContainer
    private LinearLayout gameOverContainer;
    
 // FrameLayout of the progressContainer
    private FrameLayout progressContainer;
	    
	// TextView for the You Scored message
    private TextView scoredTextView;    
	
	// userImage ProfilePictureView to display the user's profile pic
    private ProfilePictureView userImage;
    
    // profile pic of the user you smashed
    private ProfilePictureView youSmashedUserImage;
	
	// TextView for the user's name
    private TextView welcomeTextView;
	
    private GridView invitesGridView;
    private GridView requestsGridView;
    
	// Buttons ...
    private ImageView playButton;
    private ImageView scoresButton;
    private ImageView challengeButton;
    private ImageView challengeRequestToggle;
    
    private TextView numBombs;
    private TextView numCoins;
	
	// Parameters of a WebDialog that should be displayed
    private WebDialog dialog = null;
    private String dialogAction = null;
    private Bundle dialogParams = null;
    
	// Boolean indicating whether or not the game over message is displaying
    private boolean gameOverMessageDisplaying = false;
		
	// Boolean indicating if the game has been launched directly from deep linking already
	// so that it isn't launched again when the view is created (e.g. on rotation)
	private boolean gameLaunchedFromDeepLinking = false;
	
	// Attributes for posting back to Facebook
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final int AUTH_FRIENDS_PLAY_ACTIVITY_CODE = 101;
	private static final int AUTH_FRIENDS_LEADERBOARD_ACTIVITY_CODE = 102;
	private static final int AUTH_PUBLISH_ACTIONS_SCORES_ACTIVITY_CODE = 103;
	private static final String PENDING_POST_KEY = "pendingPost";
	private boolean pendingPost = false;

	private boolean invitesMode = true;
	private List<String> idsToInvite = new ArrayList<String>();
	private List<String> idsToRequest = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
				
		application = (FriendSmashApplication) getActivity().getApplication();
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("gameOverMessageDisplaying"))
				gameOverMessageDisplaying = savedInstanceState.getBoolean("gameOverMessageDisplaying");
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v;
		
		if (!FriendSmashApplication.IS_SOCIAL) {
			v = inflater.inflate(R.layout.fragment_home, parent, false);
			
		} else {
			v = inflater.inflate(R.layout.fragment_home_fb_logged_in, parent, false);
			
			// Set the userImage ProfilePictureView
			userImage = (ProfilePictureView) v.findViewById(R.id.userImage);
			
			// Set the welcomeTextView TextView
			welcomeTextView = (TextView)v.findViewById(R.id.welcomeTextView);
			
			// Personalize this HomeFragment
			personalizeHomeFragment();
						
			scoresButton = (ImageView)v.findViewById(R.id.scoresButton);
			scoresButton.setOnTouchListener(new View.OnTouchListener() {
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onScoresButtonTouched();
					return false;
				}
	              });
			
			challengeButton = (ImageView)v.findViewById(R.id.challengeButton);
			challengeButton.setOnTouchListener(new View.OnTouchListener() {
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onChallengeButtonTouched();
					return false;
				}
	              });
			
			ImageView gameOverChallengeButton = (ImageView)v.findViewById(R.id.gameOverChallengeButton);
			gameOverChallengeButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onGameOverChallengeButtonTouched();				
					return false;
				}
			});

			ImageView gameOverBragButton = (ImageView)v.findViewById(R.id.gameOverBragButton);
			gameOverBragButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onGameOverBragButtonTouched();				
					return false;
				}
			});

			challengeRequestToggle = (ImageView)v.findViewById(R.id.mfsClicker);
			challengeRequestToggle.setOnTouchListener(new View.OnTouchListener() {
				
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	if (invitesMode) {
	            		invitesMode = false;
	            		challengeRequestToggle.setImageResource(R.drawable.mfs_clicker_request);
	        			invitesGridView.setVisibility(View.INVISIBLE);
	        			requestsGridView.setVisibility(View.VISIBLE);							            		
	            	} else {
	            		invitesMode = true;
	            		challengeRequestToggle.setImageResource(R.drawable.mfs_clicker_invite);	            		
	        			invitesGridView.setVisibility(View.VISIBLE);
	        			requestsGridView.setVisibility(View.INVISIBLE);						
	            	}
					return false;
				}
	              });
			
			invitesGridView = (GridView)v.findViewById(R.id.invitesGridView);
			requestsGridView = (GridView)v.findViewById(R.id.requestsGridView);
			
			requestsGridView.setVisibility(View.INVISIBLE);			
			
			ImageView sendButton = (ImageView)v.findViewById(R.id.sendButton);
			sendButton.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (invitesMode) {
						sendDirectedInvite(idsToInvite);
					} else {
						sendDirectedRequest(idsToRequest);						
					}				
					
					// hide the challenge view and show the main menu
					challengeContainer.setVisibility(View.INVISIBLE);
					mainButtonsContainer.setVisibility(View.VISIBLE);
					return false;
				}
			});

			mainButtonsContainer = (LinearLayout)v.findViewById(R.id.mainButtonsContainer);
			challengeContainer = (RelativeLayout)v.findViewById(R.id.challengeContainer);

			// Hide the challengeContainer
			challengeContainer.setVisibility(View.INVISIBLE);
		}
		
		numBombs = (TextView)v.findViewById(R.id.numBombs);
		numCoins = (TextView)v.findViewById(R.id.numCoins);
		loadInventory();
		
		ImageView bombButton = (ImageView)v.findViewById(R.id.bombButton);
		bombButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				HomeActivity homeActivity = (HomeActivity) getActivity();
				homeActivity.buyBombs();
				return false;
			}
		});
		
		
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);
				
		playButton = (ImageView)v.findViewById(R.id.playButton);
		playButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
			public boolean onTouch(View v, MotionEvent event) {
            	onPlayButtonTouched();
				return false;
			}
             });

		gameOverContainer = (LinearLayout)v.findViewById(R.id.gameOverContainer);
		youSmashedUserImage = (ProfilePictureView)v.findViewById(R.id.youSmashedUserImage);
		scoredTextView = (TextView)v.findViewById(R.id.scoredTextView);

		ImageView gameOverCloseButton = (ImageView)v.findViewById(R.id.gameOverCloseButton);
		gameOverCloseButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
            	onGameOverCloseButtonTouched();				
				return false;
			}
		});
		
		
		// Hide the gameOverContainer
		hideGameOverContainer();
		
		// Hide the progressContainer
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Restore the state
		restoreState(savedInstanceState);
		
		return v;
	}
	
	// Personalize this HomeFragment (social-version only)
	void personalizeHomeFragment() {
		if (application.getCurrentFBUser() != null) {
			// Personalize this HomeFragment if the currentFBUser has been fetched
			
			// Set the id for the userImage ProfilePictureView
            // that in turn displays the profile picture
            userImage.setProfileId(application.getCurrentFBUser().getId());
            // and show the cropped (square) version ...
            userImage.setCropped(true);
            
            // Set the welcomeTextView Textview's text to the user's name
            welcomeTextView.setText("Welcome, " + application.getCurrentFBUser().getFirstName());
		}
	}
	
	public void loadInventory() {
		FriendSmashApplication app = (FriendSmashApplication)getActivity().getApplication();
		numBombs.setText(String.valueOf(app.getBombs()));
		numCoins.setText(String.valueOf(app.getCoins()));		
	}
	
	// Restores the state during onCreateView
	private void restoreState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			pendingPost = savedInstanceState.getBoolean(PENDING_POST_KEY, false);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// Hide the gameOverContainer
		//gameOverContainer.setVisibility(View.INVISIBLE);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (application.getCurrentFBUser() != null && !gameLaunchedFromDeepLinking) {
			// As long as the user is logged in and the game hasn't been launched yet
			// from deep linking, see if it has been deep linked and launch the game appropriately
			Uri target = getActivity().getIntent().getData();
			if (target != null) {
				Intent i = new Intent(getActivity(), GameActivity.class);
				
			    // Target is the deep-link Uri, so skip loading this home screen and load the game
				// directly with the sending user's picture to smash
				String graphRequestIDsForSendingUser = target.getQueryParameter("request_ids");
				String feedPostIDForSendingUser = target.getQueryParameter("challenge_brag");
				
				if (graphRequestIDsForSendingUser != null) {
					// Deep linked through a Request and use the latest request (request_id) if multiple requests have been sent
					String [] graphRequestIDsForSendingUsers = graphRequestIDsForSendingUser.split(",");
					String graphRequestIDForSendingUser = graphRequestIDsForSendingUsers[graphRequestIDsForSendingUsers.length-1];
					Bundle bundle = new Bundle();
					bundle.putString("request_id", graphRequestIDForSendingUser);
					i.putExtras(bundle);
					gameLaunchedFromDeepLinking = true;
					startActivityForResult(i, 0);
					
					// Delete the Request now it has been consumed and processed
					Request deleteFBRequestRequest = new Request(Session.getActiveSession(),
							graphRequestIDForSendingUser + "_" + application.getCurrentFBUser().getId(),
							new Bundle(),
		                    HttpMethod.DELETE,
		                    new Request.Callback() {

								@Override
								public void onCompleted(Response response) {
									FacebookRequestError error = response.getError();
									if (error != null) {
										Log.e(FriendSmashApplication.TAG, "Deleting consumed Request failed: " + error.getErrorMessage());
									} else {
										Log.i(FriendSmashApplication.TAG, "Consumed Request deleted");
									}
								}
							});
					Request.executeBatchAsync(deleteFBRequestRequest);
				} else if (feedPostIDForSendingUser != null) {
					// Deep linked through a feed post, so start the game smashing the user specified by the id attached to the
					// challenge_brag parameter
					Bundle bundle = new Bundle();
					bundle.putString("user_id", feedPostIDForSendingUser);
					i.putExtras(bundle);
					gameLaunchedFromDeepLinking = true;
					startActivityForResult(i, 0);
				}
			} else {
			    // Launched with no deep-link Uri, so just continue as normal and load the home screen
			}

		}

		if (!gameLaunchedFromDeepLinking && gameOverMessageDisplaying) {
			// The game hasn't just been launched from deep linking and the game over message should still be displaying, so ...

			// Complete the game over logic
			completeGameOver();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// If a dialog exists, create a new dialog (as the screen may have rotated so needs
		// new dimensions) and show it
		if (dialog != null) {
			showDialogWithoutNotificationBar(dialogAction, dialogParams);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		
		// If a dialog exists and is showing, dismiss it
		if (dialog != null) {
			dialog.dismiss();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(PENDING_POST_KEY, pendingPost);
		outState.putBoolean("gameOverMessageDisplaying", gameOverMessageDisplaying);
	}

	// Show a dialog prompting the user with an explanation of why we're asking for the 
	// user_friends permission before we play.
	private void askForFriendsForPlay(final Session session) {
		// user has already said no once this session.
		if (application.hasDeniedFriendPermission()) {
			startGame();
		} else {		
			new AlertDialog.Builder(getActivity())
			.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// User hit OK. Request Facebook friends permission.
					requestFriendsPermission(AUTH_FRIENDS_PLAY_ACTIVITY_CODE);
				}
			})
			.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User hit cancel. Keep track of deny so that we only ask once per session
					// and then just play the game.            	
					application.setHasDeniedFriendPermission(true);
					startGame();
				}
			})
			.setTitle(R.string.with_friends_dialog_title)
			.setMessage(R.string.with_friends_dialog_message)
			.show();
		}
	}

	// Show a dialog prompting the user with an explanation of why we're asking for the 
	// user_friends permission before we show the leaderboard.
	private void askForFriendsForLeaderboard() {		
		new AlertDialog.Builder(getActivity())
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// User hit OK. Request Facebook friends permission.
				requestFriendsPermission(AUTH_FRIENDS_LEADERBOARD_ACTIVITY_CODE);				
			}
		})
		.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User hit cancel.
				// do nothing.
			}
		})
		.setTitle(R.string.leaderboard_dialog_title)
		.setMessage(R.string.leaderboard_dialog_message)
		.show();
	}
	
	
	// Show a dialog prompting the user with an explanation of why we're asking for the 
	// publish_actions permission in order to save their scores.	
	private void askForPublishActionsForScores() {
		new AlertDialog.Builder(getActivity())
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// User hit OK. Request Facebook friends permission.
				requestPublishPermissions();				
			}
		})
		.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User hit cancel.
				// Hide the gameOverContainer
				hideGameOverContainer();
			}
		})
		.setTitle(R.string.publish_scores_dialog_title)
		.setMessage(R.string.publish_scores_dialog_message)
		.show();		
	}

	// Called when the Play button is touched
	private void onPlayButtonTouched() {
		if (application.IS_SOCIAL == true) {

			Session session = Session.getActiveSession();			
			if (session == null || !session.isOpened()) {
				return;
			}

			// check to see that the user granted the user_friends permission. 
			List<String> permissions = session.getPermissions();			 
			if (!permissions.contains("user_friends")) {
				// the user didn't grant this permission, so we need to prompt them.
				askForFriendsForPlay(session);
				return;
			}

			if (application.getFriends() != null && application.getFriends().size() <= 0) {
				((HomeActivity)getActivity()).showError("You don't have any friends to smash!", false);
			} else {
				startGame();
			}
		} else {
			startGame();
		}
	}
	
	private void startGame() {
        Intent i = new Intent(getActivity(), GameActivity.class);
        Bundle bundle = new Bundle();
		bundle.putInt("num_bombs", ((FriendSmashApplication) getActivity().getApplication()).getBombs());
		i.putExtras(bundle);
        startActivityForResult(i, 0);
	}
	
	// Called when the Challenge button is touched
	private void onChallengeButtonTouched() {
		sendCustomChallenge();
	}	

	// Send a request to a specific player(s)
	private void sendDirectedInvite(List<String> invitableTokens) {
		Bundle params = new Bundle();
    	params.putString("message", "Come join me in the friend smash times!");    	
    	params.putString("to", TextUtils.join(",", invitableTokens));
    	showDialogWithoutNotificationBar("apprequests", params);
	}
	
	// Send a request to a specific player(s)
	private void sendDirectedRequest(List<String> fbUIDs) {
		Bundle params = new Bundle();
    	params.putString("message", "I just scored " + application.getScore() + "! Can you beat it?");    	
    	params.putString("to", TextUtils.join(",", fbUIDs));
    	showDialogWithoutNotificationBar("apprequests", params);
	}
	
	// Called when the Scores button is touched
	private void onScoresButtonTouched() {
		Session session = Session.getActiveSession();	
		if (session == null || !session.isOpened()) {
            return;
        }
		List<String> permissions = session.getPermissions();		

		// check to see that the user granted the user_friends permission. 
		if (!permissions.contains("user_friends")) {
			// the user didn't grant this permission, so we need to prompt them.
			askForFriendsForLeaderboard();
			return;
		} else {
			Intent i = new Intent(getActivity(), ScoreboardActivity.class);
			startActivityForResult(i, 0);
		}
	}
	
	private void onGameOverChallengeButtonTouched() {		
		sendDirectedRequest(Arrays.asList(application.getLastFriendSmashedID())); 
	}
	
	private void onGameOverBragButtonTouched() {
		sendBrag();
	}
	
	private void onGameOverCloseButtonTouched() {
		// check if the user wants to post their score to facebook
		// which requires the publish_actions permissions

		if (!FriendSmashApplication.IS_SOCIAL) {
			hideGameOverContainer();
			return;
		}
		
		Session session = Session.getActiveSession();	
		if (session == null || !session.isOpened()) {
            return;
        }
		List<String> permissions = session.getPermissions();		

		// check to see that the user granted the publish_actions permission. 
		if (!permissions.contains("publish_actions")) {
			// the user didn't grant this permission, so we need to prompt them.
			askForPublishActionsForScores();
			return;
		} else {
			// Save score and hide the gameOverContainer
			postScore();
			hideGameOverContainer();
		}		
	}

    private void showGameOverContainer() {
        gameOverContainer.setVisibility(View.VISIBLE);
        gameOverMessageDisplaying = true;
    }

    private void hideGameOverContainer() {
        gameOverContainer.setVisibility(View.INVISIBLE);
        gameOverMessageDisplaying = false;
    }
	
	private void loadInvitableFriendsForInvites() {
		
		final List<JSONObject> invitableFriends;
		if (application.getInvitableFriends().size() > 8) {
			// Truncating list to first 8 to simplify our UI.
			invitableFriends = application.getInvitableFriends().subList(0, 8);
		} else {
			invitableFriends = application.getInvitableFriends();

		}
						
        final InviteUserArrayAdapter adapter = new InviteUserArrayAdapter(getActivity(), 
        		invitableFriends);
        invitesGridView.setAdapter(adapter);

        invitesGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

          @Override
          public void onItemClick(AdapterView<?> parent, final View view,
              int position, long id) {
        	          	  
        	  JSONObject clickedUser = invitableFriends.get(position);
        	  String invitableToken = clickedUser.optString("id");
        	  
        	  // items act as toggles. so check to see if item exists. if it does
        	  // then remove. otherwise, add it.
        	  if (idsToInvite.contains(invitableToken)) {
        		  idsToInvite.remove(invitableToken);
        	  } else {
        		  idsToInvite.add(invitableToken);
        	  }
          }
        });
	}	
	
	private void loadFriendsForRequests() {
		
		// assumes friends have been loaded
		List<GraphUser> friends = application.getFriends();
		
		// arbitrarily truncating the list of friends at 8 to simplify this a bit.
		if (friends.size() > 8 )
			friends = friends.subList(0, 8);
			
        final RequestUserArrayAdapter adapter = new RequestUserArrayAdapter(getActivity(), 
        		friends);
        requestsGridView.setAdapter(adapter);

        requestsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

          @Override
          public void onItemClick(AdapterView<?> parent, final View view,
              int position, long id) {
        	  
        	  GraphUser clickedUser = application.getFriends().get(position);
        	  String uid = clickedUser.getId();
        	  
        	  // items act as toggles. so check to see if item exists. if it does
        	  // then remove. otherwise, add it.
        	  if (idsToRequest.contains(uid)) {
        		  idsToRequest.remove(uid);
        	  } else {
        		  idsToRequest.add(uid);
        	  }
          }

        });

	}

	/*
	 * Now that user_friends is granted, load /me/invitable_friends to get 
	 * friends who have not installed the game. Also load /me/friends which
	 * returns friends that have installed the game (if using Platform v2.0).
	 *     
	 */
	private void loadFriendsFromFacebook(final FriendsLoadedCallback callback) {
		final Session session = Session.getActiveSession();
		
		RequestBatch requestBatch = new RequestBatch();
		
		// Get a list of friends who have _not installed_ the game. 
		Request invitableFriendsRequest = Request.newGraphPathRequest(session, 
				"/me/invitable_friends", new Request.Callback() {

			@Override
			public void onCompleted(Response response) {

				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					//handleError(error, true);
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Get the result
						GraphObject graphObject = response.getGraphObject();
						JSONArray dataArray = (JSONArray)graphObject.getProperty("data");

						List<JSONObject> invitableFriends = new ArrayList<JSONObject>();
						if (dataArray.length() > 0) {
							// Ensure the user has at least one friend ...

							for (int i=0; i<dataArray.length(); i++) {
								invitableFriends.add(dataArray.optJSONObject(i));
							}
						}

						application.setInvitableFriends(invitableFriends);							
					}
				}
			}

		});
		Bundle invitableParams = new Bundle();
		invitableParams.putString("fields", "id,first_name,picture");
		invitableFriendsRequest.setParameters(invitableParams);
		requestBatch.add(invitableFriendsRequest);
					
		// Get the user's list of friends. 
		// This only returns friends who have installed the game.
		Request friendsRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() {

			@Override
			public void onCompleted(List<GraphUser> users, Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					//handleError(error, true);
				} else if (session == Session.getActiveSession()) {
					// Set the friends attribute
					application.setFriends(users);
					callback.afterFriendsLoaded();
				}
			}
		});
		
		Bundle params = new Bundle();
        params.putString("fields", "id,first_name");
        friendsRequest.setParameters(params);
		requestBatch.add(friendsRequest);

		// Execute the batch of requests asynchronously
		requestBatch.executeAsync();
	}
	
	/*
	 * Called when and Activity returns. Checks for the following scenarios:
	 * 
	 * == Returning from a Facebook dialog asking for the user_friends permission after the user hit
	 *    the Play button.
	 *    
	 * == Returning from a Facebook dialog asking for the user_friends permission after the user hit 
	 *    the Leaderbaord button.
	 *    
	 * == Returning from a Facebook dialog asking for the publish_actions permission after the user hit 
	 *    the close button on the Game Over dialog.
	 *    
	 * == Returns from a finished game - test status with resultCode and if successfully ended, update
	 *    their score and complete the game over process, otherwise show an error if there is one
	 *    
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == AUTH_FRIENDS_PLAY_ACTIVITY_CODE) {
			Session session = Session.getActiveSession();
			if (session == null || !session.isOpened()) {
	            return;
	        }
			session.onActivityResult(getActivity(), requestCode, resultCode, data);
						
    		if (session.getPermissions().contains("user_friends")) {
    			loadFriendsFromFacebook(new FriendsLoadedCallback() {

					@Override
					public void afterFriendsLoaded() {
						startGame();
					}
    				
    			});
    		} else {
    			application.setHasDeniedFriendPermission(true);
    			startGame();    			
    		}
		} else if (requestCode == AUTH_FRIENDS_LEADERBOARD_ACTIVITY_CODE) {
			Session session = Session.getActiveSession();
			if (session == null || !session.isOpened()) {
	            return;
	        }
			session.onActivityResult(getActivity(), requestCode, resultCode, data);

			if (session.getPermissions().contains("user_friends")) {
    			loadFriendsFromFacebook(new FriendsLoadedCallback() {

					@Override
					public void afterFriendsLoaded() {
		    			Intent i = new Intent(getActivity(), ScoreboardActivity.class);
		    			startActivityForResult(i, 0);
					}

    			});    			    			
    		} else {
    			// do nothing
    		}
			
		} else if (requestCode == AUTH_PUBLISH_ACTIONS_SCORES_ACTIVITY_CODE) { 
			Session session = Session.getActiveSession();
			if (session == null || !session.isOpened()) {
	            return;
	        }
			session.onActivityResult(getActivity(), requestCode, resultCode, data);

    		if (session.getPermissions().contains("publish_actions")) {
    			postScore();
    		} 	

    		// Hide the gameOverContainer
			hideGameOverContainer();

        } else if (resultCode == Activity.RESULT_OK && data != null) {
        	// Finished a game
        	// Get the parameters passed through including the score
			Bundle bundle = data.getExtras();
			application.setScore(bundle.getInt("score"));
			
			// Save coins and bombs data to parse
			int coinsCollected = (bundle.getInt("coins_collected"));
			application.setCoinsCollected(coinsCollected);
			if (coinsCollected > 0) {
				application.setCoins(application.getCoins()+coinsCollected);
			}
			int bombsUsed = (bundle.getInt("bombs_used"));
			if (bombsUsed > 0) {
				application.setBombs(application.getBombs()-bombsUsed);
			}
			
			// Save inventory values
			application.saveInventory();
	        
	        // Reload inventory values
	        loadInventory();
			
			// Update the UI
			completeGameOver();

            // log GAME_PLAYED event
            ((HomeActivity)getActivity()).getEventsLogger().logGamePlayedEvent(application.getScore());

		} else if (resultCode == Activity.RESULT_FIRST_USER && data != null) {
			// Came from the ScoreboardFragment, so start a game with the specific user who has been clicked
			Intent i = new Intent(getActivity(), GameActivity.class);
			Bundle bundle = new Bundle();
			bundle.putString("user_id", data.getStringExtra("user_id"));
			i.putExtras(bundle);
			startActivityForResult(i, 0);
		} else if (resultCode == Activity.RESULT_CANCELED && data != null) {
			Bundle bundle = data.getExtras();
			((HomeActivity)getActivity()).showError(bundle.getString("error"), false);
		} else if (resultCode == Activity.RESULT_CANCELED &&
				((FriendSmashApplication) getActivity().getApplication()).getGameFragmentFBRequestError() != null) {
			((HomeActivity)getActivity()).handleError(
					((FriendSmashApplication) getActivity().getApplication()).getGameFragmentFBRequestError(),
					false);
			((FriendSmashApplication) getActivity().getApplication()).setGameFragmentFBRequestError(null);
		}
	}
	
	// Start a Game with a specified user id (called from the ScoreboardFragment)
	void startGame(String userId) {
		Intent i = new Intent(getActivity(), GameActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("user_id", userId);
		bundle.putInt("num_bombs", ((FriendSmashApplication) getActivity().getApplication()).getBombs());
		i.putExtras(bundle);
		startActivityForResult(i, 0);
	}
	
	// Complete the game over process
	private void completeGameOver() {
		// Set the scoreboardEntriesList to null so that the scoreboard is refreshed
		// now that the player has played another game in case they have a higher score or
		// any of their friends have a higher score
		application.setScoreboardEntriesList(null);

		if (FriendSmashApplication.IS_SOCIAL) {
			youSmashedUserImage.setProfileId(application.getLastFriendSmashedID());
			youSmashedUserImage.setCropped(true);
		} else {
			youSmashedUserImage.setVisibility(View.INVISIBLE);
		}
		
		if (application.getScore() >= 0) {
			scoredTextView.setText("You smashed " + application.getLastFriendSmashedName() +
					" " + application.getScore() + (application.getScore() == 1 ? " time!" : " times!") +
					"\n" + "Collected " + application.getCoinsCollected() +
					(application.getCoinsCollected() == 1 ? " coin!" : " coins!"));
		}
		else {
			scoredTextView.setText(getResources().getString(R.string.no_score));
		}
				
		// Show the gameOverContainer
		showGameOverContainer();
				
	}
	
	
	/* Facebook Integration */
	
	// Pop up a request dialog for the user to invite their friends to smash them back in Friend Smash
	private void sendChallenge() {
    	Bundle params = new Bundle();
    	
    	// Uncomment following link once uploaded on Google Play for deep linking
    	// params.putString("link", "https://play.google.com/store/apps/details?id=com.facebook.android.friendsmash");
    	
    	// 1. No additional parameters provided - enables generic Multi-friend selector
    	params.putString("message", "I just smashed " + application.getScore() + " friends! Can you beat it?");
    	
    	// 2. Optionally provide a 'to' param to direct the request at a specific user
//    	params.putString("to", "515768651");
    	
    	// 3. Suggest friends the user may want to request - could be game specific
    	// e.g. players you are in a match with, or players who recently played the game etc.
    	// Normally this won't be hardcoded as follows but will be context specific
//    	String [] suggestedFriends = {
//		    "695755709",
//		    "685145706",
//		    "569496010",
//		    "286400088",
//		    "627802916",
//    	};
//    	params.putString("suggestions", TextUtils.join(",", suggestedFriends));
//    	
    	// Show FBDialog without a notification bar
    	showDialogWithoutNotificationBar("apprequests", params);
	}
		
	// Pop up a filtered request dialog for the user to invite their friends that have Android devices
	// to smash them back in Friend Smash
	private void sendFilteredChallenge() {
		// Okay, we're going to filter our friends by their device, we're looking for friends with an Android device
		
		// Show the progressContainer during the network call
		progressContainer.setVisibility(View.VISIBLE);
		
		// Get a list of the user's friends' names and devices
		final Session session = Session.getActiveSession();
		Request friendDevicesGraphPathRequest = Request.newGraphPathRequest(session, "me/friends", new Request.Callback() {
			@Override
			public void onCompleted(Response response) {
				// Hide the progressContainer now that the network call has completed
				progressContainer.setVisibility(View.INVISIBLE);
				
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					((HomeActivity)getActivity()).handleError(error, false);
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Get the result
						GraphObject graphObject = response.getGraphObject();
						JSONArray dataArray = (JSONArray)graphObject.getProperty("data");
						
						if (dataArray.length() > 0) {
							// Ensure the user has at least one friend ...
							
							// Store the filtered friend ids in the following List
							ArrayList<String> filteredFriendIDs = new ArrayList<String>();
							
							for (int i=0; i<dataArray.length(); i++) {
								JSONObject currentUser = dataArray.optJSONObject(i);
								if (currentUser != null) {
									JSONArray currentUserDevices = currentUser.optJSONArray("devices");
									if (currentUserDevices != null) {
										// The user has at least one (mobile) device logged into Facebook
										for (int j=0; j<currentUserDevices.length(); j++) {
											JSONObject currentUserDevice = currentUserDevices.optJSONObject(j);
											if (currentUserDevice != null) {
												String currentUserDeviceOS = currentUserDevice.optString("os");
												if (currentUserDeviceOS != null) {
													if (currentUserDeviceOS.equals("Android")) {
														filteredFriendIDs.add(currentUser.optString("id"));
													}
												}
											}
										}
									}
								}
							}
							
							// Now we have a list of friends with an Android device, we can send requests to them
					    	Bundle params = new Bundle();
					    	
					    	// Uncomment following link once uploaded on Google Play for deep linking
					    	// params.putString("link", "https://play.google.com/store/apps/details?id=com.facebook.android.friendsmash");
					    	
					    	// We create our parameter dictionary as we did before
					    	params.putString("message", "I just smashed " + application.getScore() + " friends! Can you beat it?");
					    	
					    	// We have the same list of suggested friends
					    	String [] suggestedFriends = {
					    		    "695755709",
					    		    "685145706",
					    		    "569496010",
					    		    "286400088",
					    		    "627802916",
					    	};
							    	
					    	// Of course, not all of our suggested friends will have Android devices - we need to filter them down
					    	ArrayList<String> validSuggestedFriends = new ArrayList<String>();
		             
		                    // So, we loop through each suggested friend
		                    for (String suggestedFriend : suggestedFriends)
		                    {
		                        // If they are on our device filtered list, we know they have an Android device
		                        if (filteredFriendIDs.contains(suggestedFriend))
		                        {
		                            // So we can call them valid
		                        	validSuggestedFriends.add(suggestedFriend);
		                        }
		                    }
		                    params.putString("suggestions", TextUtils.join(",", validSuggestedFriends.toArray(new String[validSuggestedFriends.size()])));
							    	
					    	// Show FBDialog without a notification bar
					    	showDialogWithoutNotificationBar("apprequests", params);
						}
					}
				}
			}
		});
		// Pass in the fields as extra parameters, then execute the Request
		Bundle extraParamsBundle = new Bundle();
		extraParamsBundle.putString("fields", "name,devices");
		friendDevicesGraphPathRequest.setParameters(extraParamsBundle);
		Request.executeBatchAsync(friendDevicesGraphPathRequest);
	}
	
	private void sendCustomChallenge() {
		Session session = Session.getActiveSession();			
		if (session == null || !session.isOpened()) {
			return;
		}

		// check to see that the user granted the user_friends permission. 
		List<String> permissions = session.getPermissions();			 
		if (!permissions.contains("user_friends")) {
			// if the user hasn't granted user_friends, we'll just fallback 
			// to showing the request dialog without customization.
			sendChallenge();
		} else {
			loadInvitableFriendsForInvites();
			loadFriendsForRequests();		
			
			// Hide the buttons container and show the challenge container
			mainButtonsContainer.setVisibility(View.INVISIBLE);
			challengeContainer.setVisibility(View.VISIBLE);					
		}		
	}
	
	// Pop up a feed dialog for the user to brag to their friends about their score and to offer
	// them the opportunity to smash them back in Friend Smash
	private void sendBrag() {
		// This function will invoke the Feed Dialog to post to a user's Timeline and News Feed
	    // It will attempt to use the Facebook Native Share dialog
	    // If that's not supported we'll fall back to the web based dialog.
		
		GraphUser currentFBUser = application.getCurrentFBUser();
		
		// This first parameter is used for deep linking so that anyone who clicks the link will start smashing this user
    	// who sent the post
		String link = "https://apps.facebook.com/friendsmashsample/?challenge_brag=";
		if (currentFBUser != null) {
			link += currentFBUser.getId();
		}
		
		// Define the other parameters
		String name = "Checkout my Friend Smash greatness!";
		String caption = "Come smash me back!";
		String description = "I just scored " + application.getScore() + "! Can you beat my score?";
	    String picture = "http://www.friendsmash.com/images/logo_large.jpg";
		
	    if (FacebookDialog.canPresentShareDialog(getActivity(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
	    	// Create the Native Share dialog
			FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(getActivity())
			.setLink(link)
			.setName(name)
			.setCaption(caption)
			.setPicture(picture)
			.build();
			
			// Show the Native Share dialog
			((HomeActivity)getActivity()).getFbUiLifecycleHelper().trackPendingDialogCall(shareDialog.present());
		} else {
			// Prepare the web dialog parameters
			Bundle params = new Bundle();
	    	params.putString("link", link);
	    	params.putString("name", caption);
	    	params.putString("caption", caption);
	    	params.putString("description", description);
	    	params.putString("picture", picture);
	    	
	    	// Show FBDialog without a notification bar
	    	showDialogWithoutNotificationBar("feed", params);
		}
	}
	
	// Show a dialog (feed or request) without a notification bar (i.e. full screen)
	private void showDialogWithoutNotificationBar(String action, Bundle params) {
		// Create the dialog
		dialog = new WebDialog.Builder(getActivity(), Session.getActiveSession(), action, params).setOnCompleteListener(
				new WebDialog.OnCompleteListener() {
			
			@Override
			public void onComplete(Bundle values, FacebookException error) {
				if (error != null && !(error instanceof FacebookOperationCanceledException)) {
					((HomeActivity)getActivity()).showError(getResources().getString(R.string.network_error), false);
				}
				dialog = null;
				dialogAction = null;
				dialogParams = null;
			}
		}).build();
		
		// Hide the notification bar and resize to full screen
		Window dialog_window = dialog.getWindow();
    	dialog_window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	// Store the dialog information in attributes
    	dialogAction = action;
    	dialogParams = params;
    	
    	// Show the dialog
    	dialog.show();
	}
	
	// Called when the session state has changed
	void tokenUpdated() {
		// Optional place to take action after the token has been updated (typically after a new permission has 
		// been granted)
	}
	
	void requestPublishPermissions() {
		Log.d(TAG, "Requesting publish permissions.");
		final Session session = Session.getActiveSession();
        if (session != null) {
            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS)
                    // demonstrate how to set an audience for the publish permissions,
                    // if none are set, this defaults to FRIENDS
                    .setDefaultAudience(SessionDefaultAudience.FRIENDS)
                    .setRequestCode(AUTH_PUBLISH_ACTIONS_SCORES_ACTIVITY_CODE);
            session.requestNewPublishPermissions(newPermissionsRequest);
        }
    }
		
	private void requestFriendsPermission(int requestCode) {
		Log.d(TAG, "Requesting friends permissions.");
		Session.NewPermissionsRequest newFriendsPermissionsRequest = new Session.NewPermissionsRequest(this, "user_friends")
			.setRequestCode(requestCode);
    	Session.getActiveSession().requestNewReadPermissions(newFriendsPermissionsRequest);
        
    }
	
	// Post score to Facebook
	private void postScore() {
		final int score = application.getScore();
		if (score > 0) {
			// Only post the score if they smashed at least one friend!
			
			// Post the score to FB (for score stories and distribution)
			Bundle fbParams = new Bundle();
			fbParams.putString("score", "" + score);
			Request postScoreRequest = new Request(Session.getActiveSession(),
					"me/scores",
					fbParams,
                    HttpMethod.POST,
                    new Request.Callback() {

						@Override
						public void onCompleted(Response response) {
							FacebookRequestError error = response.getError();
							if (error != null) {
								Log.e(FriendSmashApplication.TAG, "Posting Score to Facebook failed: " + error.getErrorMessage());
								((HomeActivity)getActivity()).handleError(error, false);
							} else {
								Log.i(FriendSmashApplication.TAG, "Score posted successfully to Facebook");
							}
						}
					});
			Request.executeBatchAsync(postScoreRequest);						
		}
	}

	// Getters & Setters
	
	public boolean isPendingPost() {
		return pendingPost;
	}
	
	public void setPendingPost(boolean pendingPost) {
		this.pendingPost = pendingPost;
	}
	
	

}
