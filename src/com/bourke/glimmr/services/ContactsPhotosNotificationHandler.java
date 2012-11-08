package com.bourke.glimmr.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;

import android.util.Log;

import com.bourke.glimmr.activities.MainActivity;
import com.bourke.glimmr.common.Constants;
import com.bourke.glimmr.event.Events.IPhotoListReadyListener;
import com.bourke.glimmr.R;
import com.bourke.glimmr.tasks.LoadContactsPhotosTask;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import com.googlecode.flickrjandroid.oauth.OAuth;
import com.googlecode.flickrjandroid.photos.Photo;

import com.jakewharton.notificationcompat2.NotificationCompat2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContactsPhotosNotificationHandler
    implements GlimmrNotificationHandler<Photo>, IPhotoListReadyListener {

    private static final String TAG =
        "Glimmr/ContactsPhotosNotificationHandler";

    private Context mContext;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefsEditor;

    public ContactsPhotosNotificationHandler(Context context) {
        mContext = context;
        mPrefs = mContext.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        mPrefsEditor = mPrefs.edit();
    }

    @Override
    public void startTask(OAuth oauth) {
        new LoadContactsPhotosTask(this).execute(oauth);
    }

    @Override
    public boolean enabledInPreferences() {
        SharedPreferences defaultSharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(mContext);
        return defaultSharedPrefs.getBoolean(
                Constants.KEY_ENABLE_CONTACTS_NOTIFICATIONS, false);
    }

    /**
     * Once photos are ready check for new ones and notify the user.
     *
     * There are two conditions that must be satisfied for a notification to be
     * shown:
     * 1) The id must be newer than ones previously shown in the main app.
     * 2) The id must not equal the one previously notified about, to avoid
     *    duplicate notifications.
     */
    @Override
    public void onPhotosReady(List<Photo> photos) {
        if (Constants.DEBUG) Log.d(TAG, "onPhotosReady");
        if (photos != null) {
            List<Photo> newPhotos = checkForNewPhotos(photos);
            if (newPhotos != null && !newPhotos.isEmpty()) {
                String latestIdNotifiedAbout = getLatestIdNotifiedAbout();
                Photo latestPhoto = newPhotos.get(0);
                if (!latestIdNotifiedAbout.equals(latestPhoto.getId())) {
                    showNewPhotosNotification(newPhotos);
                    storeLatestIdNotifiedAbout(latestPhoto);
                }
            }
        } else {
            Log.e(TAG, "onPhotosReady: null photolist received");
        }
    }

    /** Notify that user's contacts have uploaded new photos */
    public void showNewPhotosNotification(List<Photo> newPhotos) {
        final NotificationManager mgr = (NotificationManager)
            mContext.getSystemService(
                    WakefulIntentService.NOTIFICATION_SERVICE);
        String tickerText =
            mContext.getString(R.string.notification_contacts_ticker);
        String titleText = String.format("%d %s", newPhotos.size(),
                mContext.getString(R.string.notification_contacts_title));
        String contentText =
            mContext.getString(R.string.notification_contacts_content);
        Notification newContactsPhotos = getNotification(tickerText, titleText,
                contentText, newPhotos.size());
        mgr.notify(Constants.NOTIFICATION_NEW_CONTACTS_PHOTOS,
                newContactsPhotos);
    }

    /**
     * Check if the most recent photo id we have stored is present in the
     * list of photos passed in.  If so, new photos are the sublist from 0
     * to the found id.
     */
    private List<Photo> checkForNewPhotos(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            if (Constants.DEBUG) {
                Log.d(TAG, "checkForNewPhotos: photos null or empty");
            }
            return Collections.EMPTY_LIST;
        }

        List<Photo> newPhotos = new ArrayList<Photo>();
        String newestId = getLatestViewedId();
        for (int i=0; i < photos.size(); i++) {
            Photo p = photos.get(i);
            if (p.getId().equals(newestId)) {
                newPhotos = photos.subList(0, i);
                break;
            }
        }
        if (Constants.DEBUG) {
            Log.d(TAG, String.format("Found %d new photos", newPhotos.size()));
        }
        return newPhotos;
    }

    /**
     * Returns the id the of the most recent photo the user has viewed.
     */
    private String getLatestViewedId() {
        String newestId =
            mPrefs.getString(Constants.NEWEST_CONTACT_PHOTO_ID, "");
        if (Constants.DEBUG) {
            Log.d(TAG, "getLatestViewedId: " + newestId);
        }
        return newestId;
    }

    /**
     * Returns the id the of the most recent photo in the list of photo's we've
     * notified about.
     */
    @Override
    public String getLatestIdNotifiedAbout() {
        String newestId = mPrefs.getString(
                Constants.NOTIFICATION_NEWEST_CONTACT_PHOTO_ID, "");
        if (Constants.DEBUG) {
            Log.d(TAG, "getLatestIdNotifiedAbout: " + newestId);
        }
        return newestId;
    }

    @Override
    public void storeLatestIdNotifiedAbout(Photo photo) {
        mPrefsEditor.putString(Constants.NOTIFICATION_NEWEST_CONTACT_PHOTO_ID,
                photo.getId());
        mPrefsEditor.commit();
        if (Constants.DEBUG) {
            Log.d(TAG, "Updated most recent contact photo id to " +
                    photo.getId() + " (notification)");
        }
    }

    private Notification getNotification(final String tickerText,
            final String titleText, final String contentText, int number) {
        return new NotificationCompat2.Builder(mContext)
            .setSmallIcon(R.drawable.ic_action_social_group_dark)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setNumber(number)
            .setTicker(tickerText)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(getPendingIntent())
            .build();
    }

    /**
     * Passed to NotificationCompat2.Builder.setContentIntent to start
     * MainActivity when the notification is pressed.
     */
    private PendingIntent getPendingIntent() {
        Intent i = new Intent(mContext, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(mContext, 0, i, 0);
    }
}
