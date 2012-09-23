package com.derpfish.pinkielive.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils
{
	/**
	 * Copies the bytes from istream to ostream
	 * 
	 * @param istream
	 * @param ostream
	 * @throws IOException 
	 */
	public static void copyStream(final InputStream istream, final OutputStream ostream) throws IOException
	{
		final byte[] buffer = new byte[4096];
		int nread = 0;
		while ((nread = istream.read(buffer)) >= 0)
		{
			ostream.write(buffer, 0, nread);
		}
	}
	
	/**
	 * Copies the bytes from istream to stream, closing both streams when the end of istream
	 * is reached.
	 * @throws IOException 
	 */
	public static void copyStreamAndClose(final InputStream istream, final OutputStream ostream) throws IOException
	{
		copyStream(istream, ostream);
		istream.close();
		ostream.close();
	}
}
