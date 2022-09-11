package com.diamondq.cachly.test;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.KeyBuilder;
import com.diamondq.cachly.KeyPlaceholder;
import com.diamondq.common.types.Types;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@MicronautTest
public class TestDefaults {

  public static class Keys {

    private static class Strings {
      public static final String PARTIAL_THIS = "this";

      public static final String PARTIAL_ACTUAL = "actual";

      public static final String PARTIAL_ORG = "org";
    }

    public static final Key<@Nullable Void> THIS = KeyBuilder.of(Strings.PARTIAL_THIS, Types.VOID);

    public static final Key<@Nullable Void> ACTUAL = KeyBuilder.of(Strings.PARTIAL_ACTUAL, Types.VOID);

    public static final Key<String> ACTUAL_ORG = KeyBuilder.from(ACTUAL,
      KeyBuilder.of(Strings.PARTIAL_ORG, Types.STRING)
    );

    public static final KeyPlaceholder<String> ORG = KeyBuilder.placeholder(Strings.PARTIAL_ORG,
      Types.STRING,
      ACTUAL_ORG
    );

    public static final Key<String> THIS_ORG = KeyBuilder.from(THIS, ORG);

  }

  @Inject Cache cache;

  @BeforeEach
  public void before() {
    cache.invalidateAll(cache.createAccessContext(null));
  }

  @Test
  void test() {
    AccessContext ac = cache.createAccessContext(null);
    String r = cache.get(ac, Keys.THIS_ORG);
    assertEquals("actual_1", r);
    String r1 = cache.get(ac, Keys.THIS_ORG);
    assertEquals("actual_1", r1);
    cache.invalidate(ac, Keys.ACTUAL_ORG);
    String r2 = cache.get(ac, Keys.THIS_ORG);
    assertEquals("actual_2", r2);
  }

}
