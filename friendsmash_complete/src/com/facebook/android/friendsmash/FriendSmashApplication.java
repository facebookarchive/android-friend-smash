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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.FacebookRequestError;
import com.facebook.model.GraphUser;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

/**
 *  Use a custom Application class to pass state data between Activities.
 */
public class FriendSmashApplication extends Application {

	/* Static Attributes */
	
	// Tag used when logging all messages with the same tag (e.g. for demoing purposes)
	static final String TAG = "FriendSmash";
	
	// Switch between the non-social and social Facebook versions of the game
	static final boolean IS_SOCIAL = true;

	
	/* Friend Smash application attributes */
	
	// Player inventory
	public static int NEW_USER_BOMBS = 5;
	public static int NEW_USER_COINS = 100;
	public static int NUM_BOMBS_ALLOWED_IN_GAME = 3;
	private int score = -1;
	private int bombs = 0;
	private int coins = 0;
	private int coinsCollected = 0;
	
	/* Facebook application attributes */

	// Logged in status of the user
	private boolean loggedIn = false;
	private static final String LOGGED_IN_KEY = "logged_in";
	
	private String fbAppID = null;
	
	// Current logged in FB user and key for saving/restoring during the Activity lifecycle
	private GraphUser currentFBUser;
	private static final String CURRENT_FB_USER_KEY = "current_fb_user";
	
	// List of the logged in user's friends and key for saving/restoring during the Activity lifecycle
	private List<GraphUser> friends;
	
	// List of friends the user can invite (have not installed the app).
	private List<JSONObject> invitableFriends;
	
	private static final String FRIENDS_KEY = "friends";
		
	// ID of the last friend smashed (linked to the current score)
	private String lastFriendSmashedID = null;
	
	// Name of the last friend smashed
	private String lastFriendSmashedName = null;
	
	// Check to see whether user has said no when asked to play with friends.
	private boolean hasDeniedFriendPermission = false;
	
	// List of ordered ScoreboardEntry objects in order from highest to lowest score to
	// be shown in the ScoreboardFragment
	private ArrayList<ScoreboardEntry> scoreboardEntriesList = null;
	
	// FacebookRequestError to show when the GameFragment closes
	private FacebookRequestError gameFragmentFBRequestError = null;
		

	/* Friend Smash application attribute getters & setters */
	
	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getBombs() {
		return bombs;
	}

	public void setBombs(int bombs) {
		this.bombs = bombs;
	}

	public int getCoins() {
		return coins;
	}

	public void setCoins(int coins) {
		this.coins = coins;		
	}
	
	public int getCoinsCollected() {
		return coinsCollected;
	}

	public void setCoinsCollected(int coinsCollected) {
		this.coinsCollected = coinsCollected;		
	}
	
