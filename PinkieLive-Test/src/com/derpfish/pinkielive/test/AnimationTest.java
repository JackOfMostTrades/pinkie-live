package com.derpfish.pinkielive.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.derpfish.pinkielive.animation.PinkieAnimation;
import com.derpfish.pinkielive.animation.PonyAnimation;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.test.AndroidTestCase;

public class AnimationTest extends AndroidTestCase
{
	public void testAnimation() throws IOException
	{
		final PonyAnimation animation = new PinkieAnimation(getContext().getAssets());
		animation.onCreate();
		animation.initialize(480, 800, 240, 400);
		
		final Bitmap bitmap = Bitmap.createBitmap(480, 800, Config.ARGB_8888);
		final Canvas canvas = new Canvas(bitmap);
		final File outputDir = getContext().getCacheDir();
		System.out.println("Using output: " + outputDir.getAbsolutePath());
		
		int frameNum = 0;
		while (!animation.isComplete())
		{
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			animation.drawAnimation(canvas, 20);
			
			final OutputStream ostream = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + "frame" + frameNum + ".png"); 
			bitmap.compress(CompressFormat.PNG, 0, ostream);
			ostream.close();
			frameNum += 1;
		}
		
		animation.onDestroy();
	}
}
