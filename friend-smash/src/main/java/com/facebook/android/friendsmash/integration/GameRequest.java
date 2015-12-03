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

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphResponse;
import com.facebook.android.friendsmash.FriendSmashApplication;
import com.facebook.android.friendsmash.HomeActivity;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.widget.GameRequestDialog;

import org.json.JSONObject;

import java.util.List;

/**
 * Class dealing with Game Requests: launching the Request Dialog, retrieving request data and
 * deleting consumed requests. It implements FacebookCallback<GameRequestDialog.Result>
 * to be notified when the request dialog was closed.
 * For more information about GameRequests see https://developers.facebook.com/docs/games/requests/
 */
public class GameRequest implements FacebookCallback<GameRequestDialog.Result> {

    /**
     * This is the Request Dialog used to send requests. See
     *https://developers.facebook.com/docs/reference/android/current/class/GameRequestDialog/
     */
    private GameRequestDialog dialog;

    /**
     * Creates the dialog and register itself as the callback handler. HomeActivity is passed here
     * as the dialog needs to be aware of the activity hosting it and the CallbackManager is needed
     * to register the callback.
     */
    public GameRequest(HomeActivity activity) {
        dialog = new GameRequestDialog(activity);
        dialog.registerCallback(activity.getFacebookLogin().getCallbackManager(), this);
    }

    /**
     * Shows the reqeust dialog with given title and message. The dialog will be populated with
     * friends who are not currently playing the game and gives the player an opportunity
     * to invite their friends to play.
     */
    public void showDialogForInvites (String title, String message) {
        GameRequestContent content = new GameRequestContent.Builder().
                setTitle(title).
                setMessage(message).
                setFilters(GameRequestContent.Filters.APP_NON_USERS).
                build();
        dialog.show(content);
    }

    /**
     * Shows the request dialog with given title and message. The dialog will be populated with
     * friends who are currently playing the game and gives the player an opportunity to challenge
     * and re-engage the existing players
     */
    public void showDialogForRequests (String title, String message) {
        GameRequestContent content = new GameRequestContent.Builder().
                setTitle(title).
                setMessage(message).
                setFilters(GameRequestContent.Filters.APP_USERS).
                build();
        dialog.show(content);
    }

    /**
     * Shows the request dialog with given title and message. The dialog will be populated with
     * specified recipients giving the player an opportunity to challenge a specific friend
     */
    public void showDialogForDirectedRequests (String title, String message, List<String> recipients) {
        GameRequestContent content = new GameRequestContent.Builder().
                setTitle(title).
                setMessage(message).
                setRecipients(recipients).build();
        dialog.show(content);
    }

    /**
     * Deletes the specified request. Once player has consumed a request (e.g. accepted a challenge)
     * it should be deleted to remove it from Facebook channels.
     */
    public static void deleteRequest (String requestId) {
        GraphAPICall deleteRequestRequest = GraphAPICall.deleteRequest(
                requestId,
                new GraphAPICallback() {
                    @Override
                    public void handleResponse(GraphResponse response) {
                        Log.i(FriendSmashApplication.TAG, "Consumed Request deleted");
                    }

                    @Override
                    public void handleError(FacebookRequestError error) {
                        Log.e(FriendSmashApplication.TAG, "Deleting consumed Request failed: " + error.getErrorMessage());
                    }
                });
        deleteRequestRequest.executeAsync();
    }

    /**
     * Retrieves the user data from the specified request.
     * In Friend Smash! when person accepts a challenge request they should enter the game directly
     * to the game session set up to smash the friend who sent the request. In order to do this the
     * game needs to know user id and the name of the sender.
     * What's happening in the code below is:
     * 1. Graph API call to retrieve request details. This call returns user id of the sender.
     * See https://developers.facebook.com/docs/graph-api/reference/request
     * 2. Graph API call to retrieve the first_name of the sender. Result of this call is passed
     * back to the GraphAPICallback passed to this method as a parameter allowing the caller of
     * this method to use the retrieved user data.
     * See https://developers.facebook.com/docs/graph-api/reference/user
     */
    public static void getUserDataFromRequest (String requestId, final GraphAPICallback callback) {
        GraphAPICall requestCall = GraphAPICall.callRequest(requestId, new GraphAPICallback() {
            @Override
            public void handleResponse(GraphResponse response) {
                if (response != null) {
                    JSONObject fromObject = response.getJSONObject().optJSONObject("from");
                    String userId = fromObject.optString("id");
                    GraphAPICall userCall = GraphAPICall.callUser(userId, "first_name", callback);
                    userCall.executeAsync();
                }
            }

            @Override
            public void handleError(FacebookRequestError error) {
                Log.e(FriendSmashApplication.TAG, error.toString());
            }
        });
        requestCall.executeAsync();
    }

    /**
     * The 3 methods below are the implementation of FacebookCallback<GameRequestDialog.Result> and
     * get called when user successfully sent requests, canceled the flow or encountered an error.
     */

    @Override
    public void onSuccess(GameRequestDialog.Result result) {
        Log.i(FriendSmashApplication.TAG, "Game Request sent successfully, request id=" + result.getRequestId());
    }

    @Override
    public void onCancel() {
        Log.i(FriendSmashApplication.TAG, "Sending Game Request has been cancelled");
    }

    @Override
    public void onError(FacebookException error) {
        Log.e(FriendSmashApplication.TAG, error.toString());
    }
}
