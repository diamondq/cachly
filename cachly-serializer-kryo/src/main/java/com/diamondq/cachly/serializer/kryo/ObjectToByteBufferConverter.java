package com.diamondq.cachly.serializer.kryo;

import com.diamondq.common.converters.AbstractConverter;
import com.diamondq.common.converters.Converter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Converts an object into a byte array using the Kryo serializer
 */
@Singleton
@Component(service = Converter.class)
public class ObjectToByteBufferConverter extends AbstractConverter<Object, ByteBuffer> {
  /**
   * Kryo object
   */
  @Reference
  protected @Nullable Kryo mKryo;

  /**
   * CDI constructor
   *
   * @param pKryo the kryo instance
   */
  @Inject
  public ObjectToByteBufferConverter(@Named("cachly") Kryo pKryo) {
    super(Object.class, ByteBuffer.class, "kryo");
    mKryo = pKryo;
  }

  /**
   * OSGi-constructor
   */
  public ObjectToByteBufferConverter() {
    super(Object.class, ByteBuffer.class, "kryo");
    mKryo = null;
  }

  @Override
  public ByteBuffer convert(Object pInput) {
    try (Output output = new Output(1, Integer.MAX_VALUE)) {
      Objects.requireNonNull(mKryo).writeClassAndObject(output, pInput);
      output.flush();
      return ByteBuffer.wrap(output.getBuffer());
    }
  }
}
