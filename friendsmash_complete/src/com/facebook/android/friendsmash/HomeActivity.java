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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.RequestBatch;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  Entry point for the app that represents the home screen with the Play button etc. and
 *  also the login screen for the social version of the app - these screens will switch
 *  within this activity using Fragments.
 */
public class HomeActivity extends FragmentActivity {
	
	// Tag used when logging messages
    private static final String TAG = HomeActivity.class.getSimpleName();
    
    // Uri used in handleError() below
    private static final Uri M_FACEBOOK_URL = Uri.parse("http://m.facebook.com");

	// Declare the UiLifecycleHelper for Facebook session management
    private UiLifecycleHelper fbUiLifecycleHelper;

    // App events logger
    private FriendSmashEventsLogger eventsLogger;

	// Fragment attributes
    private static final int FB_LOGGED_OUT_HOME = 0;
    private static final int HOME = 1;
    private static final int FRAGMENT_COUNT = HOME +1;
    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
 	
 	// Boolean recording whether the activity has been resumed so that
 	// the logic in onSessionStateChange is only executed if this is the case
 	private boolean isResumed = false;
    
 	// Constructor
 	public HomeActivity() {
 		super();
 	}
 	
 	// Getter for the fbUiLifecycleHelper
 	public UiLifecycleHelper getFbUiLifecycleHelper() {
		return fbUiLifecycleHelper;
	}

    // Getter for the app eventsLogger
    public FriendSmashEventsLogger getEventsLogger() { return eventsLogger; }
 	
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            	
    	// Instantiate the fbUiLifecycleHelper and call onCreate() on it
        fbUiLifecycleHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
    		@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				// Add code here to accommodate session changes
    			Log.d(TAG, "Session state changed: " + state + "  permissions: " + session.getPermissions());
    			
    			if (exception != null) 
    				Log.e(TAG, "UiLifecycleHelper exception: " + exception.getMessage());
    			
