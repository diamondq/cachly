package com.diamondq.cachly.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.diamondq.cachly.AccessContext;

public final class AccessContextImpl implements AccessContext
{

	private final Map<Class<?>, Object> mData;

	public AccessContextImpl(Map<Class<?>, Object> pData)
	{
		mData = Collections.unmodifiableMap(new HashMap<>(pData));
	}

	@Override
	public Map<Class<?>, Object> getData()
	{
		return mData;
	}
}
