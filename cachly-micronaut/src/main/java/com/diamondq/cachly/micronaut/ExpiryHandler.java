package com.diamondq.cachly.micronaut;

import java.time.Duration;

public interface ExpiryHandler {

  void markForExpiry(String pKey, Duration pOverrideExpiry);

  void invalidate(String pKey);

  void invalidateAll();

}
