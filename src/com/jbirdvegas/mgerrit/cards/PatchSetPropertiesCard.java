package com.jbirdvegas.mgerrit.cards;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.CardsActivity;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.ReviewTab;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.interfaces.OnContextItemSelectedCallback;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

public class PatchSetPropertiesCard extends Card implements View.OnClickListener {
    private final JSONCommit mJSONCommit;
    private final PatchSetViewerActivity mPatchSetViewerActivity;
    private TextView mSubject;
    private TextView mOwner;
    private TextView mAuthor;
    private TextView mCommitter;

    public PatchSetPropertiesCard(JSONCommit commit, PatchSetViewerActivity activity) {
        this.mJSONCommit = commit;
        this.mPatchSetViewerActivity = activity;
    }

    @Override
    public View getCardContent(Context context) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.properties_card, null);
        mSubject = (TextView) rootView.findViewById(R.id.prop_card_subject);
        mOwner = (TextView) rootView.findViewById(R.id.prop_card_owner);
        mAuthor = (TextView) rootView.findViewById(R.id.prop_card_author);
        mCommitter = (TextView) rootView.findViewById(R.id.prop_card_committer);

        mSubject.setText(mJSONCommit.getSubject());
        mOwner.setText(mJSONCommit.getOwnerObject().getName());
        // attach owner's gravatar
        GravatarHelper.attachGravatarToTextView(
                mOwner, mJSONCommit.getOwnerObject().getEmail());
        mOwner.setOnClickListener(this);
        mOwner.setTag(mJSONCommit.getOwnerObject());
        setContextMenu(mOwner);
        try {
            // set text will throw NullPointer if
            // we don't have author/committer objects
            mAuthor.setText(mJSONCommit.getAuthorObject().getName());
            mAuthor.setOnClickListener(this);
            mCommitter.setText(mJSONCommit.getCommitterObject().getName());
            mCommitter.setOnClickListener(this);

            // setup contextmenu click actions
            mAuthor.setTag(mJSONCommit.getAuthorObject());
            setContextMenu(mAuthor);
            mCommitter.setTag(mJSONCommit.getCommitterObject());
            setContextMenu(mCommitter);
            // attach gravatars (if objects are not null)
            GravatarHelper.attachGravatarToTextView(
                    mAuthor, mJSONCommit.getAuthorObject().getEmail());
            GravatarHelper.attachGravatarToTextView(
                    mCommitter, mJSONCommit.getCommitterObject().getEmail());
        } catch (NullPointerException npe) {
            rootView.findViewById(R.id.prop_card_author)
                    .setVisibility(View.GONE);
            rootView.findViewById(R.id.prop_card_committer)
                    .setVisibility(View.GONE);
        }
        return rootView;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), ReviewTab.class);
        if (view.equals(mOwner)) {
            intent.putExtra(CardsActivity.KEY_DEVELOPER, mJSONCommit.getOwnerObject());
        } else if (view.equals(mAuthor)) {
            intent.putExtra(CardsActivity.KEY_DEVELOPER, mJSONCommit.getAuthorObject());
        } else if (view.equals(mCommitter)) {
            intent.putExtra(CardsActivity.KEY_DEVELOPER, mJSONCommit.getCommitterObject());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        view.getContext().startActivity(intent);
    }

    private void setContextMenu(final TextView textView) {
        mPatchSetViewerActivity.registerViewForContextMenu(textView, new OnContextItemSelectedCallback() {
            @Override
            public boolean menuItemSelected(CommitterObject committerObject, int position) {
                String tab = null;
                switch (position) {
                    case CardsActivity.OWNER:
                        tab = "owner";
                        break;
                    case CardsActivity.REVIEWER:
                        tab = "reviewer";
                }
                committerObject.setState(tab);
                Intent intent = new Intent(textView.getContext(), ReviewTab.class);
                intent.putExtra(CardsActivity.KEY_DEVELOPER, committerObject);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                textView.getContext().startActivity(intent);
                return true;
            }
        });
    }
}