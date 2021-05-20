package com.diamondq.cachly.micronaut;

import java.time.Duration;

public interface ExpiryHandler {

  public void markForExpiry(String pKey, Duration pOverrideExpiry);

  public void invalidate(String pKey);

  public void invalidateAll();

}
