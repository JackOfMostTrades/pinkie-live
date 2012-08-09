package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class LiveWallpaper extends WallpaperService
{
	public static final String	SHARED_PREFS_NAME	= "livewallpapersettings";
	// Round-trip time for a jump to complete in ms.
	public static final double TIME_FOR_JUMP = 1500.0;

	private Bitmap defaultBg;
	private String selectedPony;
	private Map<String, PonyAnimation> ponyAnimations;
	
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
		for (final PonyAnimation ponyAnimation : ponyAnimations.values())
		{
			ponyAnimation.onDestroy();
		}
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine()
	{
		return new TestPatternEngine();
	}

	class TestPatternEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener
	{
		private PonyAnimation currentAnimation = null;
		private long lastUpdate;

		private Bitmap selectedBg = null;
		
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
		private SharedPreferences	mPreferences;

		private TestPatternEngine()
		{
			final Paint paint = mPaint;
			paint.setColor(0xffffffff);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);
			
			mPreferences = LiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
			mPreferences.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPreferences, null);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key)
		{
			final boolean useDefaultBg = prefs.getBoolean("livewallpaper_defaultbg", true);
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
						throw new IllegalStateException("Could not open selected image.");
					}
				}
			}
			selectedPony = prefs.getString("livewallpaper_pony", null);
			
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
			if (selectedBg != null)
			{
				selectedBg.recycle();
			}
			mHandler.removeCallbacks(mDrawPattern);
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
			offsetX = xPixels;
			offsetY = yPixels;
			drawFrame();
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
					if (currentAnimation == null)
					{
						currentAnimation = ponyAnimations.containsKey(selectedPony) ? ponyAnimations.get(selectedPony) : ponyAnimations.get("pinkie");
						currentAnimation.initialize(surfaceWidth, surfaceHeight, event.getX(), event.getY());
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
					c.drawBitmap(selectedBg != null ? selectedBg : defaultBg,
							new Rect(-(int)offsetX, (int)offsetY, -(int)offsetX + surfaceWidth, (int)offsetY + surfaceHeight),
							new Rect(0, 0, surfaceWidth, surfaceHeight), mPaint);
					
					// Decide new position and velocity.
					if (currentAnimation != null)
					{
						final long now = SystemClock.elapsedRealtime();
						long elapsedTime = now - lastUpdate;
						lastUpdate = now;
						
						currentAnimation.drawAnimation(c, elapsedTime);
						if (currentAnimation.isComplete())
						{
							currentAnimation = null;
						}
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
			if (mVisible && currentAnimation != null)
			{
				mHandler.postDelayed(mDrawPattern, 1000 / 25);
			}
		}
	}
}