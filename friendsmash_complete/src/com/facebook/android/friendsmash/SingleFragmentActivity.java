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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;

import com.facebook.AppEventsLogger;

/**
 *  Only used by Activities where a single Fragment is used and not changed (i.e. used by
 *  all Activities except HomeActivity
 */
public abstract class SingleFragmentActivity extends FragmentActivity {
	
	abstract Fragment createFragment();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment);
		FragmentManager manager = getSupportFragmentManager();
		Fragment fragment = manager.findFragmentById(R.id.fragmentContainer);
		
		if (fragment == null)
		{
			fragment = createFragment();
			manager.beginTransaction()
				.add(R.id.fragmentContainer, fragment)
				.commit();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Hide the notification bar
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// Measure mobile app install ads
 		// Ref: https://developers.facebook.com/docs/tutorials/mobile-app-ads/
		AppEventsLogger.activateApp(this, ((FriendSmashApplication)getApplication()).getString(R.string.app_id));
	}

}
