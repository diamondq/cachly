package com.diamondq.cachly.test;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderFactoryHelper;
import com.diamondq.cachly.test.TestDefaults.Keys;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Named;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.micronaut.context.annotation.Factory;

@Factory
public class TestDefaultsFactory {

  @Singleton
  public @Named("this") CacheLoader<@Nullable Void> thisFactory() {
    return CacheLoaderFactoryHelper.ofNull(Keys.THIS, "");
  }

  @Singleton
  public @Named("actual") CacheLoader<@Nullable Void> actualFactory() {
    return CacheLoaderFactoryHelper.ofNull(Keys.ACTUAL, "");
  }

  @Singleton
  public @Named("thisOrg") CacheLoader<String> thisOrgFactory() {
    return CacheLoaderFactoryHelper.of(Keys.THIS_ORG, false, "", (c, k, r) -> r.setValue(k.getKey()));
  }

  @Singleton
  public @Named("actualOrg") CacheLoader<String> actualOrgFactory() {
    AtomicInteger counter = new AtomicInteger(0);
    return CacheLoaderFactoryHelper.of(Keys.ACTUAL_ORG, false, "",
      (c, k, r) -> r.setValue("actual_" + String.valueOf(counter.incrementAndGet())));
  }
}