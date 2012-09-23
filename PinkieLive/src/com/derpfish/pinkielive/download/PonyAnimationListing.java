package com.derpfish.pinkielive.download;

public class PonyAnimationListing
{
	private String name;
	private String url;
	private String id;
	private Long version;
	private String checksum;
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public Long getVersion()
	{
		return version;
	}
	public void setVersion(Long version)
	{
		this.version = version;
	}
	public String getChecksum()
	{
		return checksum;
	}
	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}
}
