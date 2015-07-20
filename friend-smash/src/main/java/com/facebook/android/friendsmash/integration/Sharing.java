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

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.facebook.FacebookRequestError;
import com.facebook.GraphResponse;
import com.facebook.android.friendsmash.FriendSmashApplication;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

/**
 * This class handles Facebook Sharing. In Android version of Friend Smash sharing is used
 * for two purposes: publishing scores using Scores API and sharing completed game using
 * Share Dialog.
 * See https://developers.facebook.com/docs/games/scores and
 * https://developers.facebook.com/docs/games/sharing for more details
 */
public class Sharing {

    /**
     * Publishes the score if it is higher than the current topScore.
     * This allows Friend Smash! to implement friends leader board keeping track of the top scores
     * achieved by the player and their friends.
     * See https://developers.facebook.com/docs/games/scores
     */
    public static void publishScore(int score, int topScore) {
        if (score > topScore) {
            GraphAPICall publishScoreCall = GraphAPICall.publishScore(score, new GraphAPICallback() {
                @Override
                public void handleResponse(GraphResponse response) {
                    Log.i(FriendSmashApplication.TAG, "Score posted successfully to Facebook");
                }

                @Override
                public void handleError(FacebookRequestError error) {
                    Log.e(FriendSmashApplication.TAG, "Posting Score to Facebook failed: " + error.getErrorMessage());
                }
            });
            publishScoreCall.executeAsync();
        }
    }

    /**
     * Shares a story using ShareDialog. Story's name, description, picture and link can be specified.
     * Activity hosting the dialog is also required to show the dialog.
     * See https://developers.facebook.com/docs/reference/android/current/class/ShareDialog/
     */
    public static void shareViaDialog (Activity activity, String name, String description,
                                       String picture, String link) {
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareDialog shareDialog = new ShareDialog(activity);
            ShareLinkContent content = new ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse(link))
                    .setContentTitle(name)
                    .setContentDescription(description)
                    .setImageUrl(Uri.parse(picture))
                    .build();

            shareDialog.show(content);
        }
    }
}
