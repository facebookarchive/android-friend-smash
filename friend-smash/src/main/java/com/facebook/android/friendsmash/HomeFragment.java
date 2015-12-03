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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookRequestError;
import com.facebook.GraphResponse;
import com.facebook.android.friendsmash.integration.FacebookLogin;
import com.facebook.android.friendsmash.integration.FacebookLoginPermission;
import com.facebook.android.friendsmash.integration.GameRequest;
import com.facebook.android.friendsmash.integration.GraphAPICall;
import com.facebook.android.friendsmash.integration.GraphAPICallback;
import com.facebook.android.friendsmash.integration.Sharing;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;

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
    private FriendSmashApplication application;

    private LinearLayout mainButtonsContainer;
    private RelativeLayout challengeContainer;
    private LinearLayout gameOverContainer;
    private FrameLayout progressContainer;

    private TextView scoredTextView;
    private ProfilePictureView userImage;
    private ProfilePictureView youSmashedUserImage;
    private TextView welcomeTextView;

	private GridView invitesGridView;
    private GridView requestsGridView;

    private ImageView playButton;
    private ImageView scoresButton;
    private ImageView challengeButton;
    private ImageView challengeRequestToggle;
    
    private TextView numBombs;
    private TextView numCoins;

    private boolean gameOverMessageDisplaying = false;
	private boolean gameLaunchedFromDeepLinking = false;

	private static final int FRIENDS_PLAY_PERMISSION_REQUEST_CODE = 101;
	private static final int FRIENDS_LEADERBOARD_PERMISSION_REQUEST_CODE = 102;
    private static final String GAME_OVER_MESSAGE_KEY = "gameOverMessageDisplaying";

    private int latestPermissionRequestCode;

	private boolean invitesMode = true;
	private List<String> idsToRequest = new ArrayList<String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		application = (FriendSmashApplication) getActivity().getApplication();
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(GAME_OVER_MESSAGE_KEY))
				gameOverMessageDisplaying = savedInstanceState.getBoolean(GAME_OVER_MESSAGE_KEY);
		}
        checkForDeeplinkins();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_home_fb_logged_in, parent, false);
		userImage = (ProfilePictureView) v.findViewById(R.id.userImage);
		welcomeTextView = (TextView)v.findViewById(R.id.welcomeTextView);

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
					sendInvite();
				} else {
					sendDirectedRequest(idsToRequest);
				}

				challengeContainer.setVisibility(View.INVISIBLE);
				mainButtonsContainer.setVisibility(View.VISIBLE);
				return false;
			}
		});

		mainButtonsContainer = (LinearLayout)v.findViewById(R.id.mainButtonsContainer);
		challengeContainer = (RelativeLayout)v.findViewById(R.id.challengeContainer);

		challengeContainer.setVisibility(View.INVISIBLE);

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

		LoginButton logoutButton = (LoginButton)v.findViewById(R.id.loginButton);
		logoutButton.setFragment(this);

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

		hideGameOverContainer();

		progressContainer.setVisibility(View.INVISIBLE);
		
		return v;
	}

	public void personalizeHomeFragment() {
		if (application.getCurrentFBUser() != null) {
            userImage.setProfileId(application.getCurrentFBUser().optString("id"));
            userImage.setCropped(true);
            welcomeTextView.setText("Welcome, " + application.getCurrentFBUser().optString("first_name"));
		}
	}
	
	public void loadInventory() {
		FriendSmashApplication app = (FriendSmashApplication)getActivity().getApplication();
		numBombs.setText(String.valueOf(app.getBombs()));
		numCoins.setText(String.valueOf(app.getCoins()));		
	}
	
	@Override
	public void onResume() {
		super.onResume();
        checkForDeeplinkins();
	}

    private void checkForDeeplinkins() {
        if (!gameLaunchedFromDeepLinking) {
            Uri target = getActivity().getIntent().getData();
            if (target != null) {
                Intent i = new Intent(getActivity(), GameActivity.class);

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

                    GameRequest.deleteRequest(graphRequestIDForSendingUser + "_" + application.getCurrentFBUser().optString("id"));
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
            completeGameOver();
        }
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(GAME_OVER_MESSAGE_KEY, gameOverMessageDisplaying);
	}

	private void askForFriendsForPlay() {
		if (application.hasDeniedFriendPermission()) {
			startGame();
		} else {		
			new AlertDialog.Builder(getActivity())
			.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					requestFriendsPermission(FRIENDS_PLAY_PERMISSION_REQUEST_CODE);
				}
			})
			.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					application.setHasDeniedFriendPermission(true);
					startGame();
				}
			})
			.setTitle(R.string.with_friends_dialog_title)
			.setMessage(R.string.with_friends_dialog_message)
			.show();
		}
	}

	private void askForFriendsForLeaderboard() {		
		new AlertDialog.Builder(getActivity())
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				requestFriendsPermission(FRIENDS_LEADERBOARD_PERMISSION_REQUEST_CODE);
			}
		})
		.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// nothing to do here
			}
		})
		.setTitle(R.string.leaderboard_dialog_title)
		.setMessage(R.string.leaderboard_dialog_message)
		.show();
	}

	private void askForPublishActionsForScores() {
		new AlertDialog.Builder(getActivity())
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				requestPublishPermissions();				
			}
		})
		.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				hideGameOverContainer();
			}
		})
		.setTitle(R.string.publish_scores_dialog_title)
		.setMessage(R.string.publish_scores_dialog_message)
		.show();		
	}

	private void onPlayButtonTouched() {
        if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.USER_FRIENDS)) {
            startGame();
        } else {
            askForFriendsForPlay();
        }
	}

	private void onChallengeButtonTouched() {
		sendCustomChallenge();
	}	

	private void sendInvite() {
        getHomeActivity().getGameRequest().showDialogForInvites(
                getString(R.string.game_request_dialog_title), getString(R.string.invite_dialog_message));
	}

	private void sendDirectedRequest(List<String> recipients) {
        getHomeActivity().getGameRequest().showDialogForDirectedRequests(
                getString(R.string.game_request_dialog_title),
                getString(R.string.request_dialog_message, application.getTopScore()),
                recipients);
	}

    private void sendDirectedChallenge(List<String> recipient) {
        getHomeActivity().getGameRequest().showDialogForDirectedRequests(
                getString(R.string.game_request_dialog_title),
                getString(R.string.challenge_dialog_message, application.getScore()),
                recipient);
    }

	private void onScoresButtonTouched() {
        if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.USER_FRIENDS)) {
            Intent i = new Intent(getActivity(), ScoreboardActivity.class);
            startActivityForResult(i, 0);
        } else {
            askForFriendsForLeaderboard();
        }
	}
	
	private void onGameOverChallengeButtonTouched() {		
		sendDirectedChallenge(Arrays.asList(application.getLastFriendSmashedID()));
	}
	
	private void onGameOverBragButtonTouched() {
		sendBrag();
	}
	
	private void onGameOverCloseButtonTouched() {
        if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.PUBLISH_ACTIONS)) {
            Sharing.publishScore(application.getScore(), application.getTopScore());
            hideGameOverContainer();
        } else {
            askForPublishActionsForScores();
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
	
	private void loadFriendsForRequests() {
		JSONArray friends = application.getFriends();

        List<JSONObject> listOfFriends = new ArrayList<JSONObject>();
        // arbitrarily truncating the list of friends at 8 to simplify this a bit.
        for (int i = 0; i < friends.length() && i < 8; i++) {
            listOfFriends.add(friends.optJSONObject(i));
        }
			
        final RequestUserArrayAdapter adapter = new RequestUserArrayAdapter(
                getActivity(), listOfFriends);
        requestsGridView.setAdapter(adapter);

        requestsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                JSONObject clickedUser = application.getFriends().optJSONObject(position);
                String uid = clickedUser.optString("id");
                // items act as toggles.
                if (idsToRequest.contains(uid)) {
                    idsToRequest.remove(uid);
                } else {
                    idsToRequest.add(uid);
                }
            }

        });
	}

    private void loadFriendsFromFacebook(final FriendsLoadedCallback callback) {
        GraphAPICall friendsRequest = GraphAPICall.callMeFriends("name,first_name", new GraphAPICallback() {
            @Override
            public void handleResponse(GraphResponse response) {
                application.setFriends(GraphAPICall.getDataFromResponse(response));
                callback.afterFriendsLoaded();
            }

            @Override
            public void handleError(FacebookRequestError error) {
                Log.e(FriendSmashApplication.TAG, error.toString());
                getHomeActivity().showError(error.toString());
            }
        });
        friendsRequest.executeAsync();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		CallbackManager manager = getHomeActivity().getFacebookLogin().getCallbackManager();
		manager.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case Activity.RESULT_OK:
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    application.setScore(bundle.getInt("score"));
                    int coinsCollected = (bundle.getInt("coins_collected"));
                    application.setCoinsCollected(coinsCollected);
                    if (coinsCollected > 0) {
                        application.setCoins(application.getCoins()+coinsCollected);
                    }
                    int bombsUsed = (bundle.getInt("bombs_used"));
                    if (bombsUsed > 0) {
                        application.setBombs(application.getBombs()-bombsUsed);
                    }

                    application.saveInventory();
                    loadInventory();

                    completeGameOver();

                    (getHomeActivity()).getEventsLogger().logGamePlayedEvent(application.getScore());
                }
                break;
            case Activity.RESULT_FIRST_USER:
                if (data != null) {
                    startGame(data.getStringExtra("user_id"));
                }
                break;
            case Activity.RESULT_CANCELED:
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    String error = bundle.getString("error");
                    if (error != null) {
                        getHomeActivity().showError(error);
                    }
                }
        }
	}

    public void onUserFriendsGranted() {
        if (latestPermissionRequestCode == FRIENDS_PLAY_PERMISSION_REQUEST_CODE) {
            latestPermissionRequestCode = 0;
            if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.USER_FRIENDS)) {
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
        } else if (latestPermissionRequestCode == FRIENDS_LEADERBOARD_PERMISSION_REQUEST_CODE) {
            latestPermissionRequestCode = 0;
            if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.USER_FRIENDS)) {
                loadFriendsFromFacebook(new FriendsLoadedCallback() {
                    @Override
                    public void afterFriendsLoaded() {
                        Intent i = new Intent(getActivity(), ScoreboardActivity.class);
                        startActivityForResult(i, 0);
                    }
                });
            }
        }
    }

    public void onPublishActionsGranted() {
        Sharing.publishScore(application.getScore(), application.getTopScore());
        hideGameOverContainer();
    }

    private void startGame() {
        Intent i = new Intent(getActivity(), GameActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("num_bombs", ((FriendSmashApplication) getActivity().getApplication()).getBombs());
        i.putExtras(bundle);
        startActivityForResult(i, 0);
    }

	private void startGame(String userId) {
		Intent i = new Intent(getActivity(), GameActivity.class);
		Bundle bundle = new Bundle();
		bundle.putString("user_id", userId);
		bundle.putInt("num_bombs", ((FriendSmashApplication) getActivity().getApplication()).getBombs());
		i.putExtras(bundle);
		startActivityForResult(i, 0);
	}

	private void completeGameOver() {
		// Set the scoreboardEntriesList to null so that the scoreboard is refreshed
		application.setScoreboardEntriesList(null);

        if (application.getLastFriendSmashedID() != null) {
            youSmashedUserImage.setProfileId(application.getLastFriendSmashedID());
            youSmashedUserImage.setCropped(true);
            youSmashedUserImage.setVisibility(View.VISIBLE);
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

		showGameOverContainer();
	}

	private void sendChallenge() {
        getHomeActivity().getGameRequest().showDialogForRequests(
                getString(R.string.game_request_dialog_title),
                getString(R.string.request_dialog_message, application.getTopScore())
        );
	}

	private void sendCustomChallenge() {
        if (FacebookLogin.isPermissionGranted(FacebookLoginPermission.USER_FRIENDS)) {
            loadFriendsForRequests();
            mainButtonsContainer.setVisibility(View.INVISIBLE);
            challengeContainer.setVisibility(View.VISIBLE);
        } else {
            sendChallenge();
        }
	}

	private void sendBrag() {
		JSONObject currentFBUser = application.getCurrentFBUser();
		// This first parameter is used for deep linking so that anyone who clicks the link
		// will start smashing this user who sent the post
		String link = "https://apps.facebook.com/friendsmashsample/?challenge_brag=";
		if (currentFBUser != null) {
			link += currentFBUser.optString("id");
		}

		String name = getString(R.string.brag_share_name);
        String description = getString(R.string.brag_share_description, application.getScore());
	    String picture = getString(R.string.brag_share_picture);
		
	    Sharing.shareViaDialog(getActivity(), name, description, picture, link);
	}
	
	private void requestPublishPermissions() {
		Log.d(FriendSmashApplication.TAG, "Requesting publish permissions.");
        getHomeActivity().getFacebookLogin().requestPermission(FacebookLoginPermission.PUBLISH_ACTIONS);
    }
		
	private void requestFriendsPermission(int requestCode) {
		Log.d(FriendSmashApplication.TAG, "Requesting friends permissions.");
        latestPermissionRequestCode = requestCode;
        getHomeActivity().getFacebookLogin().requestPermission(FacebookLoginPermission.USER_FRIENDS);
    }

	private HomeActivity getHomeActivity() {
		return (HomeActivity) getActivity();
	}
}