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

import android.app.Application;
import android.content.SharedPreferences;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class FriendSmashApplication extends Application {

	// Tag used when logging all messages with the same tag (e.g. for demoing purposes)
	public static final String TAG = "FriendSmash";

	public static int NEW_USER_BOMBS = 5;
	public static int NEW_USER_COINS = 100;
	public static int NUM_BOMBS_ALLOWED_IN_GAME = 3;
	public static int NUM_COINS_PER_BOMB = 5;
	private int score = 0;
	private int bombs = 0;
	private int coins = 0;
	private int coinsCollected = 0;
    private int topScore = 0;

	private boolean loggedIn = false;
	public static final String LOGGED_IN_KEY = "logged_in";

	private JSONObject currentFBUser;
	public static final String CURRENT_FB_USER_KEY = "current_fb_user";

	private JSONArray friends;
	
	public static final String FRIENDS_KEY = "friends";

	private String lastFriendSmashedID = null;
	
	private String lastFriendSmashedName = null;
	
	private boolean hasDeniedFriendPermission = false;

	private ArrayList<ScoreboardEntry> scoreboardEntriesList = null;
	
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

    public int getTopScore() { return topScore; }

    public void setTopScore(int topScore) { this.topScore = topScore; }
	
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
		if (!loggedIn) {
			setScore(0);
			setCurrentFBUser(null);
        	setFriends(null);
        	setLastFriendSmashedID(null);
        	setScoreboardEntriesList(null);
		}
	}

	public JSONObject getCurrentFBUser() {
		return currentFBUser;
	}

	public void setCurrentFBUser(JSONObject currentFBUser) {
		this.currentFBUser = currentFBUser;
	}

	public JSONArray getFriends() {
		return friends;
	}

	public ArrayList<String> getFriendsAsArrayListOfStrings() {
		ArrayList<String> friendsAsArrayListOfStrings = new ArrayList<String>();
		
		int numFriends = friends.length();
		for (int i = 0; i < numFriends; i++) {
            friendsAsArrayListOfStrings.add(getFriend(i).toString());
		}
		
		return friendsAsArrayListOfStrings;
	}
	
	public JSONObject getFriend(int index) {
        JSONObject friend = null;
        if (friends != null && friends.length() > index) {
            friend = friends.optJSONObject(index);
        }
        return friend;
    }
	
	public void setFriends(JSONArray friends) {
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

	public String getFBAppID() {
		return getString(R.string.facebook_app_id);
	}

	public void saveInventory() {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences("Inventory", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("bombs", getBombs());
        editor.putInt("coins", getCoins());
        editor.putLong("lastSavedTime", System.currentTimeMillis());
        editor.commit();		

        if (ParseUser.getCurrentUser() != null) {
    		ParseUser.getCurrentUser().put("bombs", getBombs());
    		ParseUser.getCurrentUser().put("coins", getCoins());
    		ParseUser.getCurrentUser().saveInBackground();        	
        }
	}
	
	public void loadInventory() {
        if (ParseUser.getCurrentUser() != null) {
    		setBombs(ParseUser.getCurrentUser().getInt("bombs"));
    		setCoins(ParseUser.getCurrentUser().getInt("coins"));                      
        } else { 
	        SharedPreferences prefs = getApplicationContext().getSharedPreferences("Inventory", MODE_PRIVATE);
	        long lastSavedTime = prefs.getLong("lastSavedTime", 0);
	
	        if (lastSavedTime == 0) {
	    		setBombs(NEW_USER_BOMBS);
	    		setCoins(NEW_USER_COINS);
	        } else {
	            setBombs(prefs.getInt("bombs", 0));
	            setCoins(prefs.getInt("coins", 0));
	        }
        }
	}
	
	public void onCreate() {
		super.onCreate();
		Parse.initialize(this, getString(R.string.parse_app_id), getString(R.string.parse_client_key));
		ParseFacebookUtils.initialize(this);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        loadInventory();
	}

	
}
