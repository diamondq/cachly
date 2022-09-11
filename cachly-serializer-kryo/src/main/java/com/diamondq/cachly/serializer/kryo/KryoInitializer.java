package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

public interface KryoInitializer {

  void initialize(Kryo pKryo);

}
