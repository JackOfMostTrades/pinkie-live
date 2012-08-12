package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class PinkieAnimation implements PonyAnimation
{
	private float surfaceWidth;
	private float surfaceHeight;
	
	private float pinkieX;
	private boolean completed;
	private long accumulatedTime;
	
	private final Paint mPaint = new Paint();

	final AssetManager assetManager;
	int currentFrame = -1;
	Bitmap[] bmAnimation = new Bitmap[max(NUM_FRAMES)];
	int lastAnim = -1;
	
	private static final int[] NUM_FRAMES = {18,17,16,16,16,18,20};
	
	private static final int max(final int[] arr)
	{
		int max = -1;
		for (final int x : arr)
		{
			max = Math.max(max, x);
		}
		return max;
	}
	
	public PinkieAnimation(final AssetManager assetManager)
	{
		this.assetManager = assetManager;
	}
	
	@Override
	public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY)
	{
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;
		pinkieX = tapX;
		
		completed = false;
		accumulatedTime = 0L;
		lastAnim = (lastAnim+1) % NUM_FRAMES.length;
		for (int i = 0; i < bmAnimation.length; i++)
		{
			if (bmAnimation[i] != null)
			{
				bmAnimation[i].recycle();
				bmAnimation[i] = null;
			}
		}
		for (int i = 0; i < NUM_FRAMES[lastAnim]; i++)
		{
			try
			{
				final InputStream istr = assetManager.open("jump" + (lastAnim+1) + "/pinkie_jumps" + (i+1) + ".png");
				bmAnimation[i] = BitmapFactory.decodeStream(istr);
				istr.close();
			}
			catch (IOException e)
			{
				throw new IllegalStateException("Could not load frame: " + i);
			}
		}

	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		// Decide new position and velocity.
		if (!completed)
		{
			accumulatedTime += elapsedTime;
			final int frameNum = (int)(accumulatedTime / 40);
			
			if (frameNum < bmAnimation.length && bmAnimation[frameNum] != null)
			{
				// FIXME (all animations should have same scale)
				//final float scale = Math.min(surfaceWidth*0.5f / (float)bmAnimation[frameNum].getWidth(),
				//		surfaceHeight*0.5f / (float)bmAnimation[frameNum].getWidth());
				final float scale = Math.min(surfaceWidth*0.6f / 225.0f, surfaceHeight*0.6f / 225.0f);
				final int targetWidth = (int)(bmAnimation[frameNum].getWidth()*scale);
				final int targetHeight = (int)(bmAnimation[frameNum].getHeight()*scale);
				
				canvas.drawBitmap(bmAnimation[frameNum],
						new Rect(0, 0, bmAnimation[frameNum].getWidth(), bmAnimation[frameNum].getHeight()),
						new Rect((int)pinkieX - targetWidth/2, (int)surfaceHeight-targetHeight, (int)pinkieX + targetWidth/2, (int)surfaceHeight),
						mPaint);
			}
			else
			{
				completed = true;
			}
		}
	}

	@Override
	public boolean isComplete()
	{
		return completed;
	}

	@Override
	public void onDestroy()
	{
		for (int i = 0; i < bmAnimation.length; i++)
		{
			if (bmAnimation[i] != null)
			{
				bmAnimation[i].recycle();
				bmAnimation[i] = null;
			}
		}
	}
	
}
