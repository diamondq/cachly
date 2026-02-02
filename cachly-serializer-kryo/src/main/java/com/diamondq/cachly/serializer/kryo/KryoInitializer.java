package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

/**
 * Defines initialization steps for Kryo
 */
@SuppressWarnings({ "InterfaceMayBeAnnotatedFunctional", "ClassNamePrefixedWithPackageName" })
public interface KryoInitializer {

  /**
   * Called to initialize Kryo
   *
   * @param pKryo the Kryo instance to initialize
   */
  void initialize(Kryo pKryo);

}
