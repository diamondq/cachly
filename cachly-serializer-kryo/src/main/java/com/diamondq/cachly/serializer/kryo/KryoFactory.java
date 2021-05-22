package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

import java.lang.reflect.InvocationHandler;
import java.util.GregorianCalendar;

import javax.inject.Named;
import javax.inject.Singleton;

import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ArrayTableSerializer;
import de.javakaffee.kryoserializers.guava.HashBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableTableSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeBasedTableSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import io.micronaut.context.annotation.Factory;

@Factory
public class KryoFactory {
  @Singleton
  public @Named("cachly") Kryo createKryo() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
    kryo.register(InvocationHandler.class, new JdkProxySerializer());
    UnmodifiableCollectionsSerializer.registerSerializers(kryo);
    SynchronizedCollectionsSerializer.registerSerializers(kryo);
    boolean hasGuava;
    try {
      Class.forName("com.google.common.collect.ImmutableMap");
      hasGuava = true;
    }
    catch (ClassNotFoundException ex) {
      hasGuava = false;
    }
    if (hasGuava) {
      // guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, ImmutableTable, ReverseList,
      // UnmodifiableNavigableSet
      ImmutableListSerializer.registerSerializers(kryo);
      ImmutableSetSerializer.registerSerializers(kryo);
      ImmutableMapSerializer.registerSerializers(kryo);
      ImmutableMultimapSerializer.registerSerializers(kryo);
      ImmutableTableSerializer.registerSerializers(kryo);
      ReverseListSerializer.registerSerializers(kryo);
      UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
      // guava ArrayListMultimap, HashMultimap, LinkedHashMultimap, LinkedListMultimap, TreeMultimap, ArrayTable,
      // HashBasedTable, TreeBasedTable
      ArrayListMultimapSerializer.registerSerializers(kryo);
      HashMultimapSerializer.registerSerializers(kryo);
      LinkedHashMultimapSerializer.registerSerializers(kryo);
      LinkedListMultimapSerializer.registerSerializers(kryo);
      TreeMultimapSerializer.registerSerializers(kryo);
      ArrayTableSerializer.registerSerializers(kryo);
      HashBasedTableSerializer.registerSerializers(kryo);
      TreeBasedTableSerializer.registerSerializers(kryo);
    }
    return kryo;
  }
}
