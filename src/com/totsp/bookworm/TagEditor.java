package com.totsp.bookworm;

import com.totsp.bookworm.model.Tag;
import com.totsp.bookworm.util.TaskUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * Tag editing and display activity.
 */
public class TagEditor extends Activity {
	private EditText name;
	private EditText description;
	private Button saveButton;

	private BookWormApplication application;

	private SaveTagTask saveTagTask;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tageditor);
		application = (BookWormApplication) getApplication();
		setTitle(R.string.titleTagEditor);

		name = (EditText)findViewById(R.id.tagtext);
					
		if (application.selectedTag != null) {
			name.setText(application.selectedTag.text);
		}

		saveButton = (Button) findViewById(R.id.savetag);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (((name != null) && (name.getText() != null) && (!name.getText().toString().equals("")))) {
					saveEdits();
				} else {
					Toast.makeText(TagEditor.this, getString(R.string.msgMinimumSave), Toast.LENGTH_LONG).show();
				}
			}
		});	    
	}

	@Override
	public void onPause() {
		name = null;
		TaskUtil.pauseTask(saveTagTask);
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (application.selectedTag == null) {
			long groupId = savedInstanceState.getLong(Constants.GROUP_ID, 0L);
			if (groupId > 0) {
				application.establishSelectedGroup(groupId);
				setExistingViewData();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle saveState) {
		if (application.selectedTag != null) {
			saveState.putLong(Constants.GROUP_ID, application.selectedTag.id);
		}
		super.onSaveInstanceState(saveState);
	}


	private void setExistingViewData() {
		Tag group = application.selectedTag;
		if (group != null) {
			name.setText(group.text);
		}
	}

	/**
	 * Posts changes to background task to be saved.
	 */
	private void saveEdits() {
		// establish newGroup
		Tag newGroup = new Tag();
		newGroup.text = (name.getText().toString());

		// save settings from existing Group, if present (if we are editing)
		Tag tag = application.selectedTag;
		if (tag != null) {
			newGroup.id = (tag.id);
		}

		saveTagTask = new SaveTagTask();
		saveTagTask.execute(newGroup);
	}

	/**
	 * Asynchronous save changes task.
	 * Saves changes to the specified tag to the DB in the background.
	 * Extends {@link android.os.AsyncTask}
	 */
	private class SaveTagTask extends AsyncTask<Tag, Void, Boolean> {
		private final ProgressDialog dialog = new ProgressDialog(TagEditor.this);

		@Override
		protected void onPreExecute() {
			dialog.setMessage(getString(R.string.msgSavingTagInfo));
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(final Tag... args) {
			Tag tag = args[0];
			
			if ((tag != null) && (tag.id > 0)) {
				application.dataManager.updateTag(tag);
				application.establishSelectedGroup(tag.id);
				return true;
			} else if ((tag != null) && (tag.id == 0)) {
				long tagId = application.dataManager.insertTag(tag);
				if (tagId > 0) {
					application.establishSelectedGroup(tagId);
					return true;
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean b) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			if (!b) {
				Toast.makeText(TagEditor.this, getString(R.string.msgTagSaveError), Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(TagEditor.this, getString(R.string.msgTagSaved), Toast.LENGTH_SHORT).show();
			}
		}
	}	
}
