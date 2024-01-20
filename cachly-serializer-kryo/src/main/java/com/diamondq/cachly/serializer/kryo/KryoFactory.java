package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.List;

@SuppressWarnings("ClassNamePrefixedWithPackageName")
@Factory
public class KryoFactory {
  @SuppressWarnings("MethodMayBeStatic")
  @Singleton
  @Named("cachly")
  public Kryo createKryo(List<KryoInitializer> pInitializers) {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    kryo.setReferences(true);
    for (KryoInitializer ki : pInitializers)
      ki.initialize(kryo);
    return kryo;
  }
}
