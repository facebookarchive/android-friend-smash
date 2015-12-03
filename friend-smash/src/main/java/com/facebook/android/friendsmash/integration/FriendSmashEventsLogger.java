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

import android.content.Context;
import android.os.Bundle;

import com.facebook.appevents.AppEventsLogger;

/**
 * Class responsible for logging AppEvents in Friend Smash!
 */
public class FriendSmashEventsLogger {
    private AppEventsLogger logger;

    /**
     * Context is needed to create an instance of AppEventsLogger.
     * See https://developers.facebook.com/docs/reference/android/current/class/AppEventsLogger/
     */
    public FriendSmashEventsLogger(Context context) {
        logger = AppEventsLogger.newLogger(context);
    }

    /**
     * Logs a custom App Event when player has completed the game with given score.
     * To understand how to best use App Events see
     * https://developers.facebook.com/docs/app-events/best-practices
     */
    public void logGamePlayedEvent(int score) {
        Bundle params = new Bundle();
        params.putInt(FriendSmashCustomAppEvent.EVENT_PARAM_SCORE, score);
        logger.logEvent(FriendSmashCustomAppEvent.EVENT_NAME_GAME_PLAYED, params);
    }
}