	/* Facebook attribute getters & setters */
	
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
		if (!loggedIn) {
			// If the user is logged out, reset the score and nullify all the logged-in user's values
			setScore(-1);
			setCurrentFBUser(null);
        	setFriends(null);
        	setLastFriendSmashedID(null);
        	setScoreboardEntriesList(null);
		}
	}

	public GraphUser getCurrentFBUser() {
		return currentFBUser;
	}

	public void setCurrentFBUser(GraphUser currentFBUser) {
		this.currentFBUser = currentFBUser;
	}

	public List<GraphUser> getFriends() {
		return friends;
	}
	
	// Method to get the list of friends in an ArrayList<String> where each entry
	// is an inner JSON objects of each friend represented as a string - used for
	// saving/restoring each friend during the Activity lifecycle
	public ArrayList<String> getFriendsAsArrayListOfStrings() {
		ArrayList<String> friendsAsArrayListOfStrings = new ArrayList<String>();
		
		Iterator<GraphUser> friendsIterator = friends.iterator();
		while (friendsIterator.hasNext()) {
			friendsAsArrayListOfStrings.add(friendsIterator.next().getInnerJSONObject().toString());
		}
		
		return friendsAsArrayListOfStrings;
	}
	
	public GraphUser getFriend(int index) {
		if (friends != null && friends.size() > index) {
			return friends.get(index);
		} else {
			return null;
		}
	}
	
	public void setFriends(List<GraphUser> friends) {
		this.friends = friends;
	}

	public String getLastFriendSmashedID() {
		return lastFriendSmashedID;
	}

	public void setLastFriendSmashedID(String lastFriendSmashedID) {
		this.lastFriendSmashedID = lastFriendSmashedID;
	}
	
	public String getLastFriendSmashedName() {
		return lastFriendSmashedName;
	}

	public void setLastFriendSmashedName(String lastFriendSmashedName) {
		this.lastFriendSmashedName = lastFriendSmashedName;
	}
	
	public boolean hasDeniedFriendPermission() {
		return hasDeniedFriendPermission;
	}

	public void setHasDeniedFriendPermission(boolean hasDeniedFriendPermission) {
		this.hasDeniedFriendPermission = hasDeniedFriendPermission;
	}

	public ArrayList<ScoreboardEntry> getScoreboardEntriesList() {
		return scoreboardEntriesList;
	}

	public void setScoreboardEntriesList(ArrayList<ScoreboardEntry> scoreboardEntriesList) {
		this.scoreboardEntriesList = scoreboardEntriesList;
	}

	public FacebookRequestError getGameFragmentFBRequestError() {
		return gameFragmentFBRequestError;
	}

	public void setGameFragmentFBRequestError(FacebookRequestError gameFragmentFBRequestError) {
		this.gameFragmentFBRequestError = gameFragmentFBRequestError;
	}

	public static String getLoggedInKey() {
		return LOGGED_IN_KEY;
	}

	public String getFBAppID() {
		return fbAppID;
	}

	public static String getCurrentFbUserKey() {
		return CURRENT_FB_USER_KEY;
	}

	public static String getFriendsKey() {
		return FRIENDS_KEY;
	}
	
	public List<JSONObject> getInvitableFriends() {
		return invitableFriends;
	}

	public void setInvitableFriends(List<JSONObject> invitableFriends) {
		this.invitableFriends = invitableFriends;
	}

	public void saveInventory() {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences("Inventory", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("bombs", getBombs());
        editor.putInt("coins", getCoins());
        editor.putLong("lastSavedTime", System.currentTimeMillis());
        editor.commit();		
        
        // Store data to Parse too.
        if (ParseUser.getCurrentUser() != null) {
    		ParseUser.getCurrentUser().put("bombs", getBombs());
    		ParseUser.getCurrentUser().put("coins", getCoins());
    		ParseUser.getCurrentUser().saveInBackground();        	
        }
	}
	
	
	/*
	 * The logic here is to check if we're connected to Parse. If we are, accept the data
	 * there as the authoritative source of data. If we are not connected, then look for
	 * data that is stored locally. If that doesn't exist, then use some default values.
	 * 
	 * In your own project, you may want to have more sophisticated conflict resolution. 
	 * For example, you may want to use lastSavedTime as a timestamp that could be 
	 * compared to the timestamp of data pulled from Parse and then use whichever data
	 * was the most recent. You may want to do this if you want support offline gaming. 
	 * 
	 */
	public void loadInventory() {
        if (ParseUser.getCurrentUser() != null) {
    		setBombs(ParseUser.getCurrentUser().getInt("bombs"));
    		setCoins(ParseUser.getCurrentUser().getInt("coins"));                      
        } else { 
	        SharedPreferences prefs = getApplicationContext().getSharedPreferences("Inventory", MODE_PRIVATE);
	        long lastSavedTime = prefs.getLong("lastSavedTime", 0);
	
	        if (lastSavedTime == 0) {
	        	// Have never saved state. Initialize.
	    		setBombs(NEW_USER_BOMBS);
	    		setCoins(NEW_USER_COINS);
	        } else {
	            setBombs(prefs.getInt("bombs", 0));
	            setCoins(prefs.getInt("coins", 0));
	        }
        }
	}
	
	public void onCreate() {
		fbAppID = getString(R.string.app_id);
		Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
		ParseFacebookUtils.initialize(fbAppID);

		loadInventory();		
	}

	
}
