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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.login.widget.ProfilePictureView;

import org.json.JSONObject;

import java.util.List;

public class RequestUserArrayAdapter extends ArrayAdapter<JSONObject> {
	private final Context context;
	private final List<JSONObject> users;
		
	public RequestUserArrayAdapter(Context context, List<JSONObject> users) {
		super(context, R.layout.request_list_item_view, users);
		this.context = context;
		this.users = users;
	}
	
	@Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    View listItemView = inflater.inflate(R.layout.request_list_item_view, parent, false);
	    ProfilePictureView profilePicView = (ProfilePictureView) listItemView.findViewById(R.id.requestListItemProfilePic);
	    TextView nameView = (TextView) listItemView.findViewById(R.id.requestListItemName);
	    final ImageView checkBox = (ImageView) listItemView.findViewById(R.id.requestListItemCheckbox); 
	    
	    JSONObject currentUser = users.get(position);
	    
	    profilePicView.setProfileId(currentUser.optString("id"));
	    profilePicView.setCropped(true);        
	    nameView.setText(currentUser.optString("first_name"));
	    
	    checkBox.setOnTouchListener(new View.OnTouchListener() {
	    	boolean checked = false;
	    	
            @Override
			public boolean onTouch(View v, MotionEvent event) {
            	// toggle image 
            	if (checked) {
            		checked = false;
            		checkBox.setImageResource(R.drawable.checkbox_cold);
            	} else {
            		checked = true;
            		checkBox.setImageResource(R.drawable.checkbox_hot);
            	}
				return false;
			}
        });
	    
	    return listItemView;
	  }
}
