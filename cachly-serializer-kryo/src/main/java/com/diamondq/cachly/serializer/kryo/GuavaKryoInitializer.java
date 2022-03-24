package com.diamondq.cachly.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;

import jakarta.inject.Singleton;

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

@Singleton
@javax.inject.Singleton
public class GuavaKryoInitializer implements KryoInitializer {

  /**
   * @see com.diamondq.cachly.serializer.kryo.KryoInitializer#initialize(com.esotericsoftware.kryo.Kryo)
   */
  @Override
  public void initialize(Kryo pKryo) {
    boolean hasGuava;
    try {
      Class.forName("com.google.common.collect.ImmutableMap");
      hasGuava = true;
    }
    catch (@SuppressWarnings("unused") ClassNotFoundException ex) {
      hasGuava = false;
    }
    if (hasGuava) {
      // guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, ImmutableTable, ReverseList,
      // UnmodifiableNavigableSet
      ImmutableListSerializer.registerSerializers(pKryo);
      ImmutableSetSerializer.registerSerializers(pKryo);
      ImmutableMapSerializer.registerSerializers(pKryo);
      ImmutableMultimapSerializer.registerSerializers(pKryo);
      ImmutableTableSerializer.registerSerializers(pKryo);
      ReverseListSerializer.registerSerializers(pKryo);
      UnmodifiableNavigableSetSerializer.registerSerializers(pKryo);
      // guava ArrayListMultimap, HashMultimap, LinkedHashMultimap, LinkedListMultimap, TreeMultimap, ArrayTable,
      // HashBasedTable, TreeBasedTable
      ArrayListMultimapSerializer.registerSerializers(pKryo);
      HashMultimapSerializer.registerSerializers(pKryo);
      LinkedHashMultimapSerializer.registerSerializers(pKryo);
      LinkedListMultimapSerializer.registerSerializers(pKryo);
      TreeMultimapSerializer.registerSerializers(pKryo);
      ArrayTableSerializer.registerSerializers(pKryo);
      HashBasedTableSerializer.registerSerializers(pKryo);
      TreeBasedTableSerializer.registerSerializers(pKryo);
    }
  }

}
