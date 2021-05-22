package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

import javax.inject.Named;
import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;

@Factory
public class KryoFactory {
  @Singleton
  public @Named("cachly") Kryo createKryo() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    return kryo;
  }
}
