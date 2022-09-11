package com.diamondq.cachly.base;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;

import java.util.Map;

/**
 * This base class is used to implement a CacheLoader that returns a VALUE from a KEY when there is a CacheLoader that
 * returns a Map<KEY, VALUE>
 *
 * @param <VALUE> the VALUE type
 */
public class AbstractMapChildCacheLoader<VALUE> extends AbstractCacheLoader<VALUE> {

  protected final Key<Map<String, VALUE>> mMapKey;

  /**
   * Constructor
   *
   * @param pKey the key that is returned
   * @param pSupportsNull whether null is a valid value
   * @param pHelp help describing this loader
   * @param pMapKey the Key to load the Map<String, VALUE> data
   */
  public AbstractMapChildCacheLoader(Key<VALUE> pKey, boolean pSupportsNull, String pHelp,
    Key<Map<String, VALUE>> pMapKey) {
    super(pKey, pSupportsNull, pHelp);
    mMapKey = pMapKey;
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.AccessContext,
   *   com.diamondq.cachly.Key, com.diamondq.cachly.CacheResult)
   */
  @Override
  public void load(Cache pCache, AccessContext pAccessContext, Key<VALUE> pKey, CacheResult<VALUE> pResult) {

    /* Query for the map */

    Map<String, VALUE> map = pCache.get(pAccessContext, mMapKey);

    /* Attempt to get the requested key from the map. This may return NULL */

    VALUE bo = map.get(pKey.getKey());

    if ((bo == null) && (mCacheLoaderInfo.supportsNull)) {

      /* Since the value can be null, we need to differentiate from a NULL value and a non-existing value */

      if (map.containsKey(pKey.getKey())) pResult.setNullableVaue(null);
      else pResult.setNotFound();
    } else pResult.setValue(bo);
  }
}
