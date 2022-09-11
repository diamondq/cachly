package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

@SuppressWarnings({ "InterfaceMayBeAnnotatedFunctional", "ClassNamePrefixedWithPackageName" })
public interface KryoInitializer {

  void initialize(Kryo pKryo);

}
