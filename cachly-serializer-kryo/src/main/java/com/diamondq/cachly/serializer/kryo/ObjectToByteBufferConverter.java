package com.diamondq.cachly.serializer.kryo;

import com.diamondq.common.converters.AbstractConverter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.ByteBuffer;

@Singleton
@javax.inject.Singleton
public class ObjectToByteBufferConverter extends AbstractConverter<Object, ByteBuffer> {
  private final Kryo mKryo;

  @Inject
  @javax.inject.Inject
  public ObjectToByteBufferConverter(@javax.inject.Named("cachly") @Named("cachly") Kryo pKryo) {
    super(Object.class, ByteBuffer.class, "kryo");
    mKryo = pKryo;
  }

  @Override
  public ByteBuffer convert(Object pInput) {
    try (Output output = new Output(1, Integer.MAX_VALUE)) {
      mKryo.writeClassAndObject(output, pInput);
      output.flush();
      return ByteBuffer.wrap(output.getBuffer());
    }
  }
}
