package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.derpfish.pinkielive.animation.PinkieAnimation;
import com.derpfish.pinkielive.animation.PonyAnimation;
import com.derpfish.pinkielive.animation.RarityAnimation;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class PinkiePieLiveWallpaper extends WallpaperService
{
	public static final String	SHARED_PREFS_NAME	= "livewallpapersettings";
	// Round-trip time for a jump to complete in ms.
	public static final double TIME_FOR_JUMP = 1500.0;

	private Bitmap defaultBg;
	private Map<String, PonyAnimation> ponyAnimations;
	
	// Settings
	private Bitmap selectedBg = null;
	private boolean useDefaultBg = true;
	private long targetFramerate = 30L;
	private boolean enableParallax = true;
	private PonyAnimation selectedPony;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
        AssetManager assetManager = getAssets();
		try {
	        InputStream istr = assetManager.open("defaultbg.jpg");
	        defaultBg = BitmapFactory.decodeStream(istr);
	        istr.close();
		} catch (IOException e) {
			throw new IllegalStateException("Could not find background image");
		}
		
		ponyAnimations = new HashMap<String, PonyAnimation>();
		ponyAnimations.put("pinkie", new PinkieAnimation(assetManager));
		ponyAnimations.put("rarity", new RarityAnimation(assetManager));
	}

	@Override
	public void onDestroy()
	{
		defaultBg.recycle();
		if (selectedBg != null)
		{
			selectedBg.recycle();
			selectedBg = null;
		}
		if (selectedPony != null)
		{
			selectedPony.onDestroy();
			selectedPony = null;
		}
		
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine()
	{
		return new PonyEngine();
	}

	class PonyEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener
	{
		private long lastUpdate;
		
		private int surfaceWidth;
		private int surfaceHeight;
		private float offsetX;
		private float offsetY;
		private final Handler		mHandler		= new Handler();
		private final Paint			mPaint			= new Paint();
		private final Runnable		mDrawPattern	= new Runnable()
													{
														public void run()
														{
															drawFrame();
														}
													};
		
		private boolean				mVisible;
		private final SharedPreferences mPreferences;
		private final BroadcastReceiver broadcastReceiver;

		private PonyEngine()
		{
			final Paint paint = mPaint;
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
			
			mPreferences = PinkiePieLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
			mPreferences.registerOnSharedPreferenceChangeListener(this);
			
			/*
			 * If the media scanner finishes a scan, reload the preferences since this means a
			 * previously unavailable background is now available.
			 */
            final IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
            iFilter.addDataScheme("file");
            
            broadcastReceiver = new BroadcastReceiver()
				{
	                @Override
	                public void onReceive(Context context, Intent intent)
	                {
                        final String action = intent.getAction();
                        if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED))
                        {
                        	onSharedPreferenceChanged(mPreferences, null);
                        }
	                }
				};
			registerReceiver(broadcastReceiver, iFilter);
			
			// Load saved preferences
			onSharedPreferenceChanged(mPreferences, null);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key)
		{
			useDefaultBg = prefs.getBoolean("livewallpaper_defaultbg", true);
			if (useDefaultBg)
			{
				if (selectedBg != null)
				{
					selectedBg.recycle();
					selectedBg = null;
				}
			}
			else
			{
				final String imageUriStr = prefs.getString("livewallpaper_image", null);
				if (imageUriStr != null)
				{
					if (selectedBg != null)
					{
						selectedBg.recycle();
					}
					
					final Uri bgImage = Uri.parse(imageUriStr);
					try
					{
						final InputStream istr = getContentResolver().openInputStream(bgImage);
						selectedBg = BitmapFactory.decodeStream(istr);
						istr.close();
					}
					catch (IOException e)
					{
						selectedBg = null;
						Log.w("PinkieLive", e);
					}
				}
			}
			
			// Framerate
			final String frameratePref = prefs.getString("livewallpaper_framerate", "30");
			try
			{
				targetFramerate = Long.parseLong(frameratePref);
			}
			catch (NumberFormatException e)
			{
				Log.e("PinkieLive", e.getMessage());
			}
			
			// Parallax
			enableParallax = prefs.getBoolean("livewallpaper_enableparallax", true);
			if (!enableParallax)
			{
				offsetX = offsetY = 0.0f;
			}
			
			// Change selected pony
			if (selectedPony != null)
			{
				selectedPony.onDestroy();
			}
			selectedPony = ponyAnimations.get(prefs.getString("livewallpaper_pony", "pinkie"));
			selectedPony.onCreate();
			
			drawFrame();
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder)
		{
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy()
		{
			super.onDestroy();
			
			mHandler.removeCallbacks(mDrawPattern);
			unregisterReceiver(broadcastReceiver);
		}

		@Override
		public void onVisibilityChanged(boolean visible)
		{
			mVisible = visible;
			if (visible)
			{
				drawFrame();
			}
			else
			{
				mHandler.removeCallbacks(mDrawPattern);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height)
		{
			super.onSurfaceChanged(holder, format, width, height);
			surfaceWidth = width;
			surfaceHeight = height;
			drawFrame();
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder)
		{
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder)
		{
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mHandler.removeCallbacks(mDrawPattern);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels)
		{
			if (enableParallax)
			{
				offsetX = xPixels;
				offsetY = yPixels;
				drawFrame();
			}
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event)
		{
			if (event.getAction() == MotionEvent.ACTION_UP)
			{
				// If the length of time pressed was less than 0.5 seconds, trigger a new drawing
				if (event.getEventTime() - event.getDownTime() < 500)
				{
					if (selectedPony.isComplete())
					{
						selectedPony.initialize(surfaceWidth, surfaceHeight, event.getX(), event.getY());
						lastUpdate = SystemClock.elapsedRealtime();
						drawFrame();
					}
				}
			}
			
			super.onTouchEvent(event);
		}

		/*
		 * Draw one frame of the animation. This method gets called repeatedly
		 * by posting a delayed Runnable. You can do any drawing you want in
		 * here. This example draws a wireframe cube.
		 */
		private void drawFrame()
		{
			final SurfaceHolder holder = getSurfaceHolder();

			Canvas c = null;
			try
			{
				c = holder.lockCanvas();
				if (c != null)
				{
					// Blank canvas
					final Paint paintBlack = new Paint();
					paintBlack.setColor(0xff000000);
					c.drawRect(0.0f, 0.0f, surfaceWidth, surfaceHeight, paintBlack);
					
					// draw something
					if (useDefaultBg || selectedBg != null)
					{
						final Bitmap actualBg = selectedBg != null ? selectedBg : defaultBg;
						final WallpaperManager wmMan = WallpaperManager.getInstance(getApplicationContext());
						final int minWidth = wmMan.getDesiredMinimumWidth();
						final int minHeight = wmMan.getDesiredMinimumHeight();
						final float bgScale = Math.min(((float)actualBg.getWidth()) / ((float)minWidth), ((float)actualBg.getHeight()) / ((float)minHeight));
						final int centeringOffsetX = (int)((float)actualBg.getWidth() - bgScale*minWidth)/2;
						final int centeringOffsetY = (int)((float)actualBg.getHeight() - bgScale*minHeight)/2;
						
						c.drawBitmap(actualBg,
								new Rect(centeringOffsetX - (int)(offsetX*bgScale),
										centeringOffsetY - (int)(offsetY*bgScale),
										centeringOffsetX + (int)((-offsetX + surfaceWidth)*bgScale),
										centeringOffsetY + (int)((-offsetY + surfaceHeight)*bgScale)),
								new Rect(0, 0, surfaceWidth, surfaceHeight), mPaint);
					}
					
					// Decide new position and velocity.
					if (!selectedPony.isComplete())
					{
						final long now = SystemClock.elapsedRealtime();
						long elapsedTime = now - lastUpdate;
						lastUpdate = now;
						selectedPony.drawAnimation(c, elapsedTime);
					}
				}
			}
			finally
			{
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			mHandler.removeCallbacks(mDrawPattern);
			// Only queue another frame if we're still animating pinkie
			if (mVisible && !selectedPony.isComplete())
			{
				mHandler.postDelayed(mDrawPattern, 1000 / targetFramerate);
			}
		}
	}
}