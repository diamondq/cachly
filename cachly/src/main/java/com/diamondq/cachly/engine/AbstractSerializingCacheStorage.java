package com.diamondq.cachly.engine;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.engine.AbstractSerializingCacheStorage.SerializedKeyValuePair;
import com.diamondq.cachly.impl.CompositeKey;
import com.diamondq.cachly.impl.ResolvedAccessContextPlaceholder;
import com.diamondq.cachly.impl.ResolvedKeyPlaceholder;
import com.diamondq.cachly.impl.StaticAccessContextPlaceholder;
import com.diamondq.cachly.impl.StaticCacheResult;
import com.diamondq.cachly.impl.StaticKey;
import com.diamondq.cachly.impl.StaticKeyPlaceholder;
import com.diamondq.cachly.impl.StaticKeyPlaceholderWithDefault;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.common.converters.ConverterManager;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This base class provides functionality for storing the richer data set that Cachely has
 *
 * @param <CACHE> the underlying cache type
 * @param <SER_KEY> the serialized key type
 * @param <SER_VALUE> the serialized value type
 */
public abstract class AbstractSerializingCacheStorage<@NonNull CACHE, @NonNull SER_KEY, @NonNull SER_VALUE>
  extends AbstractCommonCacheStorage<CACHE, SER_KEY, SER_VALUE, SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> {

  public static final byte  SERIALIZATION_VERSION          = 1;

  public static final int   FLAG_ISNULL                    = 0x01;

  private static final byte TYPE_CLASS                     = 1;

  private static final byte TYPE_PARAMETERIZED             = 2;

  private static final byte TYPE_GENERIC_ARRAY             = 3;

  private static final byte TYPE_VARIABLE                  = 4;

  private static final byte TYPE_WILDCARD                  = 5;

  private static final byte PART_TYPE_ACCESS_CONTEXT       = 1;

  private static final byte PART_TYPE_PLACEHOLDER          = 2;

  private static final byte PART_TYPE_PLACEHOLDER_DEFAULTS = 3;

  private static class NULL_TYPE_CLASS {
    // empty
  }

  private static final Type NULL_TYPE = NULL_TYPE_CLASS.class;

  protected static class SerializedKeyValuePair<PAIR_CACHE, PAIR_SER_KEY, PAIR_SER_VALUE>
    extends CommonKeyValuePair<PAIR_CACHE, PAIR_SER_KEY, PAIR_SER_VALUE> {

    public SerializedKeyValuePair(PAIR_CACHE pCache, PAIR_SER_KEY pSerKey, @Nullable Key<?> pKey,
      @Nullable PAIR_SER_VALUE pSerValue, @Nullable Duration pExpiresIn) {
      super(pCache, pSerKey, pKey, pSerValue, pExpiresIn);
    }

  }

  protected final ConverterManager             mConverterManager;

  protected final Class<SER_KEY>               mSerKeyClass;

  protected final Class<SER_VALUE>             mSerValueClass;

  protected final ConcurrentMap<String, Short> mStringToShort;

  protected final ConcurrentMap<Short, String> mShortToString;

  protected final AtomicInteger                mStringCounter;

  protected final String                       mStringPrefix;

  protected final int                          mStringPrefixLen;

  protected final ConcurrentMap<Type, Short>   mTypeToShort;

  protected final ConcurrentMap<Short, Type>   mShortToType;

  protected final AtomicInteger                mTypeCounter;

  protected final String                       mTypePrefix;

  protected final int                          mTypePrefixLen;

  protected final ConcurrentMap<Key<?>, Short> mKeyToShort;

  protected final ConcurrentMap<Short, Key<?>> mShortToKey;

  protected final AtomicInteger                mKeyCounter;

  protected final String                       mKeyPrefix;

  protected final int                          mKeyPrefixLen;

  public AbstractSerializingCacheStorage(ConverterManager pConverterManager, CACHE pPrimaryCache,
    @Nullable CACHE pMetaCache, Class<SER_KEY> pSerKeyClass, Class<SER_VALUE> pSerValueClass,
    @Nullable String pStringPrefix, @Nullable String pTypePrefix, @Nullable String pKeyPrefix,
    @Nullable String pValuePrefix, @Nullable Function<String, @NonNull SER_KEY> pKeySerializer,
    @Nullable Function<@NonNull SER_KEY, String> pKeyDeserializer) {
    super(pPrimaryCache, pMetaCache, pValuePrefix != null ? pValuePrefix : "p/", pKeySerializer, pKeyDeserializer);
    mConverterManager = pConverterManager;
    mSerKeyClass = pSerKeyClass;
    mSerValueClass = pSerValueClass;
    mStringToShort = new ConcurrentHashMap<>();
    mShortToString = new ConcurrentHashMap<>();
    mStringCounter = new AtomicInteger();
    mStringPrefix = pStringPrefix != null ? pStringPrefix : "s/";
    mStringPrefixLen = mStringPrefix.length();
    mTypeToShort = new ConcurrentHashMap<>();
    mShortToType = new ConcurrentHashMap<>();
    mTypeCounter = new AtomicInteger();
    mTypePrefix = pTypePrefix != null ? pTypePrefix : "t/";
    mTypePrefixLen = mTypePrefix.length();
    mKeyToShort = new ConcurrentHashMap<>();
    mShortToKey = new ConcurrentHashMap<>();
    mKeyCounter = new AtomicInteger();
    mKeyPrefix = pKeyPrefix != null ? pKeyPrefix : "k/";
    mKeyPrefixLen = mTypePrefix.length();
  }

  /**
   * Query the underlying cache to retrieve the meta data info
   */
  protected void init() {
    Map<Short, ByteBuffer> temporaryKeys = new HashMap<>();
    Map<Short, ByteBuffer> temporaryTypes = new HashMap<>();
    streamMetaEntries().forEach((entry) -> {
      SER_KEY key = entry.getKey();
      String keyStr = (mKeyDeserializer != null ? mKeyDeserializer.apply(key) : (String) key);
      if (keyStr.startsWith(mStringPrefix)) {
        short id = Short.parseShort(keyStr.substring(mStringPrefixLen));
        SER_VALUE value = entry.getValue();
        ByteBuffer valueBuffer = deserializeValue(value);

        /* The contents of a String is just the UTF-8 bytes */

        String valueStr = new String(valueBuffer.array(), StandardCharsets.UTF_8);
        mStringToShort.put(valueStr, id);
        mShortToString.put(id, valueStr);
        if (id > mStringCounter.get())
          mStringCounter.set(id);
      }
      else if (keyStr.startsWith(mTypePrefix)) {
        short id = Short.parseShort(keyStr.substring(mTypePrefixLen));
        SER_VALUE value = entry.getValue();
        ByteBuffer valueBuffer = deserializeValue(value);

        /* The type buffer starts with a type identifier */

        int typeType = valueBuffer.get();
        Type type;
        if (typeType == TYPE_CLASS) {

          /* If it's a CLASS type, then it's just the full classname */

          String className = StandardCharsets.UTF_8.decode(valueBuffer.slice()).toString();
          try {
            type = ClassUtils.getClass(className);
          }
          catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Unrecognized class (" + className + ")", ex);
          }
        }
        else {
          temporaryTypes.put(id, valueBuffer);
          return;
        }
        mShortToType.put(id, type);
        if (id > mTypeCounter.get())
          mTypeCounter.set(id);
      }
      else if (keyStr.startsWith(mKeyPrefix)) {
        short id = Short.parseShort(keyStr.substring(mKeyPrefixLen));
        SER_VALUE value = entry.getValue();
        ByteBuffer valueBuffer = deserializeValue(value);

        /*
         * Because all keys are being read in sequentially, we may not have the type entries needed to resolve the key
         * yet. Therefore, just remember it, and do it later
         */

        temporaryKeys.put(id, valueBuffer);
      }
      else
        return;
    });

    /* Handle all the saved types. NOTE: Again, due to ordering, this list may need to be processed multiple times */

    Map<Short, ByteBuffer> currentTypes = temporaryTypes;
    while (currentTypes.isEmpty() == false) {
      Map<Short, ByteBuffer> delayed = new HashMap<>();
      TemporaryTypeLoop: for (Map.Entry<Short, ByteBuffer> entry : currentTypes.entrySet()) {
        short id = entry.getKey();
        ByteBuffer valueBuffer = entry.getValue();
        valueBuffer.rewind();

        /* The type buffer starts with a type identifier */

        int typeType = valueBuffer.get();
        Type type;
        if (typeType == TYPE_PARAMETERIZED) {

          /* Check if the owner and raw types are available */

          short ownerTypeId = valueBuffer.getShort();
          short rawTypeId = valueBuffer.getShort();
          if (mShortToType.containsKey(ownerTypeId) == false) {
            delayed.put(id, valueBuffer);
            continue;
          }
          if (mShortToType.containsKey(rawTypeId) == false) {
            delayed.put(id, valueBuffer);
            continue;
          }

          Type ownerType = decompressType(ownerTypeId);
          Class<?> rawType = (Class<?>) Objects.requireNonNull(decompressType(rawTypeId));
          short actualTypeArgumentsLen = valueBuffer.getShort();
          @SuppressWarnings("null")
          @NonNull
          Type @NonNull [] actualTypeArguments = new Type[actualTypeArgumentsLen];
          for (short i = 0; i < actualTypeArgumentsLen; i++) {
            short actualTypeId = valueBuffer.getShort();
            if (mShortToType.containsKey(actualTypeId) == false) {
              delayed.put(id, valueBuffer);
              continue TemporaryTypeLoop;
            }
            actualTypeArguments[i] = Objects.requireNonNull(decompressType(actualTypeId));
          }
          if (ownerType != null)
            type = TypeUtils.parameterizeWithOwner(ownerType, rawType, actualTypeArguments);
          else
            type = TypeUtils.parameterize(rawType, actualTypeArguments);
        }
        else if (typeType == TYPE_GENERIC_ARRAY) {
          short gaTypeId = valueBuffer.getShort();
          if (mShortToType.containsKey(gaTypeId) == false) {
            delayed.put(id, valueBuffer);
            continue TemporaryTypeLoop;
          }
          Type gaType = Objects.requireNonNull(decompressType(gaTypeId));
          type = TypeUtils.genericArrayType(gaType);
        }
        else if (typeType == TYPE_VARIABLE) {
          throw new UnsupportedOperationException();
        }
        else if (typeType == TYPE_WILDCARD) {
          short lowerBoundsLen = valueBuffer.getShort();
          @SuppressWarnings("null")
          @NonNull
          Type @Nullable [] lowerBounds = lowerBoundsLen == 0 ? null : new Type[lowerBoundsLen];
          short upperBoundsLen = valueBuffer.getShort();
          @SuppressWarnings("null")
          @NonNull
          Type @Nullable [] upperBounds = upperBoundsLen == 0 ? null : new Type[lowerBoundsLen];
          if ((lowerBoundsLen > 0) && (lowerBounds != null))
            for (short i = 0; i < lowerBoundsLen; i++) {
              short lowerBoundsTypeId = valueBuffer.getShort();
              if (mShortToType.containsKey(lowerBoundsTypeId) == false) {
                delayed.put(id, valueBuffer);
                continue TemporaryTypeLoop;
              }
              lowerBounds[i] = Objects.requireNonNull(decompressType(lowerBoundsTypeId));
            }
          if ((upperBoundsLen > 0) && (upperBounds != null))
            for (short i = 0; i < upperBoundsLen; i++) {
              short upperBoundsTypeId = valueBuffer.getShort();
              if (mShortToType.containsKey(upperBoundsTypeId) == false) {
                delayed.put(id, valueBuffer);
                continue TemporaryTypeLoop;
              }
              upperBounds[i] = Objects.requireNonNull(decompressType(upperBoundsTypeId));
            }
          type = TypeUtils.wildcardType().withLowerBounds(lowerBounds).withUpperBounds(upperBounds).build();
        }
        else
          throw new IllegalArgumentException("Unrecognized type (" + String.valueOf(typeType) + ")");
        mTypeToShort.put(type, id);
        mShortToType.put(id, type);
        if (id > mTypeCounter.get())
          mTypeCounter.set(id);
      }
      if (currentTypes.size() == delayed.size())
        throw new IllegalStateException("Unable to proceed decoding types");
      currentTypes = delayed;
    }

    /* Handle all the saved keys */

    for (Map.Entry<Short, ByteBuffer> entry : temporaryKeys.entrySet()) {
      short id = entry.getKey();
      ByteBuffer valueBuffer = entry.getValue();
      valueBuffer.rewind();
      int partLen = valueBuffer.limit() / 4;
      @SuppressWarnings("unchecked")
      KeySPI<Object> parts[] = new KeySPI[partLen];
      for (int i = 0; i < partLen; i++) {
        Type keyType = Objects.requireNonNull(decompressType(valueBuffer.getShort()));
        String keyBase = Objects.requireNonNull(decompressString(valueBuffer.getShort()));
        parts[i] = new StaticKey<>(keyBase, keyType);
      }
      Key<?> shortKey = new CompositeKey<Object>(parts);
      mKeyToShort.put(shortKey, id);
      mShortToKey.put(id, shortKey);
      if (id > mKeyCounter.get())
        mKeyCounter.set(id);
    }
  }

  /**
   * Convert the given key/value into a stream of SerializedKeyValuePairs
   *
   * @param <V> the value type
   * @param pKey the key
   * @param pResult the cached value
   * @return the stream
   */
  @Override
  protected <V> List<SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> serializeEntry(KeySPI<V> pKey,
    CacheResult<V> pResult) {

    /* Get the pieces that need to be serialized */

    String fullKey = pKey.toString();
    String baseKey = pKey.getFullBaseKey();
    String serializerName = pKey.getLastSerializerName();
    String serializer = Cache.DEFAULT_SERIALIZER.equals(serializerName) ? null : serializerName;
    Type outputType = pKey.getOutputType();
    Duration overrideExpiry = pResult.getOverrideExpiry();
    boolean isNull = pResult.isNull();
    @Nullable
    V value = isNull == true ? null : pResult.getValue();
    @SuppressWarnings({"null", "unchecked"})
    Class<V> valueClass = isNull == false ? (Class<V>) value.getClass() : (Class<V>) outputType;

    List<SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> listOfEntries = new ArrayList<>();

    /* Now, we need to compress the metadata into smaller pieces */

    short baseKeyId = compressString(baseKey, listOfEntries);
    short serializerId = compressString(serializer, listOfEntries);
    short outputTypeId = compressType(outputType, listOfEntries);
    short valueClassId = compressType(valueClass, listOfEntries);

    /* Now build the block */

    ByteBuffer valueBuffer;
    int valueBufferSize;

    if ((isNull == false) && (value != null)) {
      valueBuffer = mConverterManager.convert(value, ByteBuffer.class, serializer);
      valueBuffer.rewind();
      valueBufferSize = valueBuffer.limit();
    }
    else {
      valueBuffer = null;
      valueBufferSize = 0;
    }

    int size = 0;
    for (KeySPI<?> part : pKey.getParts()) {
      if (part instanceof ResolvedKeyPlaceholder) {
        ResolvedKeyPlaceholder<?> rkp = (ResolvedKeyPlaceholder<?>) part;
        KeySPI<?> placeholder = rkp.getPlaceholder();
        if (placeholder instanceof StaticKeyPlaceholder)
          size += 1;
        else if (placeholder instanceof StaticKeyPlaceholderWithDefault)
          size += 3;
        else
          throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
      }
      else if (part instanceof ResolvedAccessContextPlaceholder) {
        ResolvedAccessContextPlaceholder<?> racp = (ResolvedAccessContextPlaceholder<?>) part;
        KeySPI<?> placeholder = racp.getPlaceholder();
        if (placeholder instanceof StaticAccessContextPlaceholder)
          size += 3;
        else
          throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
      }
    }

    ByteBuffer result = ByteBuffer.allocate(valueBufferSize + 9 + size);

    /* Write the version */

    byte flags = (byte) (isNull ? FLAG_ISNULL : 0);
    result.put((byte) (SERIALIZATION_VERSION + (flags << 4)));

    /* Write the ids */

    result.putShort(baseKeyId);
    result.putShort(serializerId);
    result.putShort(outputTypeId);
    result.putShort(valueClassId);

    /* Write any of the placeholder part data */

    for (KeySPI<?> part : pKey.getParts()) {
      if (part instanceof ResolvedKeyPlaceholder) {
        ResolvedKeyPlaceholder<?> rkp = (ResolvedKeyPlaceholder<?>) part;
        KeySPI<?> placeholder = rkp.getPlaceholder();
        if (placeholder instanceof StaticKeyPlaceholder) {
          result.put(PART_TYPE_PLACEHOLDER);
        }
        else if (placeholder instanceof StaticKeyPlaceholderWithDefault) {
          StaticKeyPlaceholderWithDefault skpwd = (StaticKeyPlaceholderWithDefault) placeholder;
          result.put(PART_TYPE_PLACEHOLDER_DEFAULTS);
          result.putShort(compressKey(skpwd.getDefaultKey(), listOfEntries));
        }
        else
          throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
      }
      else if (part instanceof ResolvedAccessContextPlaceholder) {
        ResolvedAccessContextPlaceholder<?> racp = (ResolvedAccessContextPlaceholder<?>) part;
        KeySPI<?> placeholder = racp.getPlaceholder();
        if (placeholder instanceof StaticAccessContextPlaceholder) {
          StaticAccessContextPlaceholder<?, ?> sacp = (StaticAccessContextPlaceholder<?, ?>) placeholder;
          result.put(PART_TYPE_ACCESS_CONTEXT);
          result.putShort(compressType(sacp.getAccessContextValueClass(), listOfEntries));
        }
        else
          throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
      }
    }

    /* Write the data */

    if (valueBufferSize > 0)
      result.put(valueBuffer);
    result.rewind();

    /* Calculate the final primary key */

    String primaryKeyStr = mValuePrefix + fullKey;
    @SuppressWarnings("unchecked")
    SER_KEY primaryKey = (mKeySerializer != null ? mKeySerializer.apply(primaryKeyStr) : (SER_KEY) primaryKeyStr);

    /* Calculate the final value */

    SER_VALUE finalValue;
    if (mSerValueClass.equals(ByteBuffer.class)) {
      @SuppressWarnings("unchecked")
      SER_VALUE sv = (SER_VALUE) result;
      finalValue = sv;
    }
    else
      finalValue = serializeValue(result);

    listOfEntries.add(new SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>(mPrimaryCache, primaryKey, pKey, finalValue,
      overrideExpiry));
    return listOfEntries;
  }

  /**
   * Convert serialized back to a Key, CacheResult
   *
   * @param pKey the serialized key
   * @param pValue the serialized value
   * @return the Entry
   */
  @Override
  protected Map.Entry<Key<?>, CacheResult<?>> deserializeEntry(SER_KEY pKey, SER_VALUE pValue) {
    ByteBuffer buffer = deserializeValue(pValue);
    buffer.rewind();

    String fullKey = (mKeyDeserializer != null ? mKeyDeserializer.apply(pKey) : (String) pKey);

    if (mValuePrefix != null)
      fullKey = fullKey.substring(mValuePrefixLen);

    /* Get and validate the version */

    byte versionFlags = buffer.get();
    byte version = (byte) (versionFlags & 0x0F);
    if (version != SERIALIZATION_VERSION)
      throw new IllegalStateException(
        "The entry " + fullKey + " has an unrecognized serialization version (" + String.valueOf(version) + ")");

    int flags = (byte) ((versionFlags & 0xF0) >> 4);

    /* Is null? */

    boolean isNull = (flags & FLAG_ISNULL) == FLAG_ISNULL;

    /* Get the ids */

    short baseKeyId = buffer.getShort();
    short serializerId = buffer.getShort();
    short outputTypeId = buffer.getShort();
    short valueClassId = buffer.getShort();

    /* Decompress the ids */

    String baseKey = Objects.requireNonNull(decompressString(baseKeyId));
    String serializer = decompressString(serializerId);
    Type outputType = Objects.requireNonNull(decompressType(outputTypeId));
    Type valueClass = Objects.requireNonNull(decompressType(valueClassId));

    /* Now generate the Key */

    @NonNull
    String[] baseSplit = baseKey.split("/");
    int keyLen = baseSplit.length;
    @NonNull
    String[] fullSplit = fullKey.split("/");
    if (keyLen != fullSplit.length)
      throw new IllegalStateException(
        "The base key (" + baseKey + ") doesn't have the same number of parts as the full key (" + fullKey + ")");

    @SuppressWarnings({"unchecked", "null"})
    @NonNull
    KeySPI<Object>[] parts = new KeySPI[keyLen];

    for (int i = 0; i < keyLen; i++) {
      String partBaseKey = baseSplit[i];
      if (partBaseKey.startsWith("{")) {
        byte placeholderType = buffer.get();
        if (placeholderType == PART_TYPE_ACCESS_CONTEXT) {
          short accessContextValueClassId = buffer.getShort();
          Type accessContextValueClass = Objects.requireNonNull(decompressType(accessContextValueClassId));
          parts[i] = new ResolvedAccessContextPlaceholder<>(
            new StaticAccessContextPlaceholder<>(partBaseKey, (Class<?>) accessContextValueClass, outputType),
            fullSplit[i]);
        }
        else if (placeholderType == PART_TYPE_PLACEHOLDER) {
          parts[i] = new ResolvedKeyPlaceholder<>(new StaticKeyPlaceholder<>(partBaseKey, outputType), fullSplit[i]);
        }
        else if (placeholderType == PART_TYPE_PLACEHOLDER_DEFAULTS) {
          short defaultKeyId = buffer.getShort();
          @NonNull
          Key<String> defaultKey = decompressKey(defaultKeyId);
          @SuppressWarnings({"unchecked", "rawtypes"})
          KeySPI<Object> r = (KeySPI) new ResolvedKeyPlaceholder<String>(
            new StaticKeyPlaceholderWithDefault(partBaseKey, outputType, defaultKey), fullSplit[i]);
          parts[i] = r;
        }
        else
          throw new IllegalStateException(
            "The placeholder part type(" + String.valueOf(placeholderType) + ") is not recognized");
      }
      else
        parts[i] = new StaticKey<>(fullSplit[i], outputType);
    }

    Key<?> finalKey = new CompositeKey<Object>(parts);

    /* Now generate the value */

    Object value;
    if (isNull == true)
      value = null;
    else {
      ByteBuffer dataBuffer = buffer.slice();
      value = mConverterManager.convert(dataBuffer, valueClass, serializer);
    }

    CacheResult<?> finalValue = new StaticCacheResult<@Nullable Object>(value, true);

    return new SimpleEntry<>(finalKey, finalValue);
  }

  protected short compressString(@Nullable String pValue,
    List<SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> pWriteList) {
    String value = pValue == null ? "__NULL__" : pValue;
    Short id = mStringToShort.get(value);
    if (id == null) {
      synchronized (this) {
        id = mStringToShort.get(value);
        if (id == null) {
          id = (short) mStringCounter.incrementAndGet();
          mStringToShort.put(value, id);
          mShortToString.put(id, value);
          String strKey = mStringPrefix + String.valueOf(id);
          @SuppressWarnings("unchecked")
          SER_KEY idKey = (mKeySerializer != null ? mKeySerializer.apply(strKey) : (SER_KEY) strKey);
          SER_VALUE idValue = serializeValue(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
          pWriteList.add(
            new SerializedKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache, idKey, null, idValue, null));
        }
      }
    }
    return id;
  }

  protected @Nullable String decompressString(short pId) {
    String str = mShortToString.get(pId);
    if (str == null)
      throw new IllegalArgumentException("The string id (" + String.valueOf(pId) + ") is not recognized");
    if (str.equals("__NULL__"))
      return null;
    return str;
  }

  protected short compressType(@Nullable Type pType,
    List<SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> pWriteList) {
    Type type = pType == null ? NULL_TYPE : pType;
    Short id = mTypeToShort.get(type);
    if (id == null) {
      synchronized (this) {
        id = mTypeToShort.get(type);
        if (id == null) {
          id = (short) mTypeCounter.incrementAndGet();
          mTypeToShort.put(type, id);
          mShortToType.put(id, type);
          String typeKey = mTypePrefix + String.valueOf(id);
          @SuppressWarnings("unchecked")
          SER_KEY idKey = (mKeySerializer != null ? mKeySerializer.apply(typeKey) : (SER_KEY) typeKey);
          int size = 1;
          byte[] extra;
          if (type instanceof Class) {
            String className = ClassUtils.getCanonicalName((Class<?>) type);
            extra = className.getBytes(StandardCharsets.UTF_8);
            size += extra.length;
          }
          else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            short ownerTypeId = compressType(pt.getOwnerType(), pWriteList);
            short rawTypeId = compressType(pt.getRawType(), pWriteList);
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            short actualTypeArgumentsLen = (short) actualTypeArguments.length;
            ByteBuffer extraBuffer = ByteBuffer.allocate(6 + (actualTypeArgumentsLen * 2));
            extraBuffer.putShort(ownerTypeId);
            extraBuffer.putShort(rawTypeId);
            extraBuffer.putShort(actualTypeArgumentsLen);
            for (Type typeArg : actualTypeArguments) {
              short typeArgId = compressType(typeArg, pWriteList);
              extraBuffer.putShort(typeArgId);
            }
            extraBuffer.rewind();
            extra = extraBuffer.array();
            size += extra.length;
          }
          else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            short gctId = compressType(gat.getGenericComponentType(), pWriteList);
            ByteBuffer extraBuffer = ByteBuffer.allocate(2);
            extraBuffer.putShort(gctId);
            extraBuffer.rewind();
            extra = extraBuffer.array();
            size += extra.length;
          }
          else if (type instanceof TypeVariable) {
            // TypeVariable gat = (TypeVariable) type;
            throw new UnsupportedOperationException();
            // short gctId = compressType(gat.getGenericComponentType(), pWriteList);
            // ByteBuffer extraBuffer = ByteBuffer.allocate(2);
            // extraBuffer.putShort(gctId);
            // extraBuffer.rewind();
            // extra = extraBuffer.array();
            // size += extra.length;
          }
          else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] lowerBounds = wt.getLowerBounds();
            short lowerBoundsLen = (short) lowerBounds.length;
            Type[] upperBounds = wt.getUpperBounds();
            short upperBoundsLen = (short) upperBounds.length;
            ByteBuffer extraBuffer = ByteBuffer.allocate(4 + ((lowerBoundsLen + upperBoundsLen) * 2));
            extraBuffer.putShort(lowerBoundsLen);
            extraBuffer.putShort(upperBoundsLen);
            for (Type typeArg : lowerBounds) {
              short typeArgId = compressType(typeArg, pWriteList);
              extraBuffer.putShort(typeArgId);
            }
            for (Type typeArg : upperBounds) {
              short typeArgId = compressType(typeArg, pWriteList);
              extraBuffer.putShort(typeArgId);
            }
            extraBuffer.rewind();
            extra = extraBuffer.array();
            size += extra.length;
          }
          else
            throw new IllegalArgumentException("Unrecognized type (" + type.getClass().getName() + ")");
          ByteBuffer buffer = ByteBuffer.allocate(size);
          if (type instanceof Class) {
            buffer.put(TYPE_CLASS);
            buffer.put(extra);
          }
          else if (type instanceof ParameterizedType) {
            buffer.put(TYPE_PARAMETERIZED);
            buffer.put(extra);
          }
          else if (type instanceof GenericArrayType) {
            buffer.put(TYPE_GENERIC_ARRAY);
            buffer.put(extra);
          }
          else if (type instanceof TypeVariable) {
            buffer.put(TYPE_VARIABLE);
            buffer.put(extra);
          }
          else if (type instanceof WildcardType) {
            buffer.put(TYPE_WILDCARD);
            buffer.put(extra);
          }
          buffer.rewind();
          SER_VALUE idValue = serializeValue(buffer);
          pWriteList.add(
            new SerializedKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache, idKey, null, idValue, null));
        }
      }
    }
    return id;
  }

  protected @Nullable Type decompressType(short pTypeId) {
    Type type = mShortToType.get(pTypeId);
    if (type == null)
      throw new IllegalArgumentException("The type id (" + String.valueOf(pTypeId) + ") is not recognized");
    if (type == NULL_TYPE)
      return null;
    return type;
  }

  protected short compressKey(KeySPI<?> pKey, List<SerializedKeyValuePair<CACHE, SER_KEY, SER_VALUE>> pWriteList) {
    Short id = mKeyToShort.get(pKey);
    if (id == null) {
      synchronized (this) {
        id = mKeyToShort.get(pKey);
        if (id == null) {
          id = (short) mKeyCounter.incrementAndGet();
          mKeyToShort.put(pKey, id);
          mShortToKey.put(id, pKey);

          /* Verify that this key is just made up of static keys */

          KeySPI<Object>[] parts = pKey.getParts();
          int partsLen = parts.length;
          ByteBuffer buffer = ByteBuffer.allocate(4 * partsLen);
          for (KeySPI<?> p : parts) {
            if (p instanceof StaticKey == false)
              throw new IllegalArgumentException(
                "Only purely static keys can be used for defaults that are serialized");
            short typeId = compressType(p.getOutputType(), pWriteList);
            short valueId = compressString(p.getBaseKey(), pWriteList);
            buffer.putShort(typeId);
            buffer.putShort(valueId);
          }
          buffer.rewind();
          String keyKey = mKeyPrefix + String.valueOf(id);
          @SuppressWarnings("unchecked")
          SER_KEY idKey = (mKeySerializer != null ? mKeySerializer.apply(keyKey) : (SER_KEY) keyKey);
          SER_VALUE idValue = serializeValue(buffer);
          pWriteList.add(
            new SerializedKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache, idKey, null, idValue, null));
        }
      }
    }
    return id;
  }

  protected <V> Key<V> decompressKey(short pId) {
    @SuppressWarnings("unchecked")
    Key<V> key = (Key<V>) mShortToKey.get(pId);
    if (key == null)
      throw new IllegalArgumentException("The key id (" + String.valueOf(pId) + ") is not recognized");
    return key;
  }

  protected @NonNull SER_VALUE serializeValue(ByteBuffer pValue) {

    /* Shortcut if they are the same */

    if (ByteBuffer.class.equals(mSerValueClass)) {
      @SuppressWarnings("unchecked")
      SER_VALUE serValue = (SER_VALUE) pValue;
      return serValue;
    }
    if (byte[].class.equals(mSerValueClass)) {
      @SuppressWarnings("unchecked")
      SER_VALUE serValue = (SER_VALUE) pValue.array();
      return serValue;
    }
    return mConverterManager.convert(pValue, mSerValueClass);
  }

  protected ByteBuffer deserializeValue(SER_VALUE pValue) {

    /* Shortcut if they are the same */

    if (ByteBuffer.class.equals(mSerValueClass)) {
      ByteBuffer deserValue = (ByteBuffer) pValue;
      return deserValue;
    }
    if (byte[].class.equals(mSerValueClass)) {
      return ByteBuffer.wrap((byte[]) pValue);
    }
    return mConverterManager.convert(pValue, ByteBuffer.class);
  }

}
