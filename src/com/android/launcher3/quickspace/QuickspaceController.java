/*
 * Copyright (C) 2018 CypherOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.quickspace;

import android.annotation.NonNull;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.PackageUserKey;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;

public class QuickspaceController {

    public final ArrayList<OnDataListener> mListeners = new ArrayList();
    private static final boolean DEBUG = false;
    private static final String TAG = "Launcher3:QuickspaceController";

    private final Context mContext;
    private final Handler mHandler;
    private QuickEventsController mEventsController;

    private MediaController mController;
    private MediaMetadata mMediaMetadata;
    private String mLastTrackTitle = null;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Runnable mOnDataUpdatedRunnable = new Runnable() {
            @Override
            public void run() {
                for (OnDataListener list : mListeners) {
                    list.onDataUpdated();
                }
            }
        };

    public interface OnDataListener {
        void onDataUpdated();
    }

    private final MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            updateMediaController();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            updateMediaController();
        }
    };

    public QuickspaceController(Context context) {
        mContext = context;
        mHandler = new Handler();
        mEventsController = new QuickEventsController(context);
    }

    public void addListener(OnDataListener listener) {
        mListeners.add(listener);
        listener.onDataUpdated();
    }

    private void removeListener(OnDataListener listener) {
        mListeners.remove(listener);
    }

    public boolean isQuickEvent() {
        return mEventsController.isQuickEvent();
    }

    public QuickEventsController getEventController() {
        return mEventsController;
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    public void onPause() {
        cancelListeners();
    }

    public void onResume() {
        maybeInitExecutor();
        mEventsController.onResume();
        updateMediaController();
        notifyListeners();
    }

    private void maybeInitExecutor() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    private void cancelListeners() {
        if (mEventsController != null) {
            mEventsController.onPause();
        }
        for (OnDataListener listener : new ArrayList<>(mListeners)) {
            removeListener(listener);
        }
        unregisterMediaController();
        mHandler.removeCallbacksAndMessages(null);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }

    public void onDestroy() {
        cancelListeners();
        mMediaMetadata = null;
        executorService = null;
    }

    public void notifyListeners() {
        mHandler.post(mOnDataUpdatedRunnable);
    }

    private MediaController getActiveLocalMediaController() {
        MediaSessionManager mediaSessionManager =
                mContext.getSystemService(MediaSessionManager.class);
        MediaController localController = null;
        final List<String> remoteMediaSessionLists = new ArrayList<>();
        for (MediaController controller : mediaSessionManager.getActiveSessions(null)) {
            final MediaController.PlaybackInfo pi = controller.getPlaybackInfo();
            if (pi == null) continue;
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState == null) continue;
            if (playbackState.getState() != PlaybackState.STATE_PLAYING) continue;
            if (pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                if (localController != null
                        && TextUtils.equals(
                                localController.getPackageName(), controller.getPackageName())) {
                    localController = null;
                }
                if (!remoteMediaSessionLists.contains(controller.getPackageName())) {
                    remoteMediaSessionLists.add(controller.getPackageName());
                }
                continue;
            }
            if (pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL) {
                if (localController == null
                        && !remoteMediaSessionLists.contains(controller.getPackageName())) {
                    localController = controller;
                }
            }
        }
        return localController;
    }

    private void registerMediaController() {
        MediaController localController = getActiveLocalMediaController();
        if (localController != null && (mController == null || !sameSessions(mController, localController))) {
            unregisterMediaController();
            mController = localController;
            mController.registerCallback(mMediaCallback);
        }
    }

    private void unregisterMediaController() {
        if (mController != null) {
            mController.unregisterCallback(mMediaCallback);
            mController = null;
        }
    }

    private void updateMediaController() {
        if (!Utilities.isQuickspaceNowPlaying(mContext)) {
            unregisterMediaController();
            return;
        }
        registerMediaController();
        if (mController != null) {
            mMediaMetadata = mController.getMetadata();
        }
        boolean isPlaying = PlaybackState.STATE_PLAYING == getMediaControllerPlaybackState(mController);
        String trackArtist = isPlaying && mMediaMetadata != null ? mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) : "";
        String trackTitle = isPlaying && mMediaMetadata != null ? mMediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) : "";
        mEventsController.setMediaInfo(trackTitle, trackArtist, isPlaying);
        mEventsController.updateQuickEvents();
        notifyListeners();
    }

    private boolean sameSessions(MediaController a, MediaController b) {
        if (a == b) return true;
        if (a == null) return false;
        return a.controlsSameSession(b);
    }
}
