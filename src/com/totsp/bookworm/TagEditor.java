package com.totsp.bookworm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.totsp.bookworm.model.Tag;
import com.totsp.bookworm.util.TaskUtil;


/**
 * Tag editing and display activity.
 */
public class TagEditor extends Activity {
	private Spinner template;
	private EditText name;
	private Button saveButton;

	private BookWormApplication application;

	private SaveTagTask saveTagTask;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tageditor);
		application = (BookWormApplication) getApplication();
		setTitle(R.string.titleTagEditor);

		template = (Spinner) findViewById(R.id.tagtemplate);
		
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.tagtemplates, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        template.setAdapter(adapter);
		
		// TODO: Parse tag text into template and text
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
					Toast.makeText(TagEditor.this, getString(R.string.msgMinmumTag), Toast.LENGTH_LONG).show();
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
			long tagId = savedInstanceState.getLong(Constants.TAG_ID, 0L);
			if (tagId > 0) {
				application.establishSelectedTag(tagId);
				setExistingViewData();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle saveState) {
		if (application.selectedTag != null) {
			saveState.putLong(Constants.TAG_ID, application.selectedTag.id);
		}
		super.onSaveInstanceState(saveState);
	}


	private void setExistingViewData() {
		Tag tag = application.selectedTag;
		if (tag != null) {
			name.setText(tag.text);
		}
	}

	/**
	 * Posts changes to background task to be saved.
	 */
	private void saveEdits() {
		// establish newGroup
		Tag editedTag = new Tag();
		if (template.getSelectedItemId() == 0) {
			editedTag.text = (name.getText().toString());
		}
		else
		{
			editedTag.text = (template.getSelectedItem().toString() + " " + name.getText().toString());
		}

		// save settings from existing Group, if present (if we are editing)
		Tag selectedTag = application.selectedTag;
		if (selectedTag != null) {
			editedTag.id = (selectedTag.id);
		}

		saveTagTask = new SaveTagTask();
		saveTagTask.execute(editedTag);
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
		protected void onCancelled() {
			TaskUtil.dismissDialog(dialog);
			super.onCancelled();
		}

		@Override
		protected Boolean doInBackground(final Tag... args) {
			Tag tag = args[0];
			
			if ((tag != null) && (tag.id > 0)) {
				application.dataManager.updateTag(tag);
				application.establishSelectedTag(tag.id);
				return true;
			} else if ((tag != null) && (tag.id == 0)) {
				long tagId = application.dataManager.insertTag(tag);
				if (tagId > 0) {
					application.establishSelectedTag(tagId);
					return true;
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(final Boolean b) {
			TaskUtil.dismissDialog(dialog);
			if (!b) {
				Toast.makeText(TagEditor.this, getString(R.string.msgTagSaveError), Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(TagEditor.this, getString(R.string.msgTagSaved), Toast.LENGTH_SHORT).show();
				TagEditor.this.finish();
			}
		}
	}	
}
