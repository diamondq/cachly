package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

import java.util.List;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;

@Factory
public class KryoFactory {
  @Singleton
  @javax.inject.Singleton
  @Named("cachly")
  @javax.inject.Named("cachly")
  public Kryo createKryo(List<KryoInitializer> pInitializers) {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    kryo.setReferences(true);
    for (KryoInitializer ki : pInitializers)
      ki.initialize(kryo);
    return kryo;
  }
}
