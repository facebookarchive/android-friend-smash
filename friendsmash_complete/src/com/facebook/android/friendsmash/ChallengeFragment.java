package com.facebook.android.friendsmash;

import java.util.List;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ChallengeFragment extends Fragment {

	private static final String TAG = ChallengeFragment.class.getSimpleName();

	private FriendSmashApplication application;

	private LinearLayout challengeFriendsContainer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		application = (FriendSmashApplication) getActivity().getApplication();
		
//		
//		// Instantiate the handler
//		uiHandler = new Handler();
//		
		setRetainInstance(true);
	}
	
	@TargetApi(13)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_challenge, parent, false);		
		
		challengeFriendsContainer = (LinearLayout)v.findViewById(R.id.challengeFriendsContainer);
//		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);
//
//		// Set the progressContainer as invisible by default
//		progressContainer.setVisibility(View.INVISIBLE);
		
		// Note: Scoreboard is populated during onResume below
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();

		fetchFriendsWhoPlay();
		
		/*
		// Populate scoreboard - fetch information if necessary ...
		if (application.getScoreboardEntriesList() == null) {
			// scoreboardEntriesList is null, so fetch the information from Facebook (scoreboard will be updated in
			// the scoreboardEntriesFetched callback) and show the progress spinner while doing so
			progressContainer.setVisibility(View.VISIBLE);
			fetchScoreboardEntries();
		} else {
			// Information has already been fetched, so populate the scoreboard
			populateScoreboard();
		}
		*/
	}
	
	
	private void fetchFriendsWhoPlay () {
		String fbAppID = application.getFBAppID();		
		final Session session = Session.getActiveSession();				

		Request friendsRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() {

			@Override
			public void onCompleted(List<GraphUser> users, Response response) {
				Log.d(TAG, "great success!");

				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(TAG, error.toString());
					// TODO: Show an error or handle it better.
					//((ScoreboardActivity)getActivity()).handleError(error, false);
				} else if (session == Session.getActiveSession()) {
					
					Log.d(TAG, "Got friends: " + users);
					
					/*
					if (response != null) {						
						GraphObject graphObject = response.getGraphObject();
						JSONArray dataArray = (JSONArray)graphObject.getProperty("data");

						ArrayList<ScoreboardEntry> scoreboardEntriesList = new ArrayList<ScoreboardEntry>();

						for (int i=0; i< dataArray.length(); i++) {
							JSONObject oneData = dataArray.optJSONObject(i);							
							int score = oneData.optInt("score");
							
							JSONObject userObj = oneData.optJSONObject("user");							
							String userID = userObj.optString("id");
							String userName = userObj.optString("name");
							
							scoreboardEntriesList.add(new ScoreboardEntry(userID, userName, score));
						}
						
						Comparator<ScoreboardEntry> comparator = Collections.reverseOrder();
						Collections.sort(scoreboardEntriesList, comparator);
						application.setScoreboardEntriesList(scoreboardEntriesList);
						
						// Populate the scoreboard on the UI thread
						uiHandler.post(new Runnable() {
							@Override
							public void run() {
								populateScoreboard();
							}
						});
						
					}
						*/
				}
			}
		});
		Request.executeBatchAsync(friendsRequest);
	}
}
