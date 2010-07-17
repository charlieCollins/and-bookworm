package com.totsp.bookworm;

import com.totsp.bookworm.model.Group;
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
 * Group detail editing and display activity.
 */
public class GroupForm extends Activity {
	private EditText name;
	private EditText description;
	private Button saveButton;

	private BookWormApplication application;

	private SaveGroupTask saveGroupTask;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupform);
		application = (BookWormApplication) getApplication();
		setTitle(R.string.titleGroupForm);

		name = (EditText)findViewById(R.id.groupname);
			
		description = (EditText) findViewById(R.id.groupdescription);
		
		if (application.selectedGroup != null) {
			name.setText(application.selectedGroup.name);
			description.setText(application.selectedGroup.description);
		}

		saveButton = (Button) findViewById(R.id.savegroup);
		saveButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (((name != null) && (name.getText() != null) && (!name.getText().toString().equals("")))) {
					saveEdits();
				} else {
					Toast.makeText(GroupForm.this, getString(R.string.msgMinimumSave), Toast.LENGTH_LONG).show();
				}
			}
		});	    
	}

	@Override
	public void onPause() {
		name = null;
		TaskUtil.pauseTask(saveGroupTask);
		super.onPause();
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (application.selectedGroup == null) {
			long groupId = savedInstanceState.getLong(Constants.GROUP_ID, 0L);
			if (groupId > 0) {
				application.establishSelectedGroup(groupId);
				setExistingViewData();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle saveState) {
		if (application.selectedGroup != null) {
			saveState.putLong(Constants.GROUP_ID, application.selectedGroup.id);
		}
		super.onSaveInstanceState(saveState);
	}


	private void setExistingViewData() {
		Group group = application.selectedGroup;
		if (group != null) {
			name.setText(group.name);
			description.setText(group.description);

		}
	}

	/**
	 * Posts changes to background task to be saved.
	 */
	private void saveEdits() {
		// establish newGroup
		Group newGroup = new Group();
		newGroup.name = (name.getText().toString());
		newGroup.description = (description.getText().toString());

		// save settings from existing Group, if present (if we are editing)
		Group group = application.selectedGroup;
		if (group != null) {
			newGroup.id = (group.id);
		}

		saveGroupTask = new SaveGroupTask();
		saveGroupTask.execute(newGroup);
	}

	/**
	 * Asynchronous save changes task.
	 * Saves changes to the specified group to the DB in the background.
	 * Extends {@link android.os.AsyncTask}
	 */
	private class SaveGroupTask extends AsyncTask<Group, Void, Boolean> {
		private final ProgressDialog dialog = new ProgressDialog(GroupForm.this);

		@Override
		protected void onPreExecute() {
			dialog.setMessage(getString(R.string.msgSavingGroupInfo));
			dialog.show();
		}

		@Override
		protected Boolean doInBackground(final Group... args) {
			Group group = args[0];
			
			if ((group != null) && (group.id > 0)) {
				application.dataManager.updateGroup(group);
				application.establishSelectedGroup(group.id);
				return true;
			} else if ((group != null) && (group.id == 0)) {
				long groupId = application.dataManager.insertGroup(group);
				if (groupId > 0) {
					application.establishSelectedGroup(groupId);
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
				Toast.makeText(GroupForm.this, getString(R.string.msgGroupSaveError), Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(GroupForm.this, getString(R.string.msgGroupSaved), Toast.LENGTH_SHORT).show();
			}
		}
	}	
}
