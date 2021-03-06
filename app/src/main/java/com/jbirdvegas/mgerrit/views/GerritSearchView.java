package com.jbirdvegas.mgerrit.views;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;

import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.helpers.AnalyticsHelper;
import com.jbirdvegas.mgerrit.message.SearchQueryChanged;
import com.jbirdvegas.mgerrit.message.SearchStateChanged;
import com.jbirdvegas.mgerrit.search.OwnerSearch;
import com.jbirdvegas.mgerrit.search.ProjectSearch;
import com.jbirdvegas.mgerrit.search.SearchKeyword;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class GerritSearchView extends SearchView
        implements SearchView.OnQueryTextListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GerrritSearchView";
    private final SharedPreferences mPrefs;
    private final EventBus mEventBus;
    Context mContext;

    Set<SearchKeyword> mAdditionalKeywords;

    // The list of keyword tokens for the last processed query
    Set<SearchKeyword> mCurrentKeywords;

    public GerritSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnQueryTextListener(this);
        setupCancelButton();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEventBus = EventBus.getDefault();

        mCurrentKeywords = new HashSet<>();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        Integer user = PrefsFragment.getTrackingUser(mContext);
        if (user != null) {
            injectKeyword(new OwnerSearch(user.toString()));
        }

        String project = PrefsFragment.getCurrentProject(mContext);
        if (!project.isEmpty()) {
            injectKeyword(new ProjectSearch(project));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Set<SearchKeyword> tokens = constructTokens(query);

        if (mAdditionalKeywords != null) updatePreferences();

        // Pass this on to the current CardsFragment instance
        if (!processTokens(tokens)) {
            Log.w(TAG, "Could not process query: " + query);
        }

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Handled when the search is submitted instead.
        if (newText.isEmpty()) {
            onQueryTextSubmit(null);
        } else setVisibility(VISIBLE);
        return false;
    }

    /**
     * Set the search query. This will construct the SQL query and restart
     * the loader to perform the query
     *
     * @param query The search query text
     */
    private Set<SearchKeyword> constructTokens(@Nullable String query) {
        // An empty query will result in an empty set
        if (query == null || query.isEmpty()) {
            return new HashSet<>();
        }

        return SearchKeyword.constructTokens(query);
    }

    private boolean processTokens(final Set<SearchKeyword> tokens) {
        Set<SearchKeyword> newTokens = safeMerge(tokens, mAdditionalKeywords);
        mCurrentKeywords = newTokens;

        String where = "";
        ArrayList<String> bindArgs = new ArrayList<>();

        if (newTokens != null && !newTokens.isEmpty()) {
            where = SearchKeyword.constructDbSearchQuery(newTokens);
            if (where != null && !where.isEmpty()) {
                for (SearchKeyword token : newTokens) {
                    bindArgs.addAll(Arrays.asList(token.getEscapeArgument()));
                }
            } else {
                return false;
            }
        }

        /* In case we change the search query without showing the search view post a new event
         * for the CardsFragment to show the Refine Search card */
        mEventBus.post(new SearchStateChanged(getVisibility() == VISIBLE));

        mEventBus.postSticky(new SearchQueryChanged(where, bindArgs,
                getContext().getClass().getSimpleName(), tokens));
        return true;
    }

    /**
     * Always show the cancel button and set its onClick listener. The button
     * has private visibility so we need reflection to access it.
     */
    private void setupCancelButton() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeBtn = (ImageView) searchField.get(this);
            closeBtn.setVisibility(VISIBLE);
            closeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleVisibility();
                }
            });
        } catch (Exception e) {
            AnalyticsHelper.getInstance().sendAnalyticsEvent(mContext,
                    "GerritSearchView",
                    "setupCancelButton",
                    "search_button_reflection_visibility",
                    null);
            e.printStackTrace();
        }
    }

    public void toggleVisibility() {
        int visibility = getVisibility();
        if (visibility == View.GONE) {
            setVisibility(View.VISIBLE);
            requestFocus();
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        String query = getQuery().toString();
        if (!query.isEmpty() && visibility == GONE) setQuery("", true);
        super.setVisibility(visibility);
        setIconified(visibility == GONE);
        mEventBus.post(new SearchStateChanged(visibility == VISIBLE));

    }

    /**
     * Modifies future searches for this fragment by appending additional
     * keywords to search for that will not be present in the original
     * search query. This clears all old keywords that were previously injected.
     *
     * @param keywords
     */
    public void injectKeywords(Collection<SearchKeyword> keywords) {
        if (keywords == null && mAdditionalKeywords != null) {
            mAdditionalKeywords.clear();
        } else if (keywords != null) {
            mAdditionalKeywords = new HashSet<>(keywords);
        } // We can leave mAdditionalKeywords unset as we will only use it if it is non-null

        onQueryTextSubmit(getQuery().toString()); // Force search refresh
    }

    /**
     * Modifies future searches for this fragment by appending additional
     * keywords to search for that will not be present in the original
     * search query.
     *
     * @param keyword
     */
    public void injectKeyword(@NotNull SearchKeyword keyword) {
        if (mAdditionalKeywords == null) mAdditionalKeywords = new HashSet<>();

        if (!keyword.isValid()) {
            SearchKeyword.replaceKeyword(mAdditionalKeywords, keyword);
        } else {
            mAdditionalKeywords.add(keyword);
        }

        onQueryTextSubmit(getQuery().toString()); // Force search refresh
    }

    /**
     * Add the elements of otherSet to oldSet and return a new set.
     */
    private Set<SearchKeyword> safeMerge(Set<SearchKeyword> oldSet, Set<SearchKeyword> otherSet) {
        HashSet<SearchKeyword> newSet = new HashSet<>();
        if (oldSet != null && !oldSet.isEmpty()) {
            newSet.addAll(oldSet);
        }
        if (otherSet != null && !otherSet.isEmpty()) {
            newSet.addAll(otherSet);
        }
        return newSet;
    }

    /**
     * @return The list of search keywords that were included in the query plus any additional
     * keywords that were set via injectKeywords(Set<SearchKeyword>)
     */
    public Set<SearchKeyword> getLastQuery() {
        return mCurrentKeywords;
    }

    /**
     * Search for a given search keyword in the current list of tokens
     *
     * @param keyword The search keyword to search for (needle)
     * @return Whether the keyword was found in the list or not
     */
    public boolean hasKeyword(SearchKeyword keyword) {
        return SearchKeyword.findKeyword(mCurrentKeywords, keyword) != -1;
    }

    /**
     * @return The number of refine search filters (SearchKeywords) already active
     */
    public int getFilterCount() {
        return mAdditionalKeywords == null ? 0 : mAdditionalKeywords.size();
    }

    private void updatePreferences() {
        // If there is no project keyword in the query, it should be cleared
        if (SearchKeyword.findKeyword(mAdditionalKeywords, ProjectSearch.class) < 0 &&
                !PrefsFragment.getCurrentProject(mContext).isEmpty()) {
            PrefsFragment.setCurrentProject(mContext, null);
        }

        // If there is no owner keyword in the query, it should be cleared
        if (SearchKeyword.findKeyword(mAdditionalKeywords, OwnerSearch.class) < 0 &&
                PrefsFragment.getTrackingUser(mContext) != null) {
            PrefsFragment.clearTrackingUser(mContext);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PrefsFragment.CURRENT_PROJECT:
                injectKeyword(new ProjectSearch(PrefsFragment.getCurrentProject(mContext)));
                break;
            case PrefsFragment.TRACKING_USER:
                injectKeyword(new OwnerSearch(PrefsFragment.getTrackingUser(mContext)));
                break;
        }
    }
}
