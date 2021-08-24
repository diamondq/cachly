package com.diamondq.cachly.base;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.common.lambda.interfaces.Predicate2;

import java.util.Collection;

/**
 * This base class is used to implement a CacheLoader that returns a VALUE from a KEY when there is a CacheLoader that
 * returns a Collection<VALUE>
 *
 * @param <VALUE> the VALUE type
 * @param <COLL> the collection type
 */
public class AbstractCollectionChildCacheLoader<VALUE, COLL extends Collection<VALUE>>
  extends AbstractCacheLoader<VALUE> {

  protected final Key<COLL>                 mListKey;

  protected final Predicate2<String, VALUE> mMatcher;

  /**
   * Constructor
   *
   * @param pKey the key that is returned
   * @param pSupportsNull whether null is a valid value
   * @param pHelp help describing this loader
   * @param pListKey the Key to load the Collection<VALUE> data
   * @param pMatcher the matcher to find the requested entry
   */
  public AbstractCollectionChildCacheLoader(Key<VALUE> pKey, boolean pSupportsNull, String pHelp, Key<COLL> pListKey,
    Predicate2<String, VALUE> pMatcher) {
    super(pKey, pSupportsNull, pHelp);
    mListKey = pListKey;
    mMatcher = pMatcher;
  }

  /**
   * @see com.diamondq.cachly.CacheLoader#load(com.diamondq.cachly.Cache, com.diamondq.cachly.Key,
   *      com.diamondq.cachly.CacheResult)
   */
  @Override
  public void load(Cache pCache, Key<VALUE> pKey, CacheResult<VALUE> pResult) {

    /* Query for the map */

    Collection<VALUE> coll = pCache.get(mListKey);

    /* Attempt to get the requested key from the collection. */

    String key = pKey.getKey();
    for (VALUE entry : coll) {
      if (mMatcher.test(key, entry) == true) {
        pResult.setNullableVaue(entry);
        return;
      }
    }

    /* There was no match */

    pResult.setNotFound();
  }
}
