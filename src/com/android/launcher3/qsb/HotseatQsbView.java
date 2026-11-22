/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3.qsb;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.app.animation.Interpolators;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.util.HorizontalInsettableView;

/**
 * Pixel-style Hotseat Quick Search Bar view.
 */
public class HotseatQsbView extends FrameLayout implements HorizontalInsettableView, View.OnClickListener {

    private static final String GOOGLE_SEARCHBOX_PKG = "com.google.android.googlequicksearchbox";
    private static final String GOOGLE_LENS_PKG = "com.google.ar.lens";
    private static final String GOOGLE_LENS_ACTIVITY = "com.google.android.apps.search.lens.LensActivity";

    private float mHorizontalInsets = 0f;

    public HotseatQsbView(@NonNull Context context) {
        this(context, null);
    }

    public HotseatQsbView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HotseatQsbView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setOnClickListener(this);

        View inner = findViewById(R.id.hotseat_qsb_inner);
        if (inner != null) {
            inner.setOnClickListener(this);
        }

        View gIcon = findViewById(R.id.qsb_g_icon);
        if (gIcon != null) {
            gIcon.setOnClickListener(this);
        }

        View hint = findViewById(R.id.qsb_search_hint);
        if (hint != null) {
            hint.setOnClickListener(this);
        }

        View mic = findViewById(R.id.qsb_mic_icon);
        if (mic != null) {
            mic.setOnClickListener(this);
        }

        View lens = findViewById(R.id.qsb_lens_icon);
        if (lens != null) {
            lens.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.qsb_mic_icon) {
            launchVoiceSearch();
        } else if (id == R.id.qsb_lens_icon) {
            launchLens();
        } else {
            launchSearch();
        }
    }

    private void launchSearch() {
        Context context = getContext();
        View inner = findViewById(R.id.hotseat_qsb_inner);
        View animTarget = inner != null ? inner : this;
        animTarget.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        animTarget.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .withEndAction(() -> {
                    animTarget.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(140)
                            .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                            .withEndAction(() -> animTarget.setLayerType(View.LAYER_TYPE_NONE, null))
                            .start();
                }).start();

        Bundle opts = null;
        try {
            Launcher launcher = Launcher.getLauncher(context);
            if (launcher != null) {
                opts = launcher.getActivityLaunchOptions(this, null).toBundle();
            }
        } catch (Exception ignored) {}

        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isPackageInstalled(GOOGLE_SEARCHBOX_PKG)) {
            intent.setPackage(GOOGLE_SEARCHBOX_PKG);
        }
        try {
            context.startActivity(intent, opts);
        } catch (Exception e) {
            try {
                Launcher launcher = Launcher.getLauncher(context);
                if (launcher != null) {
                    launcher.startSearch("", false, null, true);
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent, opts);
            } catch (Exception ignored) {
            }
        }
    }

    private void launchVoiceSearch() {
        Context context = getContext();
        View mic = findViewById(R.id.qsb_mic_icon);
        if (mic != null) {
            mic.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mic.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(90)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .withEndAction(() -> {
                        mic.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(140)
                                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                                .withEndAction(() -> mic.setLayerType(View.LAYER_TYPE_NONE, null))
                                .start();
                    }).start();
        }

        Bundle opts = null;
        try {
            Launcher launcher = Launcher.getLauncher(context);
            if (launcher != null) {
                opts = launcher.getActivityLaunchOptions(mic != null ? mic : this, null).toBundle();
            }
        } catch (Exception ignored) {}

        Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (isPackageInstalled(GOOGLE_SEARCHBOX_PKG)) {
            intent.setPackage(GOOGLE_SEARCHBOX_PKG);
        }
        try {
            context.startActivity(intent, opts);
        } catch (Exception e) {
            launchSearch();
        }
    }

    private void launchLens() {
        Context context = getContext();
        View lens = findViewById(R.id.qsb_lens_icon);
        if (lens != null) {
            lens.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            lens.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(90)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .withEndAction(() -> {
                        lens.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(140)
                                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                                .withEndAction(() -> lens.setLayerType(View.LAYER_TYPE_NONE, null))
                                .start();
                    }).start();
        }

        Bundle opts = null;
        try {
            Launcher launcher = Launcher.getLauncher(context);
            if (launcher != null) {
                opts = launcher.getActivityLaunchOptions(lens != null ? lens : this, null).toBundle();
            }
        } catch (Exception ignored) {}

        // 1. Try standard Google Lens uri: googleapp://lens targeted at Google Search app
        try {
            Intent lensUriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lensUriIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 2. Try googleapp://lens without package restriction
        try {
            Intent lensUriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lensUriIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 3. Try standalone Lens app (com.google.ar.lens)
        try {
            Intent lensIntent = context.getPackageManager().getLaunchIntentForPackage(GOOGLE_LENS_PKG);
            if (lensIntent != null) {
                lensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lensIntent, opts);
                return;
            }
        } catch (Exception ignored) {}

        // 4. Fallback to general search
        launchSearch();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void setHorizontalInsets(float insetPercentage) {
        mHorizontalInsets = insetPercentage;
        int insetPx = (int) (getWidth() * insetPercentage);
        setPadding(insetPx, getPaddingTop(), insetPx, getPaddingBottom());
    }

    @Override
    public float getHorizontalInsets() {
        return mHorizontalInsets;
    }
}
