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

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.window.SplashScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.app.animation.Interpolators;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.util.ActivityOptionsWrapper;
import com.android.launcher3.util.HorizontalInsettableView;

/**
 * Pixel-style Hotseat Quick Search Bar view.
 */
public class HotseatQsbView extends FrameLayout implements HorizontalInsettableView, View.OnClickListener {

    private static final String GOOGLE_SEARCHBOX_PKG = "com.google.android.googlequicksearchbox";
    private static final String GOOGLE_LENS_PKG = "com.google.ar.lens";

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

        View gemini = findViewById(R.id.qsb_gemini_icon);
        if (gemini != null) {
            gemini.setOnClickListener(this);
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
        } else if (id == R.id.qsb_gemini_icon) {
            launchGemini();
        } else {
            launchSearch();
        }
    }

    private ActivityOptions getLaunchOptions(View v) {
        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            if (launcher != null) {
                ActivityOptionsWrapper wrapper = launcher.getActivityLaunchOptions(v, null);
                if (wrapper != null && wrapper.options != null) {
                    wrapper.options.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR);
                    wrapper.options.setPendingIntentLaunchFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    wrapper.options.setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                    return wrapper.options;
                }
            }
        } catch (Exception ignored) {}
        return ActivityOptions.makeBasic();
    }

    private void animateClick(View view) {
        if (view == null) return;
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        view.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(90)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(140)
                            .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                            .withEndAction(() -> view.setLayerType(View.LAYER_TYPE_NONE, null))
                            .start();
                }).start();
    }

    @Nullable
    private View findGoogleWidgetButton(String buttonEntryName) {
        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            if (launcher == null || launcher.getWorkspace() == null) {
                return null;
            }
            Context context = getContext();
            Resources gsaRes = context.getPackageManager().getResourcesForApplication(GOOGLE_SEARCHBOX_PKG);
            int targetId = gsaRes.getIdentifier(buttonEntryName, "id", GOOGLE_SEARCHBOX_PKG);
            if (targetId == 0) {
                return null;
            }
            final View[] result = new View[1];
            launcher.getWorkspace().mapOverItems((info, v) -> {
                if (result[0] != null) return true;
                if (v instanceof ViewGroup) {
                    View found = v.findViewById(targetId);
                    if (found != null) {
                        result[0] = found;
                        return true;
                    }
                }
                return false;
            });
            return result[0];
        } catch (Exception ignored) {
            return null;
        }
    }

    private void launchSearch() {
        Context context = getContext();
        View inner = findViewById(R.id.hotseat_qsb_inner);
        View target = inner != null ? inner : this;
        animateClick(target);

        Bundle opts = getLaunchOptions(target).toBundle();

        // 1. Try launching Google Search directly with ACTION_WEB_SEARCH targeted to Google app
        try {
            Intent googleSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH)
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(googleSearchIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 2. Try generic ACTION_WEB_SEARCH
        try {
            Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(searchIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 3. Fallback to browser search
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent, opts);
        } catch (Exception ignored) {}
    }

    private void launchVoiceSearch() {
        Context context = getContext();
        View mic = findViewById(R.id.qsb_mic_icon);
        View target = mic != null ? mic : this;
        animateClick(target);

        Bundle opts = getLaunchOptions(target).toBundle();

        // 1. Try voice search with Google package
        Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND)
                .setPackage(GOOGLE_SEARCHBOX_PKG)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            intent = new Intent("android.speech.action.WEB_SEARCH")
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            intent = new Intent(Intent.ACTION_VOICE_COMMAND)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent, opts);
        } catch (Exception e) {
            launchSearch();
        }
    }

    private void launchGemini() {
        Context context = getContext();
        View gemini = findViewById(R.id.qsb_gemini_icon);
        View target = gemini != null ? gemini : this;
        animateClick(target);

        Bundle opts = getLaunchOptions(target).toBundle();

        // 1. Launch Local Gemini / AI Mode in Google Search App directly
        try {
            Intent aiIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("ga:/data?tab_type=21&go_back_to_discover_feed_from_srp=false&go_back_to_inbox_from_srp=false&aim_options=CAIYACAAQABIAFAAWAA%3D"))
                    .setComponent(new ComponentName(GOOGLE_SEARCHBOX_PKG, "com.google.android.googlequicksearchbox.SearchActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(aiIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 2. If Google Search widget is on workspace, click its AI Mode button
        View widgetAiBtn = findGoogleWidgetButton("googleapp_search_widget_shortcut_btn");
        if (widgetAiBtn != null) {
            widgetAiBtn.performClick();
            return;
        }

        // 3. Try Gemini Gateway URL in Google Search App
        try {
            Intent geminiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(geminiIntent, opts);
            return;
        } catch (Exception ignored) {}

        launchSearch();
    }

    private void launchLens() {
        Context context = getContext();
        View lens = findViewById(R.id.qsb_lens_icon);
        View target = lens != null ? lens : this;
        animateClick(target);

        // 1. If Google Search widget is on workspace, click its Lens button
        View widgetLensBtn = findGoogleWidgetButton("googleapp_search_widget_lens_btn");
        if (widgetLensBtn != null) {
            widgetLensBtn.performClick();
            return;
        }

        Bundle opts = getLaunchOptions(target).toBundle();

        // 2. Try googleapp://lens deep-link
        try {
            Intent lensIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lensIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 3. Try https://lens.google/trylens deep-link
        try {
            Intent lensIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google/trylens"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lensIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 4. Try LensExportedActivity directly
        try {
            Intent lensIntent = new Intent()
                    .setComponent(new ComponentName(GOOGLE_SEARCHBOX_PKG,
                            "com.google.android.apps.search.lens.LensExportedActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lensIntent, opts);
            return;
        } catch (Exception ignored) {}

        // 5. Standalone Lens app (com.google.ar.lens)
        try {
            Intent lensIntent = context.getPackageManager().getLaunchIntentForPackage(GOOGLE_LENS_PKG);
            if (lensIntent != null) {
                lensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lensIntent, opts);
                return;
            }
        } catch (Exception ignored) {}

        launchSearch();
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
