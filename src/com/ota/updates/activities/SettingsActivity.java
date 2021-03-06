/*
 * Copyright (C) 2014 Matt Booth (Kryten2k35).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ota.updates.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;

import com.ota.updates.R;
import com.ota.updates.utils.Constants;
import com.ota.updates.utils.DirectoryPicker;
import com.ota.updates.utils.Preferences;
import com.ota.updates.utils.Utils;

@SuppressLint("SdCardPath")
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnSharedPreferenceChangeListener, Constants{

	public final String TAG = this.getClass().getSimpleName();

	Context mContext;
	Preference mDownloadLocation;
	Builder mInstallPrefsDialog;
	Preference mInstallPrefs;

	SparseBooleanArray mInstallPrefsItems = new SparseBooleanArray();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = this;
		setTheme(Preferences.getTheme(mContext));
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(Preferences.PREF_NAME);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);

		mDownloadLocation = (Preference) findPreference(DOWNLOAD_LOC);
		mDownloadLocation.setOnPreferenceClickListener(this);
		mDownloadLocation.setSummary(Preferences.getDownloadLocation(mContext));

		mInstallPrefs = (Preference) findPreference(INSTALL_PREFS);
		mInstallPrefs.setOnPreferenceClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());

			if(key.equals(CURRENT_THEME)){
				Preferences.setTheme(mContext, listPref.getValue());
				Intent intent = new Intent(mContext, MainActivity.class);
				startActivity(intent);
			} else if(key.equals(UPDATER_BACK_FREQ) || key.equals(UPDATER_BACK_SERVICE)){
	            Utils.setBackgroundCheck(mContext, Preferences.getBackgroundService(mContext));
	        }
		}	
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference == mDownloadLocation){
			showChooser();
		} else if(preference == mInstallPrefs){
			showInstallPrefs();
		}
		return false;
	}

	private void showInstallPrefs(){
		boolean wipeData, wipeCache, wipeDalvik, deleteAfterInstall;

		wipeData = Preferences.getWipeData(mContext);
		wipeCache = Preferences.getWipeCache(mContext);
		wipeDalvik = Preferences.getWipeDalvik(mContext);
		deleteAfterInstall = Preferences.getDeleteAfterInstall(mContext);

		// Default value array for the multichoice class.
		boolean [] defaultValues = { wipeData, wipeCache, wipeDalvik, deleteAfterInstall };

		// Also fill in InstallPrefItems with the default values
		// So that, if the user changes nothing, it doesn't reset all to false.
		mInstallPrefsItems.put(0, wipeData);
		mInstallPrefsItems.put(1, wipeCache);
		mInstallPrefsItems.put(2, wipeDalvik);
		mInstallPrefsItems.put(3, deleteAfterInstall);

		mInstallPrefsDialog = new AlertDialog.Builder(mContext);
		mInstallPrefsDialog.setTitle(R.string.twrp_ors_install_prefs);
		mInstallPrefsDialog.setMultiChoiceItems(R.array.ors_install_entries, defaultValues,
				new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which,
					boolean isChecked) {            
				mInstallPrefsItems.put(which, isChecked);
			}
		});
		mInstallPrefsDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				Preferences.setWipeData(mContext, mInstallPrefsItems.get(0));
				Preferences.setWipeCache(mContext, mInstallPrefsItems.get(1));   
				Preferences.setWipeDalvik(mContext, mInstallPrefsItems.get(2));   
				Preferences.setDeleteAfterInstall(mContext, mInstallPrefsItems.get(3));   
			}
		});
		mInstallPrefsDialog.show();
	}

	private void showChooser() {
		Intent intent = new Intent(this, DirectoryPicker.class);
		intent.putExtra(DirectoryPicker.START_DIR, "/storage");
		startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);

			// Make the path recovery clean
			if(path.contains("/storage/emulated/0")){
				path = path.replaceAll("/storage/emulated/0", "/sdcard");
			} else if(path.contains("/storage/sdcard0")){
				path = path.replaceAll("/storage/sdcard0", "/sdcard");
			} else if(path.contains("/storage/emulated/legacy")){
				path = path.replaceAll("/storage/emulated/legacy", "/sdcard");
			} else{
				path = path.replaceAll("/storage/sdcard1", "/extSdCard");
			}
			Preferences.setDownloadLocation(mContext, path);
			mDownloadLocation.setSummary(path);
		}   
	}
}