package com.derpfish.pinkielive.test;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.TestCase;

import com.derpfish.pinkielive.download.PonyAnimationListing;
import com.derpfish.pinkielive.download.PonyDownloader;
import com.derpfish.pinkielive.util.Base64;

public class PonyDownloaderTest extends TestCase
{
	public void testSigVerify() throws Exception
	{
		boolean result;
		result = PonyDownloader.verifyData(new ByteArrayInputStream(
				Base64.decode("tQaFHvciCjpTVxM+n8vksAxLr53tAoyRf+fp0UJEgsAyzZpY5FLkqimpByc20sQaybSdEGg8QPnQzs0HkHwpHQ==", Base64.DEFAULT)),
				"gIsIjkCgZJnod5ulHSAx6g1HoPKOG6V7Scg+PO23dFJu5Pr9eDzl51jEVc/MC3FvIsR2/R3L8noc" +
						"bS3UnyZduGHOaNScoWwnNp0jI7bLMz/CEWvVXj9QsZ2Z7VwOyAKD15btPD402wIDU49x31PHWhk2" +
						"Xh2Jpag7FsE2iHNJODIzuvvanURqiqX65/WO5gmS2smPYUXvO7dLZzKgxLDfRjiG3nPUn/cT0JVY" +
						"yAQKUfvLSRrhj0vz6WZ1xAQ+TCY4N1yTEXU83ZgPcazBmIXZEt0syqMJcUS338eA50QRhJvy0GQN" +
						"MTw9fWloD379X8yummGqH/zTaLy5xWGEIcqg4A==");
		assertFalse(result);
		
		result = PonyDownloader.verifyData(new ByteArrayInputStream(
				Base64.decode("tQaFHvciCjpTVxM+n8vksAxLr53tAoyRf+fp0UJEgsAyzZpY5FLkqimpByc20sQaybSdEGg8QPnQzs0HkHwpHQ==", Base64.DEFAULT)),
				"cLcfvAoo2q3aCwhuWo5wYy/5k5EZa/EQZdtnt3YQwiesXAI+BCCaXvA7+iUPdqRIJZKqehpUKZju" +
						"VgWpeJi53o8g24zZjkM3FpkxNzbv+orALg1NmN0izBvYFFIiNywzEpxziXe+fDt/Rnc61dMqvWSe" +
						"PbOQl0HSuKnDgMr2eEZaFbqVN60usXskjNFRBeoeH5jfC4BeiF1gxFBbRfPejfdoZxTZuufSEMVm" +
						"baixL45HGF+0Q11838vRKlYEWYl4CsM6j9g8SuLoM/WQ+lSZ++bOLEq2RbsAD5svqcskt+lVT48E" +
						"90AqlMbgvg4AHoe6ROA6lLH+swzrxoHUWZKFVw==");
		assertTrue(result);
		
		result = PonyDownloader.verifyData(new ByteArrayInputStream(
				Base64.decode("QgDD8BrdLDcWRI+pEhvm9kdwXn3Dc2+bjKSUpacPSMlWsIjwoWMpYljDy1biPCc8aa6zvwxFZHKQxRYuT3lMDw==", Base64.DEFAULT)),
				"cLcfvAoo2q3aCwhuWo5wYy/5k5EZa/EQZdtnt3YQwiesXAI+BCCaXvA7+iUPdqRIJZKqehpUKZju" +
						"VgWpeJi53o8g24zZjkM3FpkxNzbv+orALg1NmN0izBvYFFIiNywzEpxziXe+fDt/Rnc61dMqvWSe" +
						"PbOQl0HSuKnDgMr2eEZaFbqVN60usXskjNFRBeoeH5jfC4BeiF1gxFBbRfPejfdoZxTZuufSEMVm" +
						"baixL45HGF+0Q11838vRKlYEWYl4CsM6j9g8SuLoM/WQ+lSZ++bOLEq2RbsAD5svqcskt+lVT48E" +
						"90AqlMbgvg4AHoe6ROA6lLH+swzrxoHUWZKFVw==");
		assertFalse(result);
	}
	
	public void testFetchAnimations() throws Exception
	{
		final List<PonyAnimationListing> listings = PonyDownloader.fetchListings();
		assertEquals(2, listings.size());
		assertEquals("lyra", listings.get(0).getId());
		assertEquals("https://animations.pinkie-live.googlecode.com/git/lyra.zip", listings.get(0).getUrl());
		assertEquals("Lyra", listings.get(0).getName());
		assertEquals(Long.valueOf(1L), listings.get(0).getVersion());
		assertEquals("JaKenoGefQJn8DHAodwWve07grFNtxD6xocwLHV2M5yLcMXbbcGHOQseM0FMjf6iqCfSBeqn0wa+d4rh6PluUKqTwuHS129BBn9FRnDTmLY1WnjL/fV5n9BQCMCb3pA9ZZMotpp26VhBU3GfvEulIgp+l2ZSkUE1LCQWiOmGl2kYfZvRfezhlJhddteXLsJL1vS9cH16CJj1HmBaV+00FNUwxkbhCFk/ZpIKuAjUxY4azRUt6H11XAfXdkluEFDryRApD2tRImmIHGoNaOnvHc7Z0TBN52sTuUlOxXAG7WENVJEBicC4wI8DQm81sYrEqO8Zr9u4BX1DBGYmxxfpqQ==", listings.get(0).getChecksum());
		
		assertEquals("rarity", listings.get(1).getId());
		assertEquals("https://animations.pinkie-live.googlecode.com/git/rarity.zip", listings.get(1).getUrl());
		assertEquals("Rarity", listings.get(1).getName());
		assertEquals(Long.valueOf(1L), listings.get(1).getVersion());
		assertEquals("Nb0AbQPFVBQ6t32Q1W8umi1WzgVq0pHzCt4gjXuDTewYz7UgrpPf/4ErnojV96SSreoCEifqp0/IGYAvQ6Y6Zd8YMfJhb5Xgw+jGjcvIpRKqvTiqbH7qU6z0oci/bbHf32dmn5WiBACSb9xEwg43snrraIuVpKGpsXGofvbE/i/OQ7rP6YlM7xilAyIAmvKJQuB4RDERn156ojS2U86XTCisONqt1GECh9XywLR2nlyeWTKiPnEclS4re/uBLcIqe2t/lej4P5CoWSOFkMlKq+6H6wilsKuQExVqBGZTJxehTVEJri/p/M+QCSC5+RQDccWoTN4rtVhBEX+bdwIZOA==", listings.get(1).getChecksum());
		
	}
}
