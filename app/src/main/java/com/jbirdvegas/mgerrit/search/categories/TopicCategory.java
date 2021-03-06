/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.search.categories;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.search.TopicSearch;

public class TopicCategory extends SearchCategory<TopicSearch> {

    @Override
    public View dialogLayout(Context context, LayoutInflater inflater) {
        return getTextDialogView(inflater);
    }

    @NonNull
    @Override
    public String name(Context context) {
        return context.getString(R.string.search_category_topic);
    }

    @Override
    public TopicSearch onSave(Dialog dialog) {
        EditText text = (EditText) dialog.findViewById(android.R.id.text1);
        String s = text.getText().toString();
        if (s.length() > 0) return new TopicSearch(s);
        else return null;
    }

    @Override
    public Class<TopicSearch> getClazz() {
        return TopicSearch.class;
    }
}
