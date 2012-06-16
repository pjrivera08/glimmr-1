package com.bourke.glimmr;

import android.os.AsyncTask;

import com.gmail.yuyang226.flickr.Flickr;
import com.gmail.yuyang226.flickr.oauth.OAuth;
import com.gmail.yuyang226.flickr.oauth.OAuthInterface;

/**
 * Gets an access token from Flickr once authorised to access the user's
 * account
 */
public class GetOAuthTokenTask extends AsyncTask<String, Integer, OAuth> {

    private static final String TAG = "Glimmr/GetOAuthTokenTask";

	private BaseFragment mFragment;

	public GetOAuthTokenTask(BaseFragment fragment) {
        mFragment = fragment;
	}

	@Override
	protected OAuth doInBackground(String... params) {
		String oauthToken = params[0];
		String oauthTokenSecret = params[1];
		String verifier = params[2];

		Flickr f = FlickrHelper.getInstance().getFlickr();
		OAuthInterface oauthApi = f.getOAuthInterface();
		try {
			return oauthApi.getAccessToken(oauthToken, oauthTokenSecret,
					verifier);
		} catch (Exception e) {
            e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void onPostExecute(OAuth result) {
		if (mFragment != null) {
			mFragment.onOAuthDone(result);
		}
	}
}
