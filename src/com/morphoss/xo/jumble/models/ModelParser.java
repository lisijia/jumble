/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.morphoss.xo.jumble.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ModelParser {

	/**
	 * This class reads all the data from the JSON file
	 */
	public static final String TAG = "ModelParser";

	public static Collection<Category> readJsonData(String json)
			throws IOException, JSONException {

		JSONObject file = new JSONObject(json);
		JSONObject cats = file.getJSONObject("categories");
		JSONObject Words = file.getJSONObject("words");
		HashMap<Integer, Category> catMap = Category
				.getCategoriesFromJson(cats);

		HashMap<String, Word> words = Word.getWordFromJson(Words, catMap);

		HashMap<String, ArrayList<Localisation>> localisations = Localisation
				.getLocalisationFromJson(file.getJSONObject("localisations"));

		for (String cc : localisations.keySet()) {
			ArrayList<Localisation> locs = localisations.get(cc);
			for (Localisation loc : locs) {
				Word word = words.get(loc.getNameKey());
				if (word != null) {
					word.addLocalisation(cc, loc);
				} else {
					Log.e(TAG, "Unable to find original word with name key: "
							+ loc.getNameKey());
				}
			}
		}

		return catMap.values();
	}
}
