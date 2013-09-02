/*
 * Copyright (C) 2013 Morphoss Ltd
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
package com.morphoss.jumble.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.morphoss.jumble.BaseActivity;
import com.morphoss.jumble.Constants;
import com.morphoss.jumble.database.JumbleProvider;
import com.morphoss.jumble.database.JumbleWordsTable;
import com.morphoss.jumble.models.Category;
import com.morphoss.jumble.models.Word;
import com.morphoss.jumble.videos.AnimalsEndActivity;
import com.morphoss.jumble.videos.ClothesEndActivity;
import com.morphoss.jumble.videos.ColorsEndActivity;
import com.morphoss.jumble.videos.FruitsAndVegetablesEndActivity;
import com.morphoss.jumble.videos.HealthEndActivity;
import com.morphoss.jumble.videos.HomeEndActivity;
import com.morphoss.jumble.videos.NatureEndActivity;
import com.morphoss.jumble.videos.SportsEndActivity;
import com.morphoss.jumble.R;

public class JumbleActivity extends BaseActivity {

	/**
	 * this class displays the word to guess, the image, the pronunciation
	 */
	public static final String CATEGORY_KEY = "category";
	public static final String TAG = "JumbleActivity";
	public static final String emptySpace = "";
	public static Word correctWord;
	public static Word wordHint;
	public static int time;
	public static int numberMoves = 0;
	public static ContentResolver resolver;
	private GridView gridViewGuess;
	private GridView gridViewScrambled;
	private ImageView imageAdaptor;
	private TileGridAdapter guessAdaptor;
	private TileGridAdapter scrambledAdaptor;
	private int timeStart;
	private int timeEnd;
	private MyTouchListener touchListener;
	private MyDragListener dragListener;
	public static Category currentCategory = null;
	private View startView;
	private ImageView btnClosePopup;
	private ImageView btnCreatePopup;
	private ImageView btnHint;
	private PopupWindow pwindo;
	private boolean findWord = false;
	private ViewGroup RootView1;
	private ViewGroup RootView2;
	private Drawable d;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_jumble);
		Bundle b = getIntent().getExtras();
		int categoryIndex = b.getInt(CATEGORY_KEY);
		currentCategory = CategoryGridAdapter.getCategory(categoryIndex);
		RootView1 = (ViewGroup) findViewById(R.id.scrambled);
		RootView2 = (ViewGroup) findViewById(R.id.guess);
		touchListener = new MyTouchListener();
		dragListener = new MyDragListener();
		gridViewGuess = (GridView) findViewById(R.id.guess);
		gridViewScrambled = (GridView) findViewById(R.id.scrambled);
		imageAdaptor = (ImageView) findViewById(R.id.image);
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		btnCreatePopup = (ImageView) findViewById(R.id.hint);
		btnCreatePopup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				initiatePopupWindow();
			}
		});

		if (currentCategory.getNextWord(this) == null) {
			startVideo();
		} else {
			startNewWord(currentCategory.getNextWord(this));
		}
	}

	/**
	 * this method creates the popup window for the hint to listen to the
	 * pronunciation of the word
	 */
	private void initiatePopupWindow() {
		try {

			LayoutInflater inflater = (LayoutInflater) JumbleActivity.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.screen_popup,
					(ViewGroup) findViewById(R.id.popup_element));

			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);

			pwindo = new PopupWindow(layout, (int) (420 * metrics.density),
					(int) (300 * metrics.density), true);
			pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);

			btnClosePopup = (ImageView) layout
					.findViewById(R.id.btn_close_popup);
			btnClosePopup.setOnClickListener(cancel_button_click_listener);
			btnHint = (ImageView) layout.findViewById(R.id.btn_hint);
			btnHint.setOnClickListener(hint_button_click_listener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method closes the popup window
	 */
	private OnClickListener cancel_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			pwindo.dismiss();

		}
	};

	/**
	 * this method takes 10 points out of the total score each time that the
	 * hint is used
	 */
	private OnClickListener hint_button_click_listener = new OnClickListener() {
		public void onClick(View v) {
			playHint(v);
			MainActivity.scoreTotal -= 10;
		}
	};

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Intent intent = new Intent(this, CategoryScreenActivity.class);
		startActivity(intent);
		finish();
	}

	/**
	 * This method selects a new word, scrambled it and displays it on the
	 * screen
	 * 
	 * @param word
	 *            the word to guess
	 */
	private void startNewWord(Word word) {

		wordHint = word;
		findWord = false;
		unbindDrawables(RootView1);
		unbindDrawables(RootView2);
		imageAdaptor.setVisibility(View.VISIBLE);
		d = imageAdaptor.getDrawable();

		System.gc();
		setTimeStart((int) System.currentTimeMillis());
		Log.d(TAG, "time start :" + System.currentTimeMillis());
		correctWord = word;
		imageAdaptor.setImageDrawable(word.getImage(this));
		String scrambled = scramble(correctWord.getLocalisedWord());
		int wordLen = word.getLocalisedWord().length();
		Log.d(TAG, "Selected word: " + correctWord.getLocalisedWord()
				+ " Scrambled: " + scrambled + " Len: " + wordLen);

		myApp.stopPlaying();

		scrambledAdaptor = generateWordAdapter(scrambled);
		gridViewScrambled.setNumColumns(wordLen);
		gridViewScrambled.setAdapter(scrambledAdaptor);
		guessAdaptor = generateBlankAdapter(wordLen);
		gridViewGuess.setNumColumns(wordLen);
		gridViewGuess.setAdapter(guessAdaptor);
		Log.d(TAG,
				"current category min score :" + currentCategory.getMinScore());

	}

	/**
	 * This method plays the pronunciation of the word
	 * 
	 * @param view
	 */
	public void playHint(View view) {
		String sound = wordHint.getSoundPath();
		if(sound != null){
		Log.d(TAG, "soundPath: " + sound);
		myApp.playSoundJumble(Constants.storagePath + File.separator + sound);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	private void unbindDrawables(View view) {
		if (view.getBackground() != null) {
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			if (!(view instanceof AdapterView<?>)) {
				((ViewGroup) view).removeAllViews();
			}
		}
	}

	/**
	 * This methods generates the gridview with scrambled letters
	 * 
	 * @param word
	 * @return
	 */
	private TileGridAdapter generateWordAdapter(String word) {
		ArrayList<View> views = new ArrayList<View>();

		for (int i = 0; i < word.length(); i++) {
			LayoutInflater inflater = (LayoutInflater) this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			LinearLayout layout = (LinearLayout) inflater.inflate(
					R.layout.letter, null);
			TextView textView = (TextView) layout.findViewById(R.id.letter);
			String ch = word.substring(i, i + 1);
			textView.setText(ch);
			layout.setOnTouchListener(touchListener);
			layout.setOnDragListener(dragListener);
			views.add(layout);
		}

		return new TileGridAdapter(this, views);

	}

	/**
	 * This method generates blank views in the guess gridview
	 * 
	 * @param count
	 * @return
	 */
	private TileGridAdapter generateBlankAdapter(int count) {
		ArrayList<View> views = new ArrayList<View>();

		for (int i = 0; i < count; i++) {
			LayoutInflater inflater = (LayoutInflater) this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			LinearLayout layout = (LinearLayout) inflater.inflate(
					R.layout.bg_letter, null);
			TextView textView = (TextView) layout.findViewById(R.id.letter);
			textView.setText(emptySpace);
			layout.setOnTouchListener(touchListener);
			layout.setOnDragListener(dragListener);
			views.add(layout);
		}
		return new TileGridAdapter(this, views);

	}

	/**
	 * This method scrambles the word
	 * 
	 * @param word
	 *            to guess
	 * @return the word scrambled
	 */
	public String scramble(String word) {

		char[] wordInCharArray = word.toCharArray();
		Random myRandom = new Random();
		String newWord;
		do {
			for (int i = 0; i < 50; i++) {
				int position1 = (int) myRandom.nextInt(word.length());
				int position2 = (int) myRandom.nextInt(word.length());
				char temp;
				temp = wordInCharArray[position1];
				wordInCharArray[position1] = wordInCharArray[position2];
				wordInCharArray[position2] = temp;
			}
			newWord = new String(wordInCharArray);

		} while (newWord.equals(word));

		return newWord;
	}

	private final class MyTouchListener implements OnTouchListener {
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				startView = view;
				ClipData data = ClipData.newPlainText("", "");
				DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(
						view);
				view.startDrag(data, shadowBuilder, view, 0);
				view.setVisibility(View.VISIBLE);
				return true;
			} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == WinningActivity.RESULT_RETURN_CATEGORY) {
			finish();
			Intent intent = new Intent();
			intent.setClass(JumbleActivity.this, CategoryScreenActivity.class);
			startActivity(intent);
		} else if (resultCode == WinningActivity.RESULT_RETURN_HOME) {
			finish();
			Intent intent = new Intent();
			intent.setClass(JumbleActivity.this, MainActivity.class);
			startActivity(intent);
		} else {
			if (currentCategory.getNextWord(this) == null) {
				startVideo();
			} else {
				startNewWord(currentCategory.getNextWord(this));
			}
		}
	}

	/**
	 * This class defines the rules of the drag and drop
	 * 
	 */
	class MyDragListener implements OnDragListener {
		Drawable enterShape = getResources().getDrawable(R.drawable.letter);
		Drawable normalShape = getResources().getDrawable(R.drawable.bg_letter);

		@Override
		public boolean onDrag(View view, DragEvent event) {
			int action = event.getAction();
			switch (action) {

			case DragEvent.ACTION_DROP:
				if (startView == null)
					return false;
				TextView start = (TextView) startView.findViewById(R.id.letter);
				TextView end = (TextView) view.findViewById(R.id.letter);

				if (start.getText() == emptySpace) {
					// do not allow to move a blank from the guess view to an
					// other place
				} else if (end.getText() == emptySpace) {
					// we can move a letter to a blank
					swap(start, end);
					Log.d(TAG, "start : " + start.getText());
					Log.d(TAG, "end : " + end.getText());
					numberMoves += 1;
					Log.d(TAG, "number of moves : " + numberMoves);
					checkAnswer();
				} else if (scrambledAdaptor.contains(start)
						&& scrambledAdaptor.contains(end)) {
					// we are not allowed to move two letters in the scrambled
					// view
				} else if (start.getText() == " "
						&& scrambledAdaptor.contains(start)) {
					// we are allowed to move a space from the scrambled view to
					// the guess view
					swap(start, end);
					Log.d(TAG, "start : " + start.getText());
					Log.d(TAG, "end : " + end.getText());
					numberMoves += 1;
					Log.d(TAG, "number of moves : " + numberMoves);
					checkAnswer();
				}

			}
			return true;

		}

		/**
		 * This method swaps the views when a letter is dropped on the guess
		 * gridview
		 * 
		 * @param a
		 * @param b
		 */
		@SuppressWarnings("deprecation")
		public void swap(TextView a, TextView b) {
			Drawable enterShape = getResources().getDrawable(R.drawable.letter);
			Drawable normalShape = getResources().getDrawable(
					R.drawable.bg_letter);

			String charA = (String) a.getText();
			String charB = (String) b.getText();

			if (charA.equals(emptySpace)) {
				b.setBackgroundDrawable(enterShape);
			} else {
				b.setBackgroundDrawable(normalShape);
			}

			b.setText(charA);
			b.setBackgroundDrawable(enterShape);
			if (charB.equals(emptySpace)) {
				a.setBackgroundDrawable(normalShape);
			} else {
				a.setBackgroundDrawable(enterShape);
			}
			a.setText(charB);
		}
	}

	/**
	 * This method checks that the letters dropped on the guess gridview are in
	 * the right order
	 */
	public void checkAnswer() {

		String cc = SettingsActivity.getLanguageToLoad();

		Log.d(TAG, "correct word : " + correctWord);
		String testWord = "";
		for (int i = 0; i < gridViewGuess.getChildCount(); i++) {
			TextView v = (TextView) gridViewGuess.getChildAt(i).findViewById(
					R.id.letter);
			testWord += v.getText();
		}
		Log.d(TAG, "word on guess view: " + testWord);
		if (gridViewGuess.getChildCount() == correctWord.getLocalisedWord()
				.length()) {
			if (guessAdaptor.TestAnswer(testWord,
					correctWord.getLocalisedWord())) {
				Log.d(TAG, "good answer");
				findWord = true;
				if (findWord = true) {
					gridViewGuess.removeViewInLayout(RootView1);
					gridViewScrambled.removeViewInLayout(RootView2);
					imageAdaptor.setVisibility(View.INVISIBLE);
				}
				setTimeEnd((int) System.currentTimeMillis());
				Log.d(TAG, "time end: " + timeEnd);
				time = getTimeEnd() - getTimeStart();
				Log.d(TAG, "You have found the word in: " + time + " ms");
				insertWord(correctWord.getNameKey(),
						currentCategory.getName(this), cc);
				Intent intent = new Intent(this, WinningActivity.class);
				intent.putExtra(WinningActivity.HAVE_MORE_WORDS,
						(currentCategory.size() > 0));
				intent.putExtra(WinningActivity.AVATAR_ID, AvatarActivity.id);
				startActivityForResult(intent, 0);

			} else {
				Log.d(TAG, "bad answer");
			}

		}
	}

	/**
	 * This method inserts the word found in the table words of the database
	 * 
	 * @param word
	 * @param category
	 * @param cc
	 */
	private void insertWord(String word, String category, String cc) {

		ContentValues cv = new ContentValues();
		cv.put(JumbleWordsTable.WORD, word);
		cv.put(JumbleWordsTable.CATEGORY, category);
		cv.put(JumbleWordsTable.CC, cc);

		resolver = this.getContentResolver();
		resolver.insert(JumbleProvider.CONTENT_URI_WORDS, cv);

		Log.d(TAG, "add the word:" + word + " from category : " + category
				+ " with cc :" + cc + " in database");
	}

	/**
	 * This method gets the time when you start resolving the word
	 * 
	 * @return
	 */
	public int getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(int timeStart) {
		this.timeStart = timeStart;
	}

	/**
	 * This methods gets the time when you have found the word
	 * 
	 * @return
	 */
	public int getTimeEnd() {
		return timeEnd;
	}

	public void setTimeEnd(int timeEnd) {
		this.timeEnd = timeEnd;
	}

	/**
	 * this method starts the video at the end of each category
	 */
	public void startVideo() {
		Log.d(TAG,
				"the id of the current category is : "
						+ currentCategory.getId());

		if (currentCategory.getId() == 1) {
			// we have finish the animals category
			Intent intent = new Intent(this, AnimalsEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 2) {
			// we have finish the fruits and vegetables category
			Intent intent = new Intent(this,
					FruitsAndVegetablesEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 3) {
			// we have finish the home category
			Intent intent = new Intent(this, HomeEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 4) {
			// we have finish the nature category
			Intent intent = new Intent(this, NatureEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 5) {
			// we have finish the colors category
			Intent intent = new Intent(this, ColorsEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 6) {
			// we have finish the clothes category
			Intent intent = new Intent(this, ClothesEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 7) {
			// we have finish the health category
			Intent intent = new Intent(this, HealthEndActivity.class);
			startActivity(intent);
		}
		if (currentCategory.getId() == 8) {
			// we have finish the sports category
			Intent intent = new Intent(this, SportsEndActivity.class);
			startActivity(intent);
		}

	}

}