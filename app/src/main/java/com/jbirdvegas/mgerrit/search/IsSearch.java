package com.jbirdvegas.mgerrit.search;

import com.jbirdvegas.mgerrit.database.UserChanges;
import com.jbirdvegas.mgerrit.objects.ServerVersion;

import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
public class IsSearch extends SearchKeyword {

    public static final String OP_NAME = "is";

    static {
        registerKeyword(OP_NAME, IsSearch.class);
    }

    public IsSearch(String param) {
        super(OP_NAME, param);
    }

    @Override
    public String buildSearch() {
        String param = getParam();
        if ("starred".equals(param)) {
            return UserChanges.C_STARRED + " = 1";
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(OP_NAME).append(":").append(getParam());
        return builder.toString();
    }

    @Override
    public String getGerritQuery(ServerVersion serverVersion) {
        return toString();
    }

    @Override
    public boolean multipleResults() {
        return true;
    }

    public List<String> whitelistedParameters() {
        ArrayList<String> list = new ArrayList<>(1);
        list.add("starred");
        return list;
    }

    @Override
    public String[] getEscapeArgument() {
        // The parameter on this keyword acts as a modifier and is not used to query changes from the database
        return new String[] { };
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}