    			updateView();
    			if (fragments[HOME] != null) {
    				if (state.isOpened()) {
	    				if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	    					// Only callback if the opened token has been updated - i.e. the user
	    					// has provided write permissions
	    					((HomeFragment) fragments[HOME]).tokenUpdated();

	    					Log.d(TAG, "Session state changed: " + state + "  permissions: " + session.getPermissions());
	    	    			Log.d(TAG, "Session state changed2222: " + state + "  permissions: " + Session.getActiveSession().getPermissions());

	                    }
    				}
    			}
			}
            	});
        fbUiLifecycleHelper.onCreate(savedInstanceState);
        eventsLogger = new FriendSmashEventsLogger(fbUiLifecycleHelper.getAppEventsLogger());

		setContentView(R.layout.home);
		
		FragmentManager fm = getSupportFragmentManager();
        fragments[FB_LOGGED_OUT_HOME] = fm.findFragmentById(R.id.fbLoggedOutHomeFragment);
        fragments[HOME] = fm.findFragmentById(R.id.homeFragment);

        FragmentTransaction transaction = fm.beginTransaction();
        for(int i = 0; i < fragments.length; i++) {
            transaction.hide(fragments[i]);
        }
        transaction.commit();
		
		// Restore the logged-in user's information if it has been saved and the existing data in the application
		// has been destroyed (i.e. the app hasn't been used for a while and memory on the device is low)
		// - only do this if the session is open for the social version only
 		if (FriendSmashApplication.IS_SOCIAL) {
			// loggedIn
 			if (savedInstanceState != null) {
				boolean loggedInState = savedInstanceState.getBoolean(FriendSmashApplication.getLoggedInKey(), false);
	 			((FriendSmashApplication)getApplication()).setLoggedIn(loggedInState);
	 			
		 		if ( ((FriendSmashApplication)getApplication()).isLoggedIn() &&
		 			 ( ((FriendSmashApplication)getApplication()).getFriends() == null ||
		 			   ((FriendSmashApplication)getApplication()).getCurrentFBUser() == null) ) {
	 				try {
	 					// currentFBUser
	 					String currentFBUserJSONString = savedInstanceState.getString(FriendSmashApplication.getCurrentFbUserKey());
	 					if (currentFBUserJSONString != null) {
		 					GraphUser currentFBUser = GraphObject.Factory.create(new JSONObject(currentFBUserJSONString), GraphUser.class);
		 					((FriendSmashApplication)getApplication()).setCurrentFBUser(currentFBUser);
	 					}
	 			        
	 			        // friends
	 					ArrayList<String> friendsJSONStringArrayList = savedInstanceState.getStringArrayList(FriendSmashApplication.getFriendsKey());
	 					if (friendsJSONStringArrayList != null) {
		 					ArrayList<GraphUser> friends = new ArrayList<GraphUser>();
		 					Iterator<String> friendsJSONStringArrayListIterator = friendsJSONStringArrayList.iterator();
		 					while (friendsJSONStringArrayListIterator.hasNext()) {
		 							friends.add(GraphObject.Factory.create(new JSONObject(friendsJSONStringArrayListIterator.next()), GraphUser.class));
		 					}
		 					((FriendSmashApplication)getApplication()).setFriends(friends);
	 					}
	 				} catch (JSONException e) {
	 					Log.e(FriendSmashApplication.TAG, e.toString());
	 				}
 				}
	 		}
 		}
    }
 	
 	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
 		
 		// Call onActivityResult on fbUiLifecycleHelper
  		fbUiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
    }
	
 	@Override
    protected void onResumeFragments() {
		super.onResumeFragments();
		if (!FriendSmashApplication.IS_SOCIAL) {
			showFragment(HOME, false);
		} else {
			Session session = Session.getActiveSession();
			if (session != null && session.isOpened() && ((FriendSmashApplication)getApplication()).getCurrentFBUser() != null) {
				showFragment(HOME, false);
			} else {
				showFragment(FB_LOGGED_OUT_HOME, false);
			}
		}
    }
	
	@Override
    public void onResume() {
        super.onResume();
        isResumed = true;
        
        // Call onResume on fbUiLifecycleHelper
  		fbUiLifecycleHelper.onResume();
        
        // Hide the notification bar
 		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
        
        // Call onPause on fbUiLifecycleHelper
  		fbUiLifecycleHelper.onPause();

        AppEventsLogger.deactivateApp(this);
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Call onSaveInstanceState on fbUiLifecycleHelper
  		fbUiLifecycleHelper.onSaveInstanceState(outState);
        
  		// Save the logged-in state
  		outState.putBoolean(FriendSmashApplication.getLoggedInKey(), ((FriendSmashApplication)getApplication()).isLoggedIn());
  		
        // Save the currentFBUser
        if (((FriendSmashApplication)getApplication()).getCurrentFBUser() != null) {
	        outState.putString(FriendSmashApplication.getCurrentFbUserKey(),
	        		((FriendSmashApplication)getApplication()).getCurrentFBUser().getInnerJSONObject().toString());
        }
        
        // Save the logged-in user's list of friends
        if (((FriendSmashApplication)getApplication()).getFriends() != null) {
	        outState.putStringArrayList(FriendSmashApplication.getFriendsKey(),
	        		((FriendSmashApplication)getApplication()).getFriendsAsArrayListOfStrings());
        }
	}
	
	@Override
    public void onDestroy() {
 		super.onDestroy();
 		
 		// Call onDestroy on fbUiLifecycleHelper
  		fbUiLifecycleHelper.onDestroy();
    }
	
	public void buyBombs() {
		// Update bomb and coins count (5 coins per bomb).
		FriendSmashApplication app = (FriendSmashApplication) getApplication();
		
		// check to see that we have enough coins.
		if (app.getCoins() - 5 < 0) {
			Toast.makeText(this, "Not enough coins.", Toast.LENGTH_LONG).show();
			return;
		}
		
		app.setBombs(app.getBombs()+1);
		app.setCoins(app.getCoins()-5);

		// save inventory values
		app.saveInventory();
        
        // Reload inventory values in fragment.
        loadInventoryFragment();
	}
	
    private void showFragment(int fragmentIndex, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (int i = 0; i < fragments.length; i++) {
            if (i == fragmentIndex) {
                transaction.show(fragments[i]);
            } else {
                transaction.hide(fragments[i]);
            }
        }
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
        
        // Do other changes depending on the fragment that is now showing
        if (FriendSmashApplication.IS_SOCIAL) {
        	switch (fragmentIndex) {
        		case FB_LOGGED_OUT_HOME:
        			// Hide the progressContainer in FBLoggedOutHomeFragment 
        			if (fragments[FB_LOGGED_OUT_HOME] != null && ((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]) != null) {
        				((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]).progressContainer.setVisibility(View.INVISIBLE);
        			}
        			// Set the loggedIn attribute
        			((FriendSmashApplication)getApplication()).setLoggedIn(false);
        			break;
        		case HOME:
        			// Set the loggedIn attribute
        			((FriendSmashApplication)getApplication()).setLoggedIn(true);
        			break;
        	}
        }
    }
	
	/* Facebook Integration Only ... */

	// Call back on HomeActivity when the session state changes to update the view accordingly
	private void updateView() {
		if (isResumed) {
			Session session = Session.getActiveSession();
			if (session.isOpened() && !((FriendSmashApplication)getApplication()).isLoggedIn() && fragments[HOME] != null) {
				// Not logged in, but should be, so fetch the user information and log in (load the HomeFragment)
				fetchUserInformationAndLogin();
	        } else if (session.isClosed() && ((FriendSmashApplication)getApplication()).isLoggedIn() && fragments[FB_LOGGED_OUT_HOME] != null) {
				// Logged in, but shouldn't be, so load the FBLoggedOutHomeFragment
	        	logout();
	        	showFragment(FB_LOGGED_OUT_HOME, false);
	        }
			
			// Note that error checking for failed logins is done as within an ErrorListener attached to the
			// LoginButton within FBLoggedOutHomeFragment
		}
	}
	
	// Fetch user information and login (i.e switch to the personalized HomeFragment)
	private void fetchUserInformationAndLogin() {
		final Session session = Session.getActiveSession();				
		if (session != null && session.isOpened()) {
			// If the session is open, make an API call to get user information required for the app
			
			// Show the progress spinner during this network call
			if (fragments[FB_LOGGED_OUT_HOME] != null && 
					((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]).progressContainer != null) {
				((FBLoggedOutHomeFragment)fragments[FB_LOGGED_OUT_HOME]).progressContainer.setVisibility(View.VISIBLE);
			}
			
			// Create a RequestBatch and add a callback once the batch of requests completes
			RequestBatch requestBatch = new RequestBatch();

			// Get a list of friends who have _not installed_ the game. 
			Request invitableFriendsRequest = Request.newGraphPathRequest(session, 
					"/me/invitable_friends", new Request.Callback() {
				
				@Override
				public void onCompleted(Response response) {

					FacebookRequestError error = response.getError();
					if (error != null) {
						Log.e(FriendSmashApplication.TAG, error.toString());
						handleError(error, true);
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
														
							((FriendSmashApplication)getApplication()).setInvitableFriends(invitableFriends);							
						}
					}
				}
				
			});
			Bundle invitableParams = new Bundle();
			invitableParams.putString("fields", "id,first_name,picture");
			invitableFriendsRequest.setParameters(invitableParams);
			requestBatch.add(invitableFriendsRequest);
			
			
			// Check to see that the user granted the user_friends permission before loading friends.
			// This only loads friends who've installed the game.
			if (session.getPermissions().contains("user_friends")) {
				// Get the user's list of friends
				Request friendsRequest = Request.newMyFriendsRequest(session, 
						new Request.GraphUserListCallback() {
	
					@Override
					public void onCompleted(List<GraphUser> users, Response response) {
						FacebookRequestError error = response.getError();
						if (error != null) {
							Log.e(FriendSmashApplication.TAG, error.toString());
							handleError(error, true);
						} else if (session == Session.getActiveSession()) {
							// Set the friends attribute
							((FriendSmashApplication)getApplication()).setFriends(users);
						}
					}
				});
				Bundle params = new Bundle();
				params.putString("fields", "name,first_name,last_name");
				friendsRequest.setParameters(params);
				requestBatch.add(friendsRequest);
			}
			
			// Get current logged in user information
			Request meRequest = Request.newMeRequest(session, 
					new Request.GraphUserCallback() {
				
				@Override
				public void onCompleted(GraphUser user, Response response) {
					FacebookRequestError error = response.getError();
					if (error != null) {
						Log.e(FriendSmashApplication.TAG, error.toString());
						handleError(error, true);
					} else if (session == Session.getActiveSession()) {
						// Set the currentFBUser attribute
						((FriendSmashApplication)getApplication()).setCurrentFBUser(user);
						
						// Now save the user into Parse.
                        saveUserToParse(user, session);
					}
				}
			});
			requestBatch.add(meRequest);

			requestBatch.addCallback(new RequestBatch.Callback() {

				@Override
				public void onBatchCompleted(RequestBatch batch) {
					if ( ((FriendSmashApplication)getApplication()).getCurrentFBUser() != null) {
						// Login by switching to the personalized HomeFragment
						loadPersonalizedFragment();
					} else {
						showError(getString(R.string.error_fetching_profile), true);
					}
				}
			});
			
			// Execute the batch of requests asynchronously
			requestBatch.executeAsync();
		}
	}
	
	private void saveUserToParse(GraphUser fbUser, Session session) {
		ParseFacebookUtils.logIn(fbUser.getId(), session.getAccessToken(), 
				session.getExpirationDate(), new LogInCallback() {
			@Override
			public void done(ParseUser parseUser, ParseException err) {                   
				if (parseUser != null) {
					// The user has been saved to Parse.
					if (parseUser.isNew()) {
						// This user was created during this session with Facebook Login.                       
						Log.d(TAG, "ParseUser created.");

						// Call saveInventory() which will save data to Parse if connected. 
						FriendSmashApplication app = ((FriendSmashApplication)getApplication());
						app.saveInventory();
					} else {
						Log.d(TAG, "User exists in Parse. Pull their values: " + parseUser);

						// This user existed before. Call loadInventory() which has logic
						// to check Parse if connected.
						FriendSmashApplication app = ((FriendSmashApplication)getApplication());
						app.loadInventory();
					}

					loadInventoryFragment();
				} else {
					// The user wasn't saved. Check the exception.
					Log.d(TAG, "User was not saved to Parse: " + err.getMessage());
				}
			}
		});
	}

	// Loads the inventory portion of the HomeFragment. 
    private void loadInventoryFragment() {
    	Log.d(TAG, "Loading inventory fragment");
    	if (isResumed) {
			((HomeFragment)fragments[HOME]).loadInventory();
		} else {
			showError(getString(R.string.error_switching_screens), true);
		}
    }

	// Switches to the personalized HomeFragment as the user has just logged in
	private void loadPersonalizedFragment() {
		if (isResumed) {
			// Personalize the HomeFragment
			((HomeFragment)fragments[HOME]).personalizeHomeFragment();
			
			// Load the HomeFragment personalized
			showFragment(HOME, false);
		} else {
			showError(getString(R.string.error_switching_screens), true);
		}
	}
	
    void handleError(FacebookRequestError error, boolean logout) {
    	Log.d(TAG, "handleError: " + error.getErrorMessage());
    	
        DialogInterface.OnClickListener listener = null;
        String dialogBody = null;

        if (error == null) {
            dialogBody = getString(R.string.error_dialog_default_text);
        } else {
            switch (error.getCategory()) {
                case AUTHENTICATION_RETRY:
                    // tell the user what happened by getting the message id, and
                    // retry the operation later
                    String userAction = (error.shouldNotifyUser()) ? "" :
                            getString(error.getUserActionMessageId());
                    dialogBody = getString(R.string.error_authentication_retry, userAction);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, M_FACEBOOK_URL);
                            startActivity(intent);
                        }
                    };
                    break;

                case AUTHENTICATION_REOPEN_SESSION:
                    // close the session and reopen it.
                    dialogBody = getString(R.string.error_authentication_reopen);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Session session = Session.getActiveSession();
                            if (session != null && !session.isClosed()) {
                                session.closeAndClearTokenInformation();
                            }
                        }
                    };
                    break;

                case PERMISSION:
                    // request the publish permission
                    dialogBody = getString(R.string.error_permission);
                    listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        	if (fragments[HOME] != null) {
                        		((HomeFragment) fragments[HOME]).setPendingPost(true);
                        		((HomeFragment) fragments[HOME]).requestPublishPermissions();
                        	}
                        }
                    };
                    break;

                case SERVER:
                case THROTTLING:
                    // this is usually temporary, don't clear the fields, and
                    // ask the user to try again
                    dialogBody = getString(R.string.error_server);
                    break;

                case BAD_REQUEST:
                    // this is likely a coding error, ask the user to file a bug
                    dialogBody = getString(R.string.error_bad_request, error.getErrorMessage());
                    break;

                case CLIENT:
                	// this is likely an IO error, so tell the user they have a network issue
                	dialogBody = getString(R.string.network_error);
                    break;
                    
                case OTHER:
                default:
                    // an unknown issue occurred, this could be a code error, or
                    // a server side issue, log the issue, and either ask the
                    // user to retry, or file a bug
                    dialogBody = getString(R.string.error_unknown, error.getErrorMessage());
                    break;
            }
        }

        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.error_dialog_button_text, listener)
                .setTitle(R.string.error_dialog_title)
                .setMessage(dialogBody)
                .show();
        
        if (logout) {
        	logout();
        }
    }
	
	// Show user error message as a toast
	void showError(String error, boolean logout) {
		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
		if (logout) {
			logout();
		}
	}
	
    private void logout() {
    	Log.d(TAG, "Logging user out.");
    	
    	// log user out of Parse
    	if (ParseUser.getCurrentUser() != null)
    		ParseUser.logOut();
    	
    	// Close the session, which will cause a callback to show the logout screen
		Session.getActiveSession().closeAndClearTokenInformation();
		
		// Clear any permissions associated with the LoginButton
		LoginButton loginButton = (LoginButton) findViewById(R.id.loginButton);
		if (loginButton != null) {
			loginButton.clearPermissions();
		}
    }
	
}
