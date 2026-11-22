/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.widget.NavigableAppWidgetHostView;

/**
 * Appwidget host view with QSB specific logic.
 */
public class QsbWidgetHostView extends NavigableAppWidgetHostView {

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mPreviousOrientation;

    public QsbWidgetHostView(Context context) {
        super(context);
        setFocusable(true);
        setBackgroundResource(R.drawable.qsb_host_view_focus_bg);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        // Store the orientation in which the widget was inflated
        mPreviousOrientation = getResources().getConfiguration().orientation;
        super.updateAppWidget(remoteViews);
    }


    public boolean isReinflateRequired(int orientation) {
        // Re-inflate is required if the orientation has changed since last inflation.
        return mPreviousOrientation != orientation;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // Prevent the base class from applying the default widget padding.
        super.setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        try {
            super.onLayout(changed, left, top, right, bottom);
        } catch (final RuntimeException e) {
            // Update the widget with 0 Layout id, to reset the view to error view.
            post(() -> updateAppWidget(
                    new RemoteViews(getAppWidgetInfo().provider.getPackageName(), 0)));
        }
    }

    @Override
    protected View getErrorView() {
        return getDefaultView(this);
    }

    @Override
    protected View getDefaultView() {
        View v = super.getDefaultView();
        v.setOnClickListener((v2) ->
                Launcher.getLauncher(getContext()).startSearch("", false, null, true));
        return v;
    }

    public static View getDefaultView(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.qsb_default_view, parent, false);
        View searchBtn = v.findViewById(R.id.btn_qsb_search);
        if (searchBtn != null) {
            searchBtn.setOnClickListener((v2) ->
                    Launcher.getLauncher(v2.getContext()).startSearch("", false, null, true));
        }
        View dateView = v.findViewById(R.id.smartspace_date);
        if (dateView != null) {
            dateView.setOnClickListener((v2) -> {
                Context c = v2.getContext();
                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CALENDAR)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    c.startActivity(intent);
                } catch (Exception e) {
                    try {
                        c.startActivity(new Intent(Intent.ACTION_VIEW)
                                .setData(android.net.Uri.parse("content://com.android.calendar/time"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception ignored) {}
                }
            });
        }
        return v;
    }

    @Override
    protected boolean shouldAllowDirectClick() {
        return true;
    }
}
