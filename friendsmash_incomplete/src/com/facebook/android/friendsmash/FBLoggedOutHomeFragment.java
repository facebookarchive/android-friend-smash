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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.OnErrorListener;

/**
 *  Fragment to be displayed if the user is logged out of Facebook in the social version of the game
 */
public class FBLoggedOutHomeFragment extends Fragment {
	
	View progressContainer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		// Hide the notification bar
		getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_home_fb_logged_out, parent, false);
		
		progressContainer = v.findViewById(R.id.progressContainer);
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Set an error listener for the login button
		LoginButton loginButton = (LoginButton) v.findViewById(R.id.loginButton);
		loginButton.setReadPermissions("user_friends");
		if (loginButton != null) {
			loginButton.setOnErrorListener(new OnErrorListener() {
	
				@Override
				public void onError(FacebookException error) {
					if (error != null && !(error instanceof FacebookOperationCanceledException)) {
						// Failed probably due to network error (rather than user canceling dialog which would throw a FacebookOperationCanceledException)
						((HomeActivity)getActivity()).showError(getResources().getString(R.string.network_error), false);
					}
				}
				
			});
		}
		
		return v;
	}

}
