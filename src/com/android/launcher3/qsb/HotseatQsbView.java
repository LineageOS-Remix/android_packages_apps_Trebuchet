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
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.ActivityOptionsWrapper;
import com.android.launcher3.util.HorizontalInsettableView;

/**
 * Pixel-style Hotseat Quick Search Bar view.
 */
public class HotseatQsbView extends FrameLayout implements HorizontalInsettableView, View.OnClickListener {

    private static final String GOOGLE_SEARCHBOX_PKG = "com.google.android.googlequicksearchbox";
    private static final String GOOGLE_LENS_PKG = "com.google.ar.lens";

    private float mHorizontalInsets = 0f;
    private AppWidgetHostView mHiddenGoogleWidgetView;

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
            ItemInfo qsbInfo = new ItemInfo();
            qsbInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SEARCH_ACTION;
            qsbInfo.container = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
            inner.setTag(qsbInfo);
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
                ItemInfo info = (v != null && v.getTag() instanceof ItemInfo) ? (ItemInfo) v.getTag() : null;
                ActivityOptionsWrapper wrapper = launcher.getActivityLaunchOptions(v, info);
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
    private View getHiddenGoogleWidgetView() {
        if (mHiddenGoogleWidgetView != null) {
            return mHiddenGoogleWidgetView;
        }
        try {
            Context context = getContext();
            AppWidgetManager wm = AppWidgetManager.getInstance(context);
            int widgetId = LauncherPrefs.getPrefs(context).getInt("qsb_widget_id", -1);
            if (widgetId == -1) {
                ComponentName provider = new ComponentName(GOOGLE_SEARCHBOX_PKG,
                        "com.google.android.googlequicksearchbox.SearchWidgetProvider");
                int[] widgetIds = wm.getAppWidgetIds(provider);
                if (widgetIds != null && widgetIds.length > 0) {
                    widgetId = widgetIds[0];
                }
            }
            if (widgetId != -1) {
                AppWidgetProviderInfo info = wm.getAppWidgetInfo(widgetId);
                if (info != null && info.provider.getPackageName().equals(GOOGLE_SEARCHBOX_PKG)) {
                    AppWidgetHost host = new AppWidgetHost(context, QsbContainerView.QsbFragment.QSB_WIDGET_HOST_ID);
                    mHiddenGoogleWidgetView = host.createView(context, widgetId, info);
                    int width = context.getResources().getDisplayMetrics().widthPixels;
                    int height = (int) (60 * context.getResources().getDisplayMetrics().density);
                    mHiddenGoogleWidgetView.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    mHiddenGoogleWidgetView.layout(0, 0, width, height);
                }
            }
        } catch (Exception ignored) {}
        return mHiddenGoogleWidgetView;
    }

    @Nullable
    private View findGoogleWidgetButton(String... buttonEntryNames) {
        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            if (launcher == null) {
                return null;
            }
            Context context = getContext();

            // 1. Check workspace items if widget was placed by user
            if (launcher.getWorkspace() != null) {
                final View[] result = new View[1];
                launcher.getWorkspace().mapOverItems((info, v) -> {
                    if (result[0] != null) return true;
                    if (v instanceof ViewGroup vg) {
                        for (String entryName : buttonEntryNames) {
                            View found = findViewByEntryName(vg, entryName);
                            if (found != null) {
                                result[0] = found;
                                return true;
                            }
                        }
                    }
                    return false;
                });
                if (result[0] != null) {
                    return result[0];
                }
            }

            // 2. Check QsbFragment bound widget view
            View boundQsb = QsbContainerView.QsbFragment.getBoundQsbWidgetView();
            if (boundQsb instanceof ViewGroup vg) {
                if (vg.getWidth() == 0 || vg.getHeight() == 0) {
                    int width = context.getResources().getDisplayMetrics().widthPixels;
                    int height = (int) (60 * context.getResources().getDisplayMetrics().density);
                    vg.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    vg.layout(0, 0, width, height);
                }
                for (String entryName : buttonEntryNames) {
                    View found = findViewByEntryName(vg, entryName);
                    if (found != null) {
                        return found;
                    }
                }
            }

            // 3. Check cached QSB widget host view
            View hidden = getHiddenGoogleWidgetView();
            if (hidden instanceof ViewGroup vg) {
                for (String entryName : buttonEntryNames) {
                    View found = findViewByEntryName(vg, entryName);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private View findViewByEntryName(View root, String entryName) {
        if (root == null) return null;
        try {
            if (root.getId() != View.NO_ID && root.getResources() != null) {
                String name = root.getResources().getResourceEntryName(root.getId());
                if (entryName.equals(name)) return root;
            }
        } catch (Exception ignored) {}
        try {
            Context context = getContext();
            Resources gsaRes = context.getPackageManager().getResourcesForApplication(GOOGLE_SEARCHBOX_PKG);
            int id = gsaRes.getIdentifier(entryName, "id", GOOGLE_SEARCHBOX_PKG);
            if (id != 0 && root.getId() == id) {
                return root;
            }
        } catch (Exception ignored) {}
        if (root instanceof ViewGroup vg) {
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = findViewByEntryName(vg.getChildAt(i), entryName);
                if (child != null) return child;
            }
        }
        return null;
    }

    private void launchSearch() {
        Context context = getContext();
        Launcher launcher = Launcher.getLauncher(context);
        if (launcher != null) {
            launcher.setLastLaunchedFromQsb(true);
        }
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

        // 3. Fallback: If Google Search widget is available, delegate to its search button
        View widgetSearchBtn = findGoogleWidgetButton(
                "googleapp_search_widget_ghost_text_search",
                "googleapp_search_widget_ghost_google_logo",
                "googleapp_search_widget_search_btn");
        if (widgetSearchBtn != null) {
            widgetSearchBtn.performClick();
            return;
        }

        // 4. Fallback to browser search
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent, opts);
        } catch (Exception ignored) {}
    }

