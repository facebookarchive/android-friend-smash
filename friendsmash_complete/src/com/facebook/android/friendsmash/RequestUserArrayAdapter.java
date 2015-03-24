package com.facebook.android.friendsmash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

import java.util.List;

public class RequestUserArrayAdapter extends ArrayAdapter<GraphUser> {
	private final Context context;
	private final List<GraphUser> users;
		
	public RequestUserArrayAdapter(Context context, List<GraphUser> users) {
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
	    
	    GraphUser currentUser = users.get(position);
	    
	    profilePicView.setProfileId(currentUser.getId());
	    profilePicView.setCropped(true);        
	    nameView.setText(currentUser.getFirstName());
	    
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
