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

package com.facebook.android.friendsmash.integration;

import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.android.friendsmash.FriendSmashApplication;
import com.facebook.android.friendsmash.HomeActivity;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class handles Facebook Login using LoginButton.
 * For more information on Facebook Login for Android see
 * https://developers.facebook.com/docs/facebook-login/android/
 */
public class FacebookLogin {

    /**
     * HomeActivity is the activity handling Facebook Login in the app. Needed here to send
     * the signal back when user successfully logged in.
     */
    private HomeActivity activity;

    /**
     * CallbackManager is a Facebook SDK class managing the callbacks into the FacebookSdk from
     * an Activity's or Fragment's onActivityResult() method.
     * For more information see
     * https://developers.facebook.com/docs/reference/android/current/interface/CallbackManager/
     */
    private CallbackManager callbackManager;

    /**
     * CallbackManager is exposed here to so that onActivityResult() can be called from Activities
     * and Fragments when required. This is necessary so that the login result is passed to the
     * LoginManager
     */
    public CallbackManager getCallbackManager() { return callbackManager; }

    /**
     * AccessTokenTracker allows for tracking whenever the access token changes - whenever user logs
     * in, logs out etc abstract method onCurrentAccessTokenChanged is called.
     */
    private AccessTokenTracker tokenTracker;

    public FacebookLogin(HomeActivity activity) {
        super();
        this.activity = activity;
    }

    /**
     * Needs to be called after Facebook SDK has been initialized with a FacebookSdk.sdkInitialize()
     * It overrides a method in AccessTokenTracker to get notified whenever AccessToken is
     * changed, which typically means user logged in, logged out or granted new permissions.
     * For more information see
     * https://developers.facebook.com/docs/reference/android/current/class/AccessTokenTracker/
     * https://developers.facebook.com/docs/reference/android/current/interface/FacebookCallback/
     */
     public void init() {
         callbackManager = CallbackManager.Factory.create();
         tokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                activity.onLoginStateChanged(oldAccessToken, currentAccessToken);
            }
        };
    }

    /**
     * Called when HomeActivity resumes. Ensures tokenTracker tracks token changes
     */
    public void activate() {
        tokenTracker.startTracking();
    }

    /**
     * Called when HomeActivity is paused. Ensures tokenTracker stops tracking
     */
    public void deactivate() {
        tokenTracker.stopTracking();
    }

    /**
     * LoginButton can be used to trigger the login dialog asking for any permission so it is
     * important to specify which permissions you want to request from a user. In Friend Smash case
     * only user_friends is required to enable access to friends, so that the game can show friends'
     * profile picture to make the experience more personal and engaging.
     * For more info on permissions see
     * https://developers.facebook.com/docs/facebook-login/android/permissions
     * This method is called from onCreateView() of a Fragment displayed when user is logged out of
     * Facebook.
     */
    public void setUpLoginButton(LoginButton button) {
        button.setReadPermissions(FacebookLoginPermission.USER_FRIENDS.toString());
        button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.w("DEBUG", "onLoginButtonSuccess");
                activity.onLoginStateChanged(null, AccessToken.getCurrentAccessToken());
            }

            @Override
            public void onCancel() {
                Log.w("DEBUG", "on Login Cancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(FriendSmashApplication.TAG, error.toString());
            }
        });
    }

    /**
     * Uses LoginManager to request additional permissions when needed, e.g. when user has finished
     * the game and is trying to post score the game would call this method to request publish_actions
     * See https://developers.facebook.com/docs/facebook-login/android/permissions for more info on
     * Login permissions
     */
    public void requestPermission (FacebookLoginPermission permission) {
        if (!isPermissionGranted(permission)) {
            Collection<String> permissions = new ArrayList<String>(1);
            permissions.add(permission.toString());
            if (permission.isRead()) {
                LoginManager.getInstance().logInWithReadPermissions(activity, permissions);
            } else {
                LoginManager.getInstance().logInWithPublishPermissions(activity, permissions);
            }
        }
    }

    /**
     * Helper function checking if user is logged in and access token hasn't expired.
     */
    public static boolean isAccessTokenValid() {
        return testAccessTokenValid(AccessToken.getCurrentAccessToken());
    }
    /**
     * Helper function checking if user has granted particular permission to the app
     * For more info on permissions see
     * https://developers.facebook.com/docs/facebook-login/android/permissions
     */
    public static boolean isPermissionGranted(FacebookLoginPermission permission) {
        return testTokenHasPermission(AccessToken.getCurrentAccessToken(), permission);
    }

    /**
     * Helper function checking if the given access token is valid
     */
    public static boolean testAccessTokenValid(AccessToken token) {
        return token != null && !token.isExpired();
    }

    /**
     * Helper function checking if the given access token includes specified login permission
     */
    public static boolean testTokenHasPermission(AccessToken token, FacebookLoginPermission permission) {
        return testAccessTokenValid(token) && token.getPermissions().contains(permission.toString());
    }

}
