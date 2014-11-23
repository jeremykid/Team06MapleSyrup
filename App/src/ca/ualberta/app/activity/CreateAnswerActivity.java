/*
 * Copyright 2014 Anni Dai
 * Copyright 2014 Bicheng Yan
 * Copyright 2014 Liwen Chen
 * Copyright 2014 Liang Jingjing
 * Copyright 2014 Xiaocong Zhou
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.ualberta.app.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;

import ca.ualberta.app.ESmanager.QuestionListManager;
import ca.ualberta.app.activity.R;
import ca.ualberta.app.models.Answer;
import ca.ualberta.app.models.Question;
import ca.ualberta.app.models.User;
import ca.ualberta.app.thread.UpdateQuestionThread;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.Toast;

public class CreateAnswerActivity extends Activity {
	private ImageView imageView;
	private EditText contentText = null;
	private Answer newAnswer = null;
	private Bitmap image = null;
	private Bitmap imageThumb = null;
	private String imageString = null;
	private QuestionListManager questionListManager;
	public static String QUESTION_ID = "QUESTION_ID";
	private Intent intent;
	Uri imageFileUri;
	Uri stringFileUri;

	private Runnable doFinishAdd = new Runnable() {
		public void run() {
			finish();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_answer);
		contentText = (EditText) findViewById(R.id.answer_content_editText);
		imageView = (ImageView) findViewById(R.id.answer_image_imageView);
		questionListManager = new QuestionListManager();
		imageView.setVisibility(View.GONE);
		intent = getIntent();
	}

	public void cancel_answer(View view) {
		finish();
	}

	public void submit_answer(View view) {
		String content = contentText.getText().toString();
		if (content.trim().length() == 0)
			noContentEntered();
		else {
			if (intent != null) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					long questionId = extras.getLong(QUESTION_ID);
					newAnswer = new Answer(content, User.author.getUsername(),
							imageString);
					Thread thread = new GetUpdateThread(questionId, newAnswer);
					thread.start();
				}
			}

		}
	}

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
	private static final int GET_IMAGE_ACTIVITY_REQUEST_CODE = 2;

	public void take_answer_pic(View view) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		// Create a folder to store pictures
		String folder = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/tmp";
		File folderF = new File(folder);
		if (!folderF.exists()) {
			folderF.mkdir();
		}

		// Create an URI for the picture file
		String imageFilePath = folder + "/"
				+ String.valueOf(System.currentTimeMillis()) + ".jpg";
		File imageFile = new File(imageFilePath);
		imageFileUri = Uri.fromFile(imageFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);

		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

	}

	public void select_answer_pic(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK, null);
		intent.setType("image/*");
		startActivityForResult(intent, GET_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
				String imagePath = imageFileUri.getPath();

				if (saveImageView(imagePath)) {
					setImageView();
				} else {
					refuseUpdatePic();
				}
			}
			if (requestCode == GET_IMAGE_ACTIVITY_REQUEST_CODE) {

				String imagePath = getPath(this, data.getData());
				if (saveImageView(imagePath)) {
					setImageView();
				} else {
					refuseUpdatePic();
				}
			}
		} else if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "Photo Canceled!", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Have no idea what happened!",
					Toast.LENGTH_SHORT).show();
		}

	}

	private void refuseUpdatePic() {
		image = null;
		imageThumb = null;
		imageString = null;
		imageView.setVisibility(View.GONE);
		Toast.makeText(
				this,
				"The Photo is larger than 64KB after resize, please choose another one.",
				Toast.LENGTH_LONG).show();
	}

	// The following code is from
	// http://hmkcode.com/android-display-selected-image-and-its-real-path/
	// 2014-11-18
	private String getPath(Context context, Uri imageFileUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		String result = null;

		CursorLoader cursorLoader = new CursorLoader(context, imageFileUri,
				proj, null, null, null);
		Cursor cursor = cursorLoader.loadInBackground();

		if (cursor != null) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			result = cursor.getString(column_index);
		}
		return result;
	}

	private Boolean saveImageView(String imagePath) {
		image = BitmapFactory.decodeFile(imagePath);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.PNG, 1, stream);
		if (stream.toByteArray().length / 1024 > 1024) {
			stream.reset();
			image.compress(Bitmap.CompressFormat.PNG, 50, stream);
		}
		scaleImage(800, 800, false);
		imageString = compressImage();
		if (imageString == null)
			return false;
		return true;
	}

	private String compressImage() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		int quality = 100;
		while (stream.toByteArray().length / 1024 > 64 && quality != 0) {
			stream.reset();
			image.compress(Bitmap.CompressFormat.JPEG, quality, stream);
			quality -= 10;
		}
		if (quality == 0)
			return null;
		return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
	}

	private static final int THUMBIMAGESIZE = 200;

	private void scaleImage(int width, int height, boolean createThumb) {
		// Scale the pic if it is too large:

		double scaleFactor = 1;
		if (image.getWidth() > width) {
			scaleFactor = image.getWidth() / width;
		} else if (image.getHeight() > height
				&& image.getHeight() > image.getWidth()) {
			scaleFactor = image.getHeight() / height;
		}
		int newWidth = (int) Math.round(image.getWidth() / scaleFactor);
		int newHeight = (int) Math.round(image.getHeight() / scaleFactor);
		if (createThumb)
			imageThumb = Bitmap.createScaledBitmap(image, newWidth, newHeight,
					false);
		else
			image = Bitmap
					.createScaledBitmap(image, newWidth, newHeight, false);

	}

	private void setImageView() {
		scaleImage(THUMBIMAGESIZE, THUMBIMAGESIZE, true);
		imageView.setVisibility(View.VISIBLE);
		imageView.setImageBitmap(imageThumb);
	}

	public void noContentEntered() {
		Toast.makeText(this, "Please fill in the content!", Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.new_input, menu);
		return true;
	}

	/**
	 * Handle action bar item clicks here. The action bar will automatically
	 * handle clicks on the Home/Up button, so long as you specify a parent
	 * activity in AndroidManifest.xml.
	 * 
	 * @param item
	 *            The menu item.
	 * @return true if the item is selected.
	 */

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	class GetUpdateThread extends Thread {
		private long id;
		private Answer newAnswer;

		public GetUpdateThread(long id, Answer newAnswer) {
			this.newAnswer = newAnswer;
			this.id = id;
		}

		@Override
		public void run() {
			Question question = questionListManager.getQuestion(id);
			question.addAnswer(newAnswer);
			Thread updateThread = new UpdateQuestionThread(question);
			updateThread.run();
			runOnUiThread(doFinishAdd);
		}
	}

}