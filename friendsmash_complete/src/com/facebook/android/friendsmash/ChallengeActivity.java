package com.facebook.android.friendsmash;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

/**
 *
 */
public class ChallengeActivity extends SingleFragmentActivity {

	private static final String TAG = ChallengeActivity.class.getSimpleName();

	@Override
	protected Fragment createFragment() {
		return new ChallengeFragment();
	}
	
	public void onSwitchClicked(View view) {
	    // Is the toggle on?
	    boolean on = ((Switch) view).isChecked();
	    
	    if (on) {
	        // Enable vibrate
	    	Log.d(TAG, "on!");
	    } else {
	        // Disable vibrate
	    	Log.d(TAG, "off :(");
	    }
	}

}
