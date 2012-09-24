package com.derpfish.pinkielive.preference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.derpfish.pinkielive.R;
import com.derpfish.pinkielive.download.PonyAnimationContainer;
import com.derpfish.pinkielive.download.PonyAnimationListing;
import com.derpfish.pinkielive.download.PonyDownloader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PonyDownloadListPreference extends DialogPreference implements View.OnClickListener
{
	public PonyDownloadListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public PonyDownloadListPreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	private Map<String, PonyAnimationContainer> currentPonies = new HashMap<String, PonyAnimationContainer>();
	private List<PonyAnimationListing> newListings = new ArrayList<PonyAnimationListing>();
	
	@Override
	protected void onClick()
	{
		new AsyncTask<Void, Void, Void>()
		{
			private ProgressDialog dialog;
			private List<PonyAnimationContainer> currentPonies;
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
					currentPonies = PonyDownloader.getPonyAnimations(
							getContext().getFilesDir(), getContext().getCacheDir(), false);
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
				doRealOnclick(currentPonies, animListings);
			}
		}.execute();
	}
	
	private void doRealOnclick(final List<PonyAnimationContainer> currentPonies, final List<PonyAnimationListing> animListings)
	{
		this.currentPonies.clear();
		for (final PonyAnimationContainer pac : currentPonies)
		{
			this.currentPonies.put(pac.getId(), pac);
		}
		newListings.clear();
        for (final PonyAnimationListing pal : animListings)
        {
        	if (!this.currentPonies.containsKey(pal.getId())
        			|| this.currentPonies.get(pal.getId()).getVersion() < pal.getVersion())
        	{
        		newListings.add(pal);
        	}
        }
		
		super.onClick();
	}
	
	@Override
	public void onPrepareDialogBuilder(final AlertDialog.Builder builder)
	{
        super.onPrepareDialogBuilder(builder);
        
        final ListView listView = new ListView(getContext());
        listView.setAdapter(new PonyAnimationsAdapter(getContext(), 0, newListings));
        listView.setBackgroundColor(Color.WHITE);
        
        builder.setView(listView);
		builder.setPositiveButton(null, null);
	}
	
	private class PonyAnimationsAdapter extends ArrayAdapter<PonyAnimationListing> implements ListAdapter
	{

		public PonyAnimationsAdapter(Context context, int textViewResourceId, List<PonyAnimationListing> objects)
		{
			super(context, textViewResourceId, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final View view;
			if (convertView == null)
			{
				view = View.inflate(getContext(), R.layout.pony_listing, null);
			} else
			{
				view = convertView;
			}
			view.setId(position);
			((TextView)view.findViewById(R.id.title)).setText(this.getItem(position).getName());
			
			if (!currentPonies.containsKey(newListings.get(position).getId()))
			{
				((TextView)view.findViewById(R.id.summary)).setText("New Pony!");
			}
			else
			{
				((TextView)view.findViewById(R.id.summary)).setText("Update Available!");
			}
			view.setOnClickListener(PonyDownloadListPreference.this);
			return view;
		}
	}

	
	@Override
	public void onClick(View view)
	{
		final int which = view.getId();
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
					PonyDownloader.fetchPony(getContext().getFilesDir(), getContext().getCacheDir(), newListings.get(which));
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
				// Dismiss the list of choices
				getDialog().dismiss();
			}
		}.execute();
	}

}
