package com.facebook.android.friendsmash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;

public class InviteUserArrayAdapter extends ArrayAdapter<JSONObject> {
	private final Context context;
	private final List<JSONObject> invitableFriends;
	private ImageView profilePicView;
		
	public InviteUserArrayAdapter(Context context, List<JSONObject> invitableFriends) {
		super(context, R.layout.invite_list_item_view, invitableFriends);
		this.context = context;
		this.invitableFriends = invitableFriends;
	}
	
	@Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    View listItemView = inflater.inflate(R.layout.invite_list_item_view, parent, false);
	    
	    profilePicView = (ImageView) listItemView.findViewById(R.id.inviteListItemProfilePic); 
	    TextView nameView = (TextView) listItemView.findViewById(R.id.inviteListItemName);
	    final ImageView checkBox = (ImageView) listItemView.findViewById(R.id.inviteListItemCheckbox); 
	    
	    JSONObject currentUser = invitableFriends.get(position);

	    JSONObject pictureJson = currentUser.optJSONObject("picture")
	    		.optJSONObject("data");	    
	    new ImageDownloader(profilePicView).execute(pictureJson.optString("url"));
	    
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
	
	class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public ImageDownloader(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String url = urls[0];
			Bitmap mIcon = null;
			try {
				InputStream in = new java.net.URL(url).openStream();
				mIcon = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
			}
			return mIcon;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}
}

