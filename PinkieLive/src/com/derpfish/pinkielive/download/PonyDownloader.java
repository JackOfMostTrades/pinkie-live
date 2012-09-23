package com.derpfish.pinkielive.download;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.derpfish.pinkielive.animation.PonyAnimation;
import com.derpfish.pinkielive.util.Base64;
import com.derpfish.pinkielive.util.IOUtils;

import dalvik.system.DexClassLoader;

public class PonyDownloader
{
	private static final String animationsUrl = "https://animations.pinkie-live.googlecode.com/git/animations.xml";
	private static final String animationsSigUrl = "https://animations.pinkie-live.googlecode.com/git/animations.sig";
	
	private static final String PUBLIC_KEY_ENCODED =
"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1aP3qdJO5F64+UQVF1zl" +
"DyyM/bN/jJZgU9guq7RLLO81y1IFpxL0Rs66FMkcSz4ZtdtOT6uRhZ9TvrVhMR69" +
"WpxdH3hrh4JhILd+/ZRxQt2GX4FyLDIPqfpA867ZjS+lrgncN48kC2X3z3ETQ46Q" +
"eCjYrLfKeseGy620dWuIV2yXenr1NHSJ5kwOKvOdddEHwijSNwpDo8C93XGCAHtT" +
"fXcmknBeVPNWC1iL0CshpMWuDIvLEh867J6HvpSzyAss0q62mvRyttifjZO8aiSH" +
"LctTLxMnTrxhL9mw4lmFCzI0UoWmeSOiEOQTXhGxWhVE9gXv0jzizTvX5DG9MtTA" +
"CwIDAQAB";
	
	private static Object lock = new Object();
	private static PublicKey publicKey = null;
	
