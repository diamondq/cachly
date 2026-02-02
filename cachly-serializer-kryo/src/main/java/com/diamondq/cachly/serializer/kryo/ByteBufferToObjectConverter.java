package com.diamondq.cachly.serializer.kryo;

import com.diamondq.common.converters.AbstractConverter;
import com.diamondq.common.converters.Converter;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Converter from a ByteBuffer to an Object using Kryo
 */
@Singleton
@Component(service = Converter.class)
public class ByteBufferToObjectConverter extends AbstractConverter<ByteBuffer, Object> {

  /**
   * The Kryo instance
   */
  @Reference
  protected @Nullable Kryo mKryo;

  /**
   * CDI-based constructor
   *
   * @param pKryo the Kryo instance
   */
  @Inject
  public ByteBufferToObjectConverter(@Named("cachly") Kryo pKryo) {
    super(ByteBuffer.class, Object.class, "kryo");
    mKryo = pKryo;
  }

  /**
   * OSGi-based constructor
   */
  public ByteBufferToObjectConverter() {
    super(ByteBuffer.class, Object.class, "kryo");
    mKryo = null;
  }

  @SuppressWarnings("null")
  @Override
  public Object convert(ByteBuffer pInput) {
    try (ByteBufferInput input = new ByteBufferInput(pInput)) {
      return Objects.requireNonNull(mKryo).readClassAndObject(input);
    }
  }
}
