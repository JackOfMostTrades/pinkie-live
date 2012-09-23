package com.derpfish.pinkielive.preference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.derpfish.pinkielive.download.PonyAnimationListing;
import com.derpfish.pinkielive.download.PonyDownloader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.AttributeSet;

public class PonyDownloadListPreference extends ListPreference implements OnPreferenceChangeListener
{
	public PonyDownloadListPreference(Context context)
	{
		super(context);
		this.setOnPreferenceChangeListener(this);
	}
	
	public PonyDownloadListPreference(Context context, AttributeSet attributeSet)
	{
		super(context, attributeSet);
		this.setOnPreferenceChangeListener(this);
	}

	private Map<String, PonyAnimationListing> fetchedListings = new HashMap<String, PonyAnimationListing>();
	
	@Override
	protected void onClick()
	{
		new AsyncTask<Void, Void, Void>()
		{
			private ProgressDialog dialog;
			private List<PonyAnimationListing> animListings;
			
			@Override
			protected void onPreExecute()
			{
				dialog = ProgressDialog.show(getContext(), "", "Loading, please wait...");
			}
			
			@Override
			protected Void doInBackground(Void... arg0)
			{
				try
				{
					animListings = PonyDownloader.fetchListings();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result)
			{
				dialog.dismiss();
				doRealOnclick(animListings);
			}
		}.execute();
	}
	
	private void doRealOnclick(final List<PonyAnimationListing> animListings)
	{
		fetchedListings.clear();
		final String[] entries = new String[animListings.size()];
		final String[] entryValues = new String[animListings.size()];
		for (int i = 0; i < animListings.size(); i++)
		{
			fetchedListings.put(animListings.get(i).getId(), animListings.get(i));
			entries[i] = animListings.get(i).getName();
			entryValues[i] = animListings.get(i).getId();
		}
		setEntries(entries);
		setEntryValues(entryValues);
		
		super.onClick();
	}

	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue)
	{
		if (preference.getKey().equals(getKey()))
		{
			if (fetchedListings.containsKey(newValue))
			{
				new AsyncTask<Void, Void, Void>()
				{
					private ProgressDialog dialog;
					
					@Override
					protected void onPreExecute()
					{
						dialog = ProgressDialog.show(getContext(), "", "Downloading...");
					}
					
					@Override
					protected Void doInBackground(Void... arg0)
					{
						try
						{
							PonyDownloader.fetchPony(getContext().getFilesDir(), getContext().getCacheDir(), fetchedListings.get(newValue));
						}
						catch (Exception e)
						{
							throw new RuntimeException(e);
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result)
					{
						dialog.dismiss();
					}
				}.execute();
			}
		}
		return false;
	}

}
