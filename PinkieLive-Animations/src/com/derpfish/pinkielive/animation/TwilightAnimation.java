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

public class TwilightAnimation implements PonyAnimation
{
	private float surfaceWidth;
	private float surfaceHeight;

	private float xPos;
	private float animTargetHeight;
	private boolean flipAnimation;

	private boolean completed = true;
	private long accumulatedTime;

	private Bitmap[] bmBubbles = null;
	private Bitmap[] bmTwilights = null;

	private final Paint mPaint = new Paint();

	private File resourceDir;
	private static final long FRAME_DELAY = 50;

	public TwilightAnimation()
	{
	}

	@Override
	public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY)
	{
		this.surfaceWidth = surfaceWidth;
		this.surfaceHeight = surfaceHeight;

		completed = false;
		accumulatedTime = 0L;

		// Target getting middle of image onto tap height
		animTargetHeight = Math.min(surfaceHeight / 2, tapY);

		xPos = tapX;
		// These images happen to already be flipped
		flipAnimation = (tapX <= surfaceWidth/2.0f);
	}

	@Override
	public void drawAnimation(final Canvas canvas, final long elapsedTime)
	{
		final int numFrames = bmTwilights.length+6;
		// Decide new position and velocity.
		if (!completed)
		{
			accumulatedTime += elapsedTime;
			final int currentFrame = (int) (accumulatedTime / FRAME_DELAY);
			
			final double yPos;
			if (currentFrame < 7)
			{
				yPos = animTargetHeight;
			}
			else
			{
				yPos = surfaceHeight + (animTargetHeight - surfaceHeight) * (1 - Math.pow((2.0 * accumulatedTime) / ((double) (numFrames * FRAME_DELAY)) - 1.0, 4.0));
			}

			if (currentFrame < numFrames)
			{
				final Bitmap bmTwilight = (currentFrame < 7 ? bmTwilights[0] : bmTwilights[currentFrame-6]);
				final Bitmap bmBubble = (currentFrame < bmBubbles.length ? bmBubbles[currentFrame] : null);
				
				for (Object[] frameDef : new Object[][] {{0.75f, bmTwilight}, {2.125f, bmBubble}})
				{
					final float relSize = (Float)frameDef[0];
					final Bitmap bitmap = (Bitmap)frameDef[1];
					if (bitmap == null)
					{
						continue;
					}
					
					final int ANIM_WIDTH = (int) Math.min(surfaceWidth * relSize, surfaceHeight * relSize);
					final float scale = (float) ANIM_WIDTH / (float) bitmap.getWidth();
					final int ANIM_HEIGHT = (int) (((float) bitmap.getHeight()) * scale);
	
					final Matrix matrix = new Matrix();
					if (flipAnimation)
					{
						matrix.postScale(-1.0f, 1.0f);
						matrix.postTranslate(bitmap.getWidth(), 0.0f);
					}
					matrix.postScale(scale, scale);
					matrix.postTranslate(xPos - ANIM_WIDTH / 2, (float) yPos - ANIM_HEIGHT / 2);
					canvas.drawBitmap(bitmap, matrix, mPaint);
				}
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
		bmBubbles = loadFromZip("bubbles.zip");
		bmTwilights = loadFromZip("twilights.zip");
	}
	
	private Bitmap[] loadFromZip(final String filename)
	{
		final Bitmap[] result;
		try
		{
			final InputStream istr = new FileInputStream(resourceDir.getAbsolutePath() + File.separator + filename);
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
			result = new Bitmap[bitmaps.size()];
			for (int i = 0; i < names.size(); i++)
			{
				result[i] = bitmaps.get(names.get(i));
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Could not load animation: " + filename);
		}
		return result;
	}

	@Override
	public void onDestroy()
	{
		if (bmBubbles != null)
		{
			for (int i = 0; i < bmBubbles.length; i++)
			{
				bmBubbles[i].recycle();
			}
		}
		if (bmTwilights != null)
		{
			for (int i = 0; i < bmTwilights.length; i++)
			{
				bmTwilights[i].recycle();
			}
		}
	}
	
	@Override
	public void setResourceDir(final File resourceDir)
	{
		this.resourceDir = resourceDir;
	}
}
