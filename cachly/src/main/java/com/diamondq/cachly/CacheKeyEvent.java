package com.diamondq.cachly;

/**
 * Provided during key events indicating what triggered the event callback
 */
public enum CacheKeyEvent {

  /**
   * The key was added for the first time
   */
  ADDED,

  /**
   * The key value was modified
   */
  MODIFIED,

  /**
   * The key was removed (likely during an invalidating call)
   */
  REMOVED

}
