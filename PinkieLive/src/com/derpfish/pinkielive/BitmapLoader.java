package com.derpfish.pinkielive;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapLoader
{
	public static Bitmap decodeSampledBitmapFromInputStream(InputStream istream, int sampleSize)
	{
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		return BitmapFactory.decodeStream(istream, null, options);
	}
	
	public static int getSampleSizeFromInputStream(InputStream istream, int reqWidth, int reqHeight)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(istream, null, options);

		// Calculate inSampleSize
		return calculateInSampleSize(options, reqWidth, reqHeight);
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;

		return Math.max(1, Math.max(
				Math.round((float) height / (float) reqHeight),
				Math.round((float) width / (float) reqWidth)));
	}
}
