package com.derpfish.pinkielive.preference;

import android.content.Context;
import com.derpfish.pinkielive.PinkiePieLiveWallpaper;
import com.derpfish.pinkielive.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import java.io.*;

public class PinkiePieLiveWallpaperSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, OnPreferenceClickListener
{
	private static final int SELECT_PICTURE = 1;

	private Intent gallery;

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		getPreferenceManager().setSharedPreferencesName(PinkiePieLiveWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.livewallpaper_settings);

		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		setDefaultBgEnabled();

		findPreference("livewallpaper_image").setOnPreferenceClickListener(this);

		gallery = new Intent();
		gallery.setType("image/*");
		gallery.setAction(Intent.ACTION_GET_CONTENT);
	}

	private void setDefaultBgEnabled()
	{
		final SharedPreferences sharedPr = getPreferenceManager().getSharedPreferences();
		findPreference("livewallpaper_defaultbg").setEnabled(
				!sharedPr.getBoolean("livewallpaper_defaultbg", true)
				|| sharedPr.getString("livewallpaper_image", null) != null);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onDestroy()
	{
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		setDefaultBgEnabled();
	}

	@Override
	public boolean onPreferenceClick(Preference pr)
	{
		if (pr.getKey().equals("livewallpaper_image"))
		{
			startActivityForResult(Intent.createChooser(gallery, "Select Background"), SELECT_PICTURE);
		}
		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			if (requestCode == SELECT_PICTURE)
			{
				Uri selectedImageUri = data.getData();
				try {
                    // Delete old image
                    String oldLocation = getPreferenceManager().getSharedPreferences().getString("livewallpaper_image", null);
                    if (oldLocation != null) {
                        File oldFile = new File(getApplicationContext().getFilesDir(), oldLocation);
                        if (oldFile.exists()) oldFile.delete();
                    }

					// Copy file to a private copy
                    String newFileName = "custom_bg_" + selectedImageUri.getLastPathSegment();
                    InputStream fis = null;
                    FileOutputStream fos = null;
                    try {
                        fos = openFileOutput(newFileName, Context.MODE_PRIVATE);
                        fis = getContentResolver().openInputStream(selectedImageUri);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = fis.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                    } finally {
                        try {
                            if (fis != null) {
					            fis.close();
                            }
                        } catch (IOException e) {
                            // Do nothing
                        }
                        try {
                            if (fos != null) {
					            fos.close();
                            }
                        } catch (IOException e) {
                            // Do nothing
                        }
                    }

					SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
					editor.putString("livewallpaper_image", newFileName);
					editor.putBoolean("livewallpaper_defaultbg", false);
					editor.commit();
					((CheckBoxPreference) findPreference("livewallpaper_defaultbg")).setChecked(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
