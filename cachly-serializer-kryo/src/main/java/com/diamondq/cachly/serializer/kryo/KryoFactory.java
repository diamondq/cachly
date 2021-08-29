package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;

@Factory
public class KryoFactory {
  @Singleton
  public @Named("cachly") Kryo createKryo(List<KryoInitializer> pInitializers) {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    kryo.setReferences(true);
    for (KryoInitializer ki : pInitializers)
      ki.initialize(kryo);
    return kryo;
  }
}
