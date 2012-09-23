package com.derpfish.pinkielive.download;

import com.derpfish.pinkielive.animation.PonyAnimation;

public class PonyAnimationContainer
{
	private String id;
	private String name;
	private PonyAnimation ponyAnimation;
	
	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public PonyAnimation getPonyAnimation()
	{
		return ponyAnimation;
	}
	
	public void setPonyAnimation(PonyAnimation ponyAnimation)
	{
		this.ponyAnimation = ponyAnimation;
	}
}
