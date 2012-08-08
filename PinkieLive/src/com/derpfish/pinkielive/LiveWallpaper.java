package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
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

	private Bitmap defaultBg;
	private Bitmap bmPinkiePie;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
        AssetManager assetManager = getAssets();
		try {
	        InputStream istr = assetManager.open("defaultbg.jpg");
	        defaultBg = BitmapFactory.decodeStream(istr);
	        istr.close();
	        
	        istr = assetManager.open("pinkie.png");
	        bmPinkiePie = BitmapFactory.decodeStream(istr);
	        istr.close();
		} catch (IOException e) {
			throw new IllegalStateException("Could not find background image");
		}
	}

	@Override
	public void onDestroy()
	{
		defaultBg.recycle();
		bmPinkiePie.recycle();
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
		// Round-trip time for a jump to complete in ms.
		private static final double TIME_FOR_JUMP = 1500.0;
		
		// Pinkie's location
		private float pinkieY;
		private double pinkieVelocityY;
		private float pinkieX;
		private double pinkieVelocityX;
		private float pinkieRotationAngle;
		private float pinkieTargetHeight;
		private boolean drawingPinkie = false;
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
					if (!drawingPinkie)
					{
						drawingPinkie = true;
						lastUpdate = SystemClock.elapsedRealtime();
						
						pinkieTargetHeight = Math.min(surfaceHeight/2, event.getY());
						
						pinkieY = surfaceHeight;
						pinkieVelocityY = 4.0*(pinkieTargetHeight-surfaceHeight)/TIME_FOR_JUMP;
						pinkieX = (surfaceWidth - event.getX());
						pinkieVelocityX = 2.0*(surfaceWidth/2.0f - pinkieX)/TIME_FOR_JUMP;

						pinkieRotationAngle = (float)Math.toDegrees(Math.atan2(pinkieVelocityX, -pinkieVelocityY));
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
					if (drawingPinkie)
					{
						final long now = SystemClock.elapsedRealtime();
						long elapsedTime = now - lastUpdate;
						lastUpdate = now;
						
						final double gravity = 8.0*(surfaceHeight-pinkieTargetHeight)/(TIME_FOR_JUMP*TIME_FOR_JUMP);
						pinkieY = (float)(elapsedTime*elapsedTime * gravity + elapsedTime * pinkieVelocityY + pinkieY);
						pinkieVelocityY = elapsedTime * gravity + pinkieVelocityY;
						
						pinkieX = (float)(elapsedTime*pinkieVelocityX + pinkieX);
						
						if (pinkieY > surfaceHeight)
						{
							drawingPinkie = false;
						}
						else
						{
							final int PINKIE_WIDTH = (int)Math.min(surfaceWidth*0.4, surfaceHeight*0.4);
							final float scale = (float)PINKIE_WIDTH/(float)bmPinkiePie.getWidth();
							
							final Matrix matrix = new Matrix();
							matrix.postRotate(pinkieRotationAngle, bmPinkiePie.getWidth()/2, bmPinkiePie.getHeight()/2);
							matrix.postScale(scale, scale);
							matrix.postTranslate(pinkieX - PINKIE_WIDTH/2, pinkieY);
							c.drawBitmap(bmPinkiePie, matrix, mPaint);
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
			if (mVisible && drawingPinkie)
			{
				mHandler.postDelayed(mDrawPattern, 1000 / 25);
			}
		}
	}
}