package com.derpfish.pinkielive;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class PinkieAnimation implements PonyAnimation
{
	private final Bitmap bmPinkiePie;
	
	private float surfaceWidth;
	private float surfaceHeight;
	
	private float pinkieY;
	private double pinkieVelocityY;
	private float pinkieX;
	private double pinkieVelocityX;
	private float pinkieRotationAngle;
	private float pinkieTargetHeight;
	private boolean completed;
	
	private final Paint mPaint = new Paint();
	
	public PinkieAnimation(final AssetManager assetManager)
	{
		try {
			final InputStream istr = assetManager.open("pinkie.png");
	        bmPinkiePie = BitmapFactory.decodeStream(istr);
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
		
		pinkieTargetHeight = Math.min(surfaceHeight/2, tapY);
		
		pinkieY = surfaceHeight;
		pinkieVelocityY = 4.0*(pinkieTargetHeight-surfaceHeight)/LiveWallpaper.TIME_FOR_JUMP;
		pinkieX = (surfaceWidth - tapX);
		pinkieVelocityX = 2.0*(surfaceWidth/2.0f - pinkieX)/LiveWallpaper.TIME_FOR_JUMP;

		pinkieRotationAngle = (float)Math.toDegrees(Math.atan2(pinkieVelocityX, -pinkieVelocityY));
		
		completed = false;
	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		// Decide new position and velocity.
		if (!completed)
		{
			final double gravity = 8.0*(surfaceHeight-pinkieTargetHeight)/(LiveWallpaper.TIME_FOR_JUMP*LiveWallpaper.TIME_FOR_JUMP);
			pinkieY = (float)(elapsedTime*elapsedTime * gravity + elapsedTime * pinkieVelocityY + pinkieY);
			pinkieVelocityY = elapsedTime * gravity + pinkieVelocityY;
			
			pinkieX = (float)(elapsedTime*pinkieVelocityX + pinkieX);
			
			if (pinkieY > surfaceHeight)
			{
				completed = true;
			}
			else
			{
				final int PINKIE_WIDTH = (int)Math.min(surfaceWidth*0.4, surfaceHeight*0.4);
				final float scale = (float)PINKIE_WIDTH/(float)bmPinkiePie.getWidth();
				
				final Matrix matrix = new Matrix();
				matrix.postRotate(pinkieRotationAngle, bmPinkiePie.getWidth()/2, bmPinkiePie.getHeight()/2);
				matrix.postScale(scale, scale);
				matrix.postTranslate(pinkieX - PINKIE_WIDTH/2, pinkieY);
				canvas.drawBitmap(bmPinkiePie, matrix, mPaint);
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
		bmPinkiePie.recycle();
	}
	
}
