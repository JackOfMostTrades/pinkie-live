package com.derpfish.pinkielive;

import com.derpfish.pinkielive.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class PinkiePieLiveWallpaperSettings extends PreferenceActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener, OnPreferenceClickListener
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
		
		Preference gallery_pref = findPreference("livewallpaper_image");
        gallery = new Intent();
        gallery.setType("image/*");
        gallery.setAction(Intent.ACTION_GET_CONTENT);
        gallery_pref.setOnPreferenceClickListener(this);
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
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key)
	{
		setDefaultBgEnabled();
	}

	@Override
	public boolean onPreferenceClick(Preference pr) {
        if(pr.getKey().equals("livewallpaper_image")) {
            startActivityForResult(Intent.createChooser(gallery, "Select Background"), SELECT_PICTURE);
        }
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK) {
	        if (requestCode == SELECT_PICTURE) {
	            Uri selectedImageUri = data.getData();
	            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
	            editor.putString("livewallpaper_image", selectedImageUri.toString());
	            editor.putBoolean("livewallpaper_defaultbg", false);
	            editor.commit();
	            ((CheckBoxPreference)findPreference("livewallpaper_defaultbg")).setChecked(false);
	        }
	    }
	}
}
