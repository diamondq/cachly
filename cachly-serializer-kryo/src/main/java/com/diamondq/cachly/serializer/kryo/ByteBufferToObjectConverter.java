package com.diamondq.cachly.serializer.kryo;

import com.diamondq.common.converters.AbstractConverter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;

import java.nio.ByteBuffer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
      Object obj = mKryo.readClassAndObject(input);
      return obj;
    }
  }
}
