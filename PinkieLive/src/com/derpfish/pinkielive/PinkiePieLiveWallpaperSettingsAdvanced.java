package com.derpfish.pinkielive;

import com.derpfish.pinkielive.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PinkiePieLiveWallpaperSettingsAdvanced extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
        
		getPreferenceManager().setSharedPreferencesName(PinkiePieLiveWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.livewallpaper_settings_advanced);	
	}	
}
