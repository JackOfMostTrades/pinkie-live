package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.FloatMath;

public class RarityAnimation implements PonyAnimation
{
	private final Bitmap bmRarity;
	
	private float surfaceWidth;
	private float surfaceHeight;
	
	private float locX;
	private double velocityX;
	private float lineAngle;
	private float lineLength;
	private boolean flipBitmap;
	
	private boolean completed;
	
	private final Paint mPaint = new Paint();
	
	public RarityAnimation(final AssetManager assetManager)
	{
		try {
			final InputStream istr = assetManager.open("rarity.png");
	        bmRarity = BitmapFactory.decodeStream(istr);
	        istr.close();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not load asset");
		}
	}
	
	@Override
	public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY)
	{
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;
		
		if (Math.abs(surfaceWidth/2 - tapX) < 1.0)
		{
			lineAngle = (float)(Math.PI/2.0);
		}
		else
		{
			lineAngle = (float)Math.atan((surfaceHeight/2 - tapY) / (surfaceWidth/2 - tapX));
		}
		lineLength = surfaceWidth / FloatMath.cos(lineAngle);
		if (tapX <= surfaceWidth/2)
		{
			flipBitmap = false;
			locX = lineLength;
			velocityX = -lineLength/LiveWallpaper.TIME_FOR_JUMP;
		}
		else
		{
			flipBitmap = true;
			locX = 0;
			velocityX = lineLength/LiveWallpaper.TIME_FOR_JUMP;
		}
		
		completed = false;
	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		// Decide new position and velocity.
		if (!completed)
		{
			locX = (int)(velocityX * elapsedTime + locX); 
			
			final int ANIMATION_WIDTH = (int)Math.min(surfaceWidth*0.8, surfaceHeight*0.8);
			final float scale = (float)ANIMATION_WIDTH/(float)bmRarity.getWidth();
			
			float locY = (int)(surfaceHeight/2 + scale*bmRarity.getHeight()/2.0f * Math.sin(Math.PI/lineLength*locX));
			
			if (locX < -ANIMATION_WIDTH || locX > lineLength + ANIMATION_WIDTH)
			{
				completed = true;
			}
			else
			{
				final Matrix matrix = new Matrix();
				matrix.postScale(scale, scale);
				if (flipBitmap)
				{
					matrix.postScale(-1.0f, 1.0f);
				}
				matrix.postTranslate(locX, locY - scale*bmRarity.getHeight()/2.0f);
				matrix.postRotate((float)Math.toDegrees(lineAngle), lineLength/2, surfaceHeight/2);
				matrix.postTranslate((surfaceWidth - lineLength)/2, 0);
				canvas.drawBitmap(bmRarity, matrix, mPaint);
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
		bmRarity.recycle();
	}
	
}
