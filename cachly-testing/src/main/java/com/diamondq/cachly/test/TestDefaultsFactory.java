package com.diamondq.cachly.test;

import com.diamondq.cachly.CacheLoader;
import com.diamondq.cachly.CacheLoaderFactoryHelper;
import com.diamondq.cachly.test.TestDefaults.Keys;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({ "MethodMayBeStatic", "ClassNamePrefixedWithPackageName" })
@Factory
public class TestDefaultsFactory {

  @Singleton
  @Named("this")
  public CacheLoader<@Nullable Void> thisFactory() {
    return CacheLoaderFactoryHelper.ofNull(Keys.THIS, "");
  }

  @Singleton
  @Named("actual")
  public CacheLoader<@Nullable Void> actualFactory() {
    return CacheLoaderFactoryHelper.ofNull(Keys.ACTUAL, "");
  }

  @Singleton
  @Named("thisOrg")
  public CacheLoader<String> thisOrgFactory() {
    return CacheLoaderFactoryHelper.of(Keys.THIS_ORG, false, "", (cache, key, result) -> result.setValue(key.getKey()));
  }

  @Singleton
  @Named("actualOrg")
  public CacheLoader<String> actualOrgFactory() {
    AtomicInteger counter = new AtomicInteger(0);
    return CacheLoaderFactoryHelper.of(Keys.ACTUAL_ORG,
      false,
      "",
      (cache, key, result) -> result.setValue("actual_" + counter.incrementAndGet())
    );
  }
}