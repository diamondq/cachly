package com.diamondq.cachly.serializer.kryo;

import com.diamondq.common.converters.AbstractConverter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.nio.ByteBuffer;

@Singleton
public class ByteBufferToObjectConverter extends AbstractConverter<ByteBuffer, Object> {

  private final Kryo mKryo;

  @Inject
  public ByteBufferToObjectConverter(@Named("cachly") Kryo pKryo) {
    super(ByteBuffer.class, Object.class, "kryo");
    mKryo = pKryo;
  }

  /**
   * @see com.diamondq.common.converters.Converter#convert(java.lang.Object)
   */
  @SuppressWarnings("null")
  @Override
  public Object convert(ByteBuffer pInput) {
    try (ByteBufferInput input = new ByteBufferInput(pInput)) {
      return mKryo.readClassAndObject(input);
    }
  }
}
