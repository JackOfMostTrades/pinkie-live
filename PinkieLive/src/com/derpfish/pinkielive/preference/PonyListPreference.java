package com.derpfish.pinkielive.preference;

import java.util.List;

import com.derpfish.pinkielive.download.PonyAnimationContainer;
import com.derpfish.pinkielive.download.PonyDownloader;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class PonyListPreference extends ListPreference
{
	public PonyListPreference(Context context)
	{
		super(context);
	}
	
	public PonyListPreference(Context context, AttributeSet attributeSet)
	{
		super(context, attributeSet);
	}

	@Override
	protected void onPrepareDialogBuilder(final AlertDialog.Builder builder)
	{
		// Setup the dynamic pony list
		final List<PonyAnimationContainer> animations;
		try
		{
			animations = PonyDownloader.getPonyAnimations(
					getContext().getFilesDir(), getContext().getCacheDir(), false);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		final String[] entries = new String[animations.size()+1];
		final String[] entryValues = new String[animations.size()+1];
		entries[0] = "Pinkie Pie";
		entryValues[0] = "pinkie";
		for (int i = 0; i < animations.size(); i++)
		{
			entries[i+1] = animations.get(i).getName();
			entryValues[i+1] = animations.get(i).getId();
		}
		this.setEntries(entries);
		this.setEntryValues(entryValues);
		
		super.onPrepareDialogBuilder(builder);
	}

}
