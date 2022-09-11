package com.diamondq.cachly;

public class CacheLoaderInfo<O> {

  public final Key<O>         key;

  public final boolean        supportsNull;

  public final String         help;

  public final CacheLoader<O> loader;

  public CacheLoaderInfo(Key<O> pKey, boolean pSupportsNull, String pHelp, CacheLoader<O> pLoader) {
      key = pKey;
    supportsNull = pSupportsNull;
    help = pHelp;
    loader = pLoader;
  }

}