    private void launchVoiceSearch() {
        Context context = getContext();
        Launcher launcher = Launcher.getLauncher(context);
        if (launcher != null) {
            launcher.setLastLaunchedFromQsb(true);
        }
        View inner = findViewById(R.id.hotseat_qsb_inner);
        View mic = findViewById(R.id.qsb_mic_icon);
        View target = inner != null ? inner : (mic != null ? mic : this);
        if (mic != null) {
            animateClick(mic);
        } else {
            animateClick(target);
        }

        Bundle opts = getLaunchOptions(target).toBundle();

        // 1. Try Google Voice Assist entrypoint (verified working on device)
        try {
            Intent intent = new Intent("android.intent.action.VOICE_ASSIST")
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent, opts);
            return;
        } catch (Exception ignored) {}

        // 2. Try speech web search
        try {
            Intent intent = new Intent("android.speech.action.WEB_SEARCH")
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent, opts);
            return;
        } catch (Exception ignored) {}

        // 3. Fallback: If Google Search widget is available, delegate to its mic button
        View widgetMicBtn = findGoogleWidgetButton(
                "googleapp_search_widget_ghost_voice_search",
                "googleapp_search_widget_voice_btn",
                "googleapp_search_widget_mic_btn");
        if (widgetMicBtn != null) {
            widgetMicBtn.performClick();
            return;
        }

        // 4. Try generic voice assist
        try {
            Intent intent = new Intent("android.intent.action.VOICE_ASSIST")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent, opts);
            return;
        } catch (Exception ignored) {}

        launchSearch();
    }

    private void launchGemini() {
        Context context = getContext();
        Launcher launcher = Launcher.getLauncher(context);
        if (launcher != null) {
            launcher.setLastLaunchedFromQsb(true);
        }
        View gemini = findViewById(R.id.qsb_gemini_icon);
        View target = gemini != null ? gemini : this;
        animateClick(target);

        // 1. If Google Search widget is on workspace, click its AI Mode button
        View widgetAiBtn = findGoogleWidgetButton(
                "googleapp_search_widget_ghost_shortcut",
                "googleapp_search_widget_shortcut_btn");
        if (widgetAiBtn != null) {
            widgetAiBtn.performClick();
            return;
        }

        Bundle opts = getLaunchOptions(target).toBundle();

        // 2. Launch Local Gemini / AI Mode in Google Search App directly
        try {
            Intent aiIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("ga:/data?tab_type=21&go_back_to_discover_feed_from_srp=false&go_back_to_inbox_from_srp=false&aim_options=CAIYACAAQABIAFAAWAA%3D"))
                    .setComponent(new ComponentName(GOOGLE_SEARCHBOX_PKG, "com.google.android.googlequicksearchbox.SearchActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(aiIntent, opts);
            return;
        } catch (Exception ignored) {}

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
        Launcher launcher = Launcher.getLauncher(context);
        if (launcher != null) {
            launcher.setLastLaunchedFromQsb(true);
        }
        View lens = findViewById(R.id.qsb_lens_icon);
        View target = lens != null ? lens : this;
        animateClick(target);

        // 1. If Google Search widget is on workspace or in host, click its Lens button
        View widgetLensBtn = findGoogleWidgetButton(
                "googleapp_search_widget_ghost_lens",
                "googleapp_search_widget_lens_btn");
        if (widgetLensBtn != null) {
            widgetLensBtn.performClick();
            return;
        }

        Launcher launcher = Launcher.getLauncher(context);

        // 2. Launch via Launcher.startActivityForResultDirect (preserves com.android.launcher3 as callingPackage)
        if (launcher != null) {
            try {
                Intent lensIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google://lens"))
                        .setPackage(GOOGLE_SEARCHBOX_PKG);
                lensIntent.putExtra("LensHomescreenShortcut", true);
                launcher.startActivityForResultDirect(lensIntent, 1027, null);
                return;
            } catch (Exception ignored) {}

            try {
                Intent lensIntent = new Intent();
                lensIntent.setComponent(new ComponentName(GOOGLE_SEARCHBOX_PKG,
                        "com.google.android.apps.lens.MainActivity"));
                lensIntent.setData(Uri.parse("google://lens"));
                lensIntent.putExtra("LensHomescreenShortcut", true);
                launcher.startActivityForResultDirect(lensIntent, 1027, null);
                return;
            } catch (Exception ignored) {}

            try {
                Intent lensIntent = new Intent();
                lensIntent.setComponent(new ComponentName(GOOGLE_SEARCHBOX_PKG,
                        "com.google.android.apps.search.lens.LensExportedActivity"));
                lensIntent.setData(Uri.parse("google://lens"));
                lensIntent.putExtra("LensHomescreenShortcut", true);
                launcher.startActivityForResultDirect(lensIntent, 1027, null);
                return;
            } catch (Exception ignored) {}
        }

        // 3. Standalone Lens app (com.google.ar.lens)
        try {
            Intent lensIntent = context.getPackageManager().getLaunchIntentForPackage(GOOGLE_LENS_PKG);
            if (lensIntent != null) {
                lensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lensIntent);
                return;
            }
        } catch (Exception ignored) {}

        // 4. Try googleapp://lens and https://lens.google/trylens deeplinks
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        } catch (Exception ignored) {}

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google/trylens"))
                    .setPackage(GOOGLE_SEARCHBOX_PKG)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
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
