package com.facebook.android.friendsmash;

import android.os.Bundle;

import com.facebook.AppEventsLogger;

public class FriendSmashEventsLogger {
    AppEventsLogger logger;

    public FriendSmashEventsLogger(AppEventsLogger logger) {
        this.logger = logger;
    }

    public void logGamePlayedEvent(int score) {
        Bundle params = new Bundle();
        params.putInt(FriendSmashCustomAppEvent.EVENT_PARAM_SCORE, score);
        logger.logEvent(FriendSmashCustomAppEvent.EVENT_NAME_GAME_PLAYED, params);
    }
}
