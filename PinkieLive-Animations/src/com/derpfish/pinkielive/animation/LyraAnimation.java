package com.derpfish.pinkielive.animation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class LyraAnimation implements PonyAnimation
{
	private float surfaceWidth;
	private float surfaceHeight;

	private float xPos;
	private float animTargetHeight;
	private boolean flipAnimation;

	private boolean completed = true;
	private long accumulatedTime;

	private Bitmap[] bmAnimation = null;

	private final Paint mPaint = new Paint();

	private File resourceDir;
	private static final long FRAME_DELAY = 50;

	public LyraAnimation()
	{
	}

	@Override
	public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY)
	{
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;

		completed = false;
		accumulatedTime = 0L;

		// Target getting 2/3 of the way up the image to this position
		final int ANIM_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
		final float scale = (float) ANIM_WIDTH / (float) bmAnimation[0].getWidth();
		animTargetHeight = Math.min(surfaceHeight / 2, tapY) - scale * (bmAnimation[0].getHeight() / 3.0f);

		xPos = tapX;
		flipAnimation = (tapX > surfaceWidth/2.0f);
	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		// Decide new position and velocity.
		if (!completed)
		{
			accumulatedTime += elapsedTime;
			final int currentFrame = (int) (accumulatedTime / FRAME_DELAY);

			final double yPos = surfaceHeight + (animTargetHeight - surfaceHeight) * (1 - Math.pow((2.0 * accumulatedTime) / ((double) (bmAnimation.length * FRAME_DELAY)) - 1.0, 4.0));

			if (currentFrame < bmAnimation.length && yPos <= surfaceHeight)
			{
				final Bitmap bitmap = bmAnimation[currentFrame];
				final int ANIM_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
				final float scale = (float) ANIM_WIDTH / (float) bitmap.getWidth();

				final Matrix matrix = new Matrix();
				if (flipAnimation)
				{
					matrix.postScale(-1.0f, 1.0f);
					matrix.postTranslate(bitmap.getWidth(), 0.0f);
				}
				matrix.postScale(scale, scale);
				matrix.postTranslate(xPos - ANIM_WIDTH / 2, (float) yPos);
				canvas.drawBitmap(bitmap, matrix, mPaint);
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
	public void onCreate()
	{
		completed = true;
		try
		{
			final InputStream istr = new FileInputStream(resourceDir.getAbsolutePath() + File.separator + "lyra.zip");
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
			throw new IllegalStateException("Could not load animation: lyra.zip");
		}
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
	public void setResourceDir(final File resourceDir)
	{
		this.resourceDir = resourceDir;
	}
}