	public static boolean verifyData(final InputStream istream, final String base64sig)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException
	{
		synchronized(lock)
		{
			if (publicKey == null)
			{
		        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_ENCODED, Base64.DEFAULT));
		        final KeyFactory kf = KeyFactory.getInstance("RSA");
		        publicKey = kf.generatePublic(keySpec);
			}
		}
        
        final Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        
        final byte[] buffer = new byte[4096];
        int nread = 0;
        while ((nread = istream.read(buffer)) >= 0)
        {
        	signature.update(buffer, 0, nread);
        }
        istream.close();
        
        return signature.verify(Base64.decode(base64sig, Base64.DEFAULT));
	}
	
	public static List<PonyAnimationListing> fetchListings() throws ParserConfigurationException, SAXException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InvalidKeySpecException
	{
		final byte[] xmlBytes = fetchUrl(animationsUrl, 131072);
		final byte[] sigBytes = fetchUrl(animationsSigUrl, 4096);
		if (!verifyData(new ByteArrayInputStream(xmlBytes), new String(sigBytes)))
		{
			throw new IllegalStateException("Unable to verify signature of animations.xml");
		}
		
		return parseListings(new ByteArrayInputStream(xmlBytes));
	}
	
	private static byte[] fetchUrl(final String url, int maxSize) throws MalformedURLException, IOException
	{
		final URLConnection connection = new URL(url).openConnection();
		connection.connect();
		
		final InputStream istream = connection.getInputStream();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int totalRead = 0;
		int nRead = 0;
		final byte[] buffer = new byte[4096];
		while ((nRead = istream.read(buffer)) >= 0)
		{
			baos.write(buffer, 0, nRead);
			totalRead += nRead;
			if (maxSize > 0 && totalRead > maxSize)
			{
				throw new IllegalStateException("Retrieved animations.xml exceeds maximum allowed size.");
			}
		}
		istream.close();
		baos.close();
		
		return baos.toByteArray();
	}
	
	private static List<PonyAnimationListing> parseListings(final InputStream istream) throws ParserConfigurationException, SAXException, IOException
	{
		final List<PonyAnimationListing> animationListings = new ArrayList<PonyAnimationListing>();
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document dom = builder.parse(istream);
        istream.close();
        
        final Element root = dom.getDocumentElement();
        if (!root.getNodeName().equals("animations"))
        {
        	throw new IllegalArgumentException("Malformed XML");
        }
        
        for (int i = 0; i < root.getChildNodes().getLength(); i++)
        {
        	final Node animation = root.getChildNodes().item(i);
        	if (animation.getNodeType() == Node.TEXT_NODE)
        	{
        		continue;
        	}
        	
        	if (!animation.getNodeName().equals("animation"))
        	{
        		throw new IllegalArgumentException("Malformed XML");
        	}
        	
        	final PonyAnimationListing animListing = new PonyAnimationListing();
        	for (int j = 0; j < animation.getChildNodes().getLength(); j++)
        	{
        		final Node attr = animation.getChildNodes().item(j);
        		if (attr.getNodeType() == Node.TEXT_NODE)
            	{
            		continue;
            	}
        		
        		if (attr.getNodeName().equals("name"))
        		{
        			if (animListing.getName() != null)
        			{
        				throw new IllegalArgumentException("Malformed XML");
        			}
        			animListing.setName(getNodeText(attr));
        		}
        		else if (attr.getNodeName().equals("url"))
        		{
        			if (animListing.getUrl() != null)
        			{
        				throw new IllegalArgumentException("Malformed XML");
        			}
        			animListing.setUrl(getNodeText(attr));
        		}
        		else if (attr.getNodeName().equals("id"))
        		{
        			if (animListing.getId() != null)
        			{
        				throw new IllegalArgumentException("Malformed XML");
        			}
        			animListing.setId(getNodeText(attr));
        		}
        		else if (attr.getNodeName().equals("version"))
        		{
        			if (animListing.getVersion() != null)
        			{
        				throw new IllegalArgumentException("Malformed XML");
        			}
        			animListing.setVersion(Long.parseLong(getNodeText(attr)));
        		}
        		else if (attr.getNodeName().equals("checksum"))
        		{
        			if (animListing.getChecksum() != null)
        			{
        				throw new IllegalArgumentException("Malformed XML");
        			}
        			animListing.setChecksum(getNodeText(attr));
        		}
        		else
        		{
        			throw new IllegalArgumentException("Malformed XML");
        		}
        	}
        	
        	if (animListing.getName() == null || animListing.getUrl() == null
        			|| animListing.getId() == null || animListing.getVersion() == null
        			|| animListing.getChecksum() == null)
        	{
        		throw new IllegalArgumentException("Malformed XML");
        	}
        	
        	animationListings.add(animListing);
        }
        
        return animationListings;
	}
	
	private static String getNodeText(final Node node)
	{
		if (node.getChildNodes().getLength() != 1)
		{
			throw new IllegalArgumentException("Malformed XML");
		}
		final Node innerNode = node.getChildNodes().item(0);
		if (innerNode.getNodeType() != Node.TEXT_NODE)
		{
			throw new IllegalArgumentException("Malformed XML");
		}
		return innerNode.getNodeValue();
	}
	
	public static void fetchPony(final File dataDir, final File cacheDir, final PonyAnimationListing animation) throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InvalidKeySpecException
	{
		final File ponyDir = new File(dataDir.getAbsolutePath() + File.separator + "ponies");
		if (!ponyDir.exists())
		{
			ponyDir.mkdir();
		}
		final File tmpFile = new File(cacheDir.getAbsolutePath() + File.separator + "delme.zip");
		
		final URLConnection connection = new URL(animation.getUrl()).openConnection();
		connection.connect();
		IOUtils.copyStreamAndClose(connection.getInputStream(), new FileOutputStream(tmpFile));
		
		if (!verifyData(new FileInputStream(tmpFile), animation.getChecksum()))
		{
			throw new IllegalStateException("Signature verification failed.");
		}
		
		final File animDir = new File(ponyDir.getAbsolutePath() + File.separator + animation.getId());
		if (animDir.exists())
		{
			for (File file : animDir.listFiles())
			{
				file.delete();
			}
		}
		else
		{
			animDir.mkdir();
		}
		
		final InputStream istr = new FileInputStream(tmpFile);
		final ZipInputStream zis = new ZipInputStream(istr);
		ZipEntry zipEntry = null;
		while ((zipEntry = zis.getNextEntry()) != null)
		{
			final FileOutputStream fos = new FileOutputStream(animDir.getAbsolutePath() + File.separator + zipEntry.getName());
			IOUtils.copyStream(zis, fos);
			fos.close();
		}
		zis.close();
		istr.close();		
		
		tmpFile.delete();
	}
	
	public static List<PonyAnimationContainer> getPonyAnimations(final File dataDir, final File cacheDir) throws FileNotFoundException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException
	{
		final List<PonyAnimationContainer> containers = new ArrayList<PonyAnimationContainer>();
		final File ponyDir = new File(dataDir.getAbsolutePath() + File.separator + "ponies");
		if (!ponyDir.exists())
		{
			return containers;
		}
		
		for (final File subDir : ponyDir.listFiles())
		{
			if (subDir.isDirectory())
			{
				final File manifest = new File(subDir.getAbsolutePath() + File.separator + "manifest.properties");
				if (!manifest.exists())
				{
					continue;
				}
				final File lib = new File(subDir.getAbsolutePath() + File.separator + "lib.jar");
				if (!lib.exists())
				{
					continue;
				}
				
				final Properties properties = new Properties();
			    properties.load(new FileInputStream(manifest));
			    
			    final PonyAnimationContainer container = new PonyAnimationContainer();
			    container.setId(properties.getProperty("id"));
			    container.setName(properties.getProperty("name"));
			    
			    final File animCacheDir = new File(cacheDir.getAbsolutePath() + File.separator + container.getId());
			    if (!animCacheDir.exists())
			    {
			    	animCacheDir.mkdir();
			    }
			    
			    final DexClassLoader classLoader = new DexClassLoader(lib.getAbsolutePath(),
			    		animCacheDir.getAbsolutePath(), null, PonyDownloader.class.getClassLoader());
			    Class<?> animClass = classLoader.loadClass(properties.getProperty("className"));
			    container.setPonyAnimation(animClass.asSubclass(PonyAnimation.class).newInstance());
			    container.getPonyAnimation().setResourceDir(subDir);
			    
			    containers.add(container);
			}
		}
		return containers;
	}
}
