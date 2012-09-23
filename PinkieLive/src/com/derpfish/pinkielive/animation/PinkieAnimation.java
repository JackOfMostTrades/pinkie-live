package com.derpfish.pinkielive.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class PinkieAnimation implements PonyAnimation
{
	private float surfaceWidth;
	private float surfaceHeight;

	private float pinkieX;
	private double pinkieVelocityX;
	private float pinkieRotationAngle;
	private float pinkieTargetHeight;
	private boolean flipAnimation;

	private boolean completed = true;
	private long accumulatedTime;

	private Bitmap[] bmAnimation = null;
	private int lastAnim = -1;

	private final Paint mPaint = new Paint();

	private final AssetManager assetManager;
	private static final int NUM_ANIMATIONS = 7;
	private static final long FRAME_DELAY = 50;

	private Thread loaderThread;
	private final LoadAnimationRunnable loadAnimationRunnable = new LoadAnimationRunnable();

	public PinkieAnimation(final AssetManager assetManager)
	{
		this.assetManager = assetManager;
	}

	@Override
	public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY)
	{
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;

		// Wait for animation to be fully loaded
		waitForLoader();

		completed = false;
		accumulatedTime = 0L;

		// Target getting 2/3 of the way up the image to this position
		final int PINKIE_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
		final float scale = (float) PINKIE_WIDTH / (float) bmAnimation[0].getWidth();
		pinkieTargetHeight = Math.min(surfaceHeight / 2, tapY) - scale * (bmAnimation[0].getHeight() / 3.0f);

		final double pinkieVelocityY = 4.0 * (pinkieTargetHeight - surfaceHeight) / ((double) (bmAnimation.length * FRAME_DELAY));
		pinkieX = (surfaceWidth - tapX);
		pinkieVelocityX = 2.0 * (surfaceWidth / 2.0f - pinkieX) / ((double) (bmAnimation.length * FRAME_DELAY));
		flipAnimation = (pinkieVelocityX < 0);

		pinkieRotationAngle = (float) Math.toDegrees(Math.atan2(pinkieVelocityX, -pinkieVelocityY));
	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		// Decide new position and velocity.
		if (!completed)
		{
			accumulatedTime += elapsedTime;
			final int currentFrame = (int) (accumulatedTime / FRAME_DELAY);

			final double pinkieY = surfaceHeight + (pinkieTargetHeight - surfaceHeight) * (1 - Math.pow((2.0 * accumulatedTime) / ((double) (bmAnimation.length * FRAME_DELAY)) - 1.0, 4.0));
			pinkieX = (float) (elapsedTime * pinkieVelocityX + pinkieX);

			if (currentFrame < bmAnimation.length && pinkieY <= surfaceHeight)
			{
				final Bitmap bitmap = bmAnimation[currentFrame];
				final int PINKIE_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
				final float scale = (float) PINKIE_WIDTH / (float) bitmap.getWidth();

				final Matrix matrix = new Matrix();
				if (flipAnimation)
				{
					matrix.postScale(-1.0f, 1.0f);
					matrix.postTranslate(bitmap.getWidth(), 0.0f);
				}
				matrix.postRotate(pinkieRotationAngle, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
				matrix.postScale(scale, scale);
				matrix.postTranslate(pinkieX - PINKIE_WIDTH / 2, (float) pinkieY);
				canvas.drawBitmap(bitmap, matrix, mPaint);
			}
			else
			{
				completed = true;
				loadNextAnimation();
			}
		}
	}

	private void loadNextAnimation()
	{
		waitForLoader();
		loaderThread = new Thread(loadAnimationRunnable);
		loaderThread.start();
	}

	private void waitForLoader()
	{
		synchronized (this)
		{
			if (loaderThread == null)
			{
				return;
			}
			try
			{
				loaderThread.join();
			}
			catch (InterruptedException e)
			{
				throw new IllegalStateException("Unhandled interruption: " + e);
			}
			loaderThread = null;
		}
	}

	private class LoadAnimationRunnable implements Runnable
	{
		@Override
		public void run()
		{
			lastAnim = (lastAnim + 1) % NUM_ANIMATIONS;
			if (bmAnimation != null)
			{
				for (int i = 0; i < bmAnimation.length; i++)
				{
					bmAnimation[i].recycle();
				}
				bmAnimation = null;
			}
			try
			{
				final InputStream istr = assetManager.open("jump" + (lastAnim + 1) + ".zip");
				final ZipInputStream zis = new ZipInputStream(istr);
				final Map<String, Bitmap> bitmaps = new HashMap<String, Bitmap>();
				ZipEntry zipEntry = null;
				while ((zipEntry = zis.getNextEntry()) != null)
				{
					bitmaps.put(zipEntry.getName(), BitmapFactory.decodeStream(zis));
				}
				zis.close();
				istr.close();

				final List<String> names = new ArrayList<String>(bitmaps.keySet());
				Collections.sort(names);
				bmAnimation = new Bitmap[bitmaps.size()];
				for (int i = 0; i < names.size(); i++)
				{
					bmAnimation[i] = bitmaps.get(names.get(i));
				}
			}
			catch (IOException e)
			{
				throw new IllegalStateException("Could not load animation: " + lastAnim);
			}
		}
	}

	@Override
	public boolean isComplete()
	{
		return completed;
	}

	@Override
	public void onCreate()
	{
		completed = true;
		loadNextAnimation();
	}

	@Override
	public void onDestroy()
	{
		if (bmAnimation != null)
		{
			for (int i = 0; i < bmAnimation.length; i++)
			{
				bmAnimation[i].recycle();
			}
		}
	}

	@Override
	public void setResourceDir(File resourceDir)
	{
	}
}
