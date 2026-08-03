package com.diamondq.cachly.base;

import com.diamondq.cachly.AccessContext;
import com.diamondq.cachly.Cache;
import com.diamondq.cachly.CacheInvalidator;
import com.diamondq.cachly.CacheKeyEvent;
import com.diamondq.cachly.CacheResult;
import com.diamondq.cachly.Key;
import com.diamondq.cachly.spi.CacheEngine;
import com.diamondq.cachly.spi.CacheStorage;
import com.diamondq.cachly.spi.CommonKeyValuePair;
import com.diamondq.cachly.spi.CompositeKey;
import com.diamondq.cachly.spi.KeySPI;
import com.diamondq.cachly.spi.ResolvedAccessContextPlaceholder;
import com.diamondq.cachly.spi.ResolvedKeyPlaceholder;
import com.diamondq.cachly.spi.StaticAccessContextPlaceholder;
import com.diamondq.cachly.spi.StaticCacheResult;
import com.diamondq.cachly.spi.StaticKey;
import com.diamondq.cachly.spi.StaticKeyPlaceholder;
import com.diamondq.cachly.spi.StaticKeyPlaceholderWithDefault;
import com.diamondq.common.converters.ConverterManager;
import com.diamondq.common.errors.DQRuntimeException;
import com.diamondq.common.lambda.interfaces.Consumer3;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This is the main abstract class for implementing Cache Storages. It provides a lot of the functionality that is
 * reused across all implementations.
 *
 * @param <CACHE> the underlying Cache type (i.e., EhCache)
 * @param <SER_KEY> the underlying type of the key
 */
public abstract class AbstractCacheStorage<CACHE, SER_KEY> implements CacheStorage {

  /**
   * Represents the serialization version number so that in the case of a change in the serialization format, older
   * persisted entries can be recognized and properly deserialized.
   */
  public static final byte SERIALIZATION_VERSION = 1;

  /**
   * The flag indicating that the value is actually null (as opposed to not found)
   */
  public static final int FLAG_ISNULL = 0x01;

  private static final byte TYPE_CLASS = 1;

  private static final byte TYPE_PARAMETERIZED = 2;

  private static final byte TYPE_GENERIC_ARRAY = 3;

  private static final byte TYPE_VARIABLE = 4;

  private static final byte TYPE_WILDCARD = 5;

  private static final byte PART_TYPE_ACCESS_CONTEXT = 1;

  private static final byte PART_TYPE_PLACEHOLDER = 2;

  private static final byte PART_TYPE_PLACEHOLDER_DEFAULTS = 3;
  private static final Type NULL_TYPE                      = NULL_TYPE_CLASS.class;

  /**
   * Holds information about the callback
   */
  protected static class CallbackInfo {
    /**
     * The actual callback function to call
     */
    public final Consumer3<Key<?>, CacheKeyEvent, Optional<?>> callback;

    /**
     * The access context used during the registration. It's used for any lookups during event firing.
     */
    public final AccessContext accessContext;

    /**
     * The last value.
     * <p>
     * This is used to determine if a change has actually occurred.
     * <p>
     * Sometimes events fire on a key, but the result is the same.
     */
    public volatile Optional<?> lastValue;

    /**
     * Primary Constructor
     *
     * @param pAccessContext the access context to store
     * @param pCallback the callback function
     */
    public CallbackInfo(AccessContext pAccessContext, Consumer3<Key<?>, CacheKeyEvent, Optional<?>> pCallback) {
      callback = pCallback;
      accessContext = pAccessContext;
      lastValue = Optional.empty();
    }
  }

  /**
   * The executor service
   */
  protected final           ExecutorService           mExecutorService;
  /**
   * The primary cache
   */
  protected final           CACHE                     mPrimaryCache;
  /**
   * The meta-cache or null if there isn't one.
   */
  protected final @Nullable CACHE                     mMetaCache;
  /**
   * The key serialization function
   */
  protected final @Nullable Function<String, SER_KEY> mKeySerializer;
  /**
   * The key deserialization function
   */
  protected final @Nullable Function<SER_KEY, String> mKeyDeserializer;
  /**
   * The Converter Manager instance
   */
  protected final           ConverterManager          mConverterManager;
  /**
   * The class of the serialized key
   */
  protected final           Class<SER_KEY>            mSerKeyClass;
  /**
   * The class of the serialized value
   */
  protected final           Class<?>                  mSerValueClass;

  /**
   * The map of strings to short ids
   */
  protected final              ConcurrentMap<String, Short>    mStringToShort;
  /**
   * The map of short ids to strings
   */
  protected final              ConcurrentMap<Short, String>    mShortToString;
  /**
   * The counter for tracking string ids
   */
  protected final              AtomicInteger                   mStringCounter;
  /**
   * The prefix to write before strings
   */
  protected final              String                          mStringPrefix;
  /**
   * The length of the string prefix (for performance)
   */
  protected final @NonNegative int                             mStringPrefixLen;
  /**
   * The map of types to short ids
   */
  protected final              ConcurrentMap<Type, Short>      mTypeToShort;
  /**
   * The map of short ids to types
   */
  protected final              ConcurrentMap<Short, Type>      mShortToType;
  /**
   * The counter for tracking type ids
   */
  protected final              AtomicInteger                   mTypeCounter;
  /**
   * The prefix to write before types
   */
  protected final              String                          mTypePrefix;
  /**
   * The length of the type prefix (for performance)
   */
  protected final @NonNegative int                             mTypePrefixLen;
  /**
   * The map of keys to short ids
   */
  protected final              ConcurrentMap<Key<?>, Short>    mKeyToShort;
  /**
   * The map of short ids to keys
   */
  protected final              ConcurrentMap<Short, Key<?>>    mShortToKey;
  /**
   * The counter for tracking key ids
   */
  protected final              AtomicInteger                   mKeyCounter;
  /**
   * The key prefix
   */
  protected final              String                          mKeyPrefix;
  /**
   * The length of the key prefix (for performance)
   */
  protected final @NonNegative int                             mKeyPrefixLen;
  /**
   * The prefix to put on all value keys
   */
  protected final @Nullable    String                          mValuePrefix;
  /**
   * The length of the value prefix
   */
  protected final @NonNegative int                             mValuePrefixLen;
  /**
   * Indicates whether the value should be serialized (because the underlying cache is going to write it, or whether it
   * can be kept as an object
   */
  protected final              boolean                         mSerializeValue;
  /**
   * Storage of the keys and the callbacks
   */
  protected final              Map<String, List<CallbackInfo>> mCallbacks;

  /**
   * The map of keys to callback semaphores
   */
  protected final             Map<String, Semaphore> mCallbackSemaphores;
  /**
   * The Cache engine
   */
  protected @MonotonicNonNull CacheEngine            mCacheEngine;

  /**
   * Primary constructor
   *
   * @param pConverterManager the Converter Manager used to perform data conversions
   * @param pExecutorService the Executor Service
   * @param pPrimaryCache the underlying cache to store data
   * @param pMetaCache the underlying cache to store metadata
   * @param pSerKeyClass the class used for serialized keys
   * @param pSerValueClass the class used for serialized values
   * @param pSerializeValue true if values should be serialized or false if just stored directly. Usually false for
   *   memory-based caches, but true is usually needed for persistent caches.
   * @param pStringPrefix a prefix to add to all keys
   * @param pTypePrefix a prefix to add to all types
   * @param pKeyPrefix the prefix for keys
   * @param pValuePrefix the prefix for values
   * @param pKeySerializer the function that will serialize keys from a string to the serialized format
   * @param pKeyDeserializer the function that will deserialize keys from the serialized format to a string
   */
  protected AbstractCacheStorage(ConverterManager pConverterManager, ExecutorService pExecutorService,
    CACHE pPrimaryCache, @Nullable CACHE pMetaCache, Class<SER_KEY> pSerKeyClass, Class<?> pSerValueClass,
    boolean pSerializeValue, @Nullable String pStringPrefix, @Nullable String pTypePrefix, @Nullable String pKeyPrefix,
    @Nullable String pValuePrefix, @Nullable Function<String, SER_KEY> pKeySerializer,
    @Nullable Function<SER_KEY, String> pKeyDeserializer) {
    mConverterManager = pConverterManager;
    mExecutorService = pExecutorService;
    mPrimaryCache = pPrimaryCache;
    mMetaCache = pMetaCache;
    mKeySerializer = pKeySerializer;
    mKeyDeserializer = pKeyDeserializer;
    mSerKeyClass = pSerKeyClass;
    mSerValueClass = pSerValueClass;
    mStringToShort = new ConcurrentHashMap<>();
    mShortToString = new ConcurrentHashMap<>();
    mStringCounter = new AtomicInteger();
    //noinspection HardcodedFileSeparator
    mStringPrefix = pStringPrefix != null ? pStringPrefix : "s/";
    mStringPrefixLen = mStringPrefix.length();
    mTypeToShort = new ConcurrentHashMap<>();
    mShortToType = new ConcurrentHashMap<>();
    mTypeCounter = new AtomicInteger();
    //noinspection HardcodedFileSeparator
    mTypePrefix = pTypePrefix != null ? pTypePrefix : "t/";
    mTypePrefixLen = mTypePrefix.length();
    mKeyToShort = new ConcurrentHashMap<>();
    mShortToKey = new ConcurrentHashMap<>();
    mKeyCounter = new AtomicInteger();
    //noinspection HardcodedFileSeparator
    mKeyPrefix = pKeyPrefix != null ? pKeyPrefix : "k/";
    mKeyPrefixLen = mTypePrefix.length();
    //noinspection HardcodedFileSeparator
    mValuePrefix = pValuePrefix != null ? pValuePrefix : "p/";
    mValuePrefixLen = mValuePrefix.length();
    mSerializeValue = pSerializeValue;
    mCallbacks = new ConcurrentHashMap<>();
    mCallbackSemaphores = new ConcurrentHashMap<>();

    if (!mSerializeValue) {
      if (mSerValueClass != MemoryStorageData.class) {
        throw new IllegalArgumentException(
          "Only a SER_VALUE of MemoryStorageData.class is supported for non-serializing caches");
      }
    }
  }

  /**
   * Query the underlying cache to retrieve the metadata information
   */
  protected void init() {
    if (mSerializeValue) {
      Map<Short, ByteBuffer> temporaryKeys = new HashMap<>();
      Map<Short, ByteBuffer> temporaryTypes = new HashMap<>();
      streamMetaEntries().forEach((entry) -> {
        SER_KEY key = entry.getKey();
        String keyStr = (mKeyDeserializer != null ? mKeyDeserializer.apply(key) : (String) key);
        if (keyStr.startsWith(mStringPrefix)) {
          var keyStrLen = keyStr.length();
          if (keyStrLen <= mStringPrefixLen) throw new IllegalStateException("Key string is too short");
          short id = Short.parseShort(keyStr.substring(mStringPrefixLen));
          Object value = entry.getValue();
          ByteBuffer valueBuffer = convertSERVALUEtoByteBuffer(value);

          /* The contents of a String is just the UTF-8 bytes */

          String valueStr = new String(valueBuffer.array(), StandardCharsets.UTF_8);
          mStringToShort.put(valueStr, id);
          mShortToString.put(id, valueStr);
          if (id > mStringCounter.get()) mStringCounter.set(id);
        } else if (keyStr.startsWith(mTypePrefix)) {
          var keyStrLen = keyStr.length();
          if (keyStrLen <= mTypePrefixLen) throw new IllegalStateException("Key string is too short");
          short id = Short.parseShort(keyStr.substring(mTypePrefixLen));
          Object value = entry.getValue();
          ByteBuffer valueBuffer = convertSERVALUEtoByteBuffer(value);

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
          } else {
            temporaryTypes.put(id, valueBuffer);
            return;
          }
          mShortToType.put(id, type);
          if (id > mTypeCounter.get()) mTypeCounter.set(id);
        } else if (keyStr.startsWith(mKeyPrefix)) {
          var keyStrLen = keyStr.length();
          if (keyStrLen <= mKeyPrefixLen) throw new IllegalStateException("Key string is too short");
          short id = Short.parseShort(keyStr.substring(mKeyPrefixLen));
          Object value = entry.getValue();
          ByteBuffer valueBuffer = convertSERVALUEtoByteBuffer(value);

          /*
           * Because all keys are being read in sequentially, the type entries needed to resolve the key may not exist
           * yet.
           *
           * Therefore, remember it and do it later
           */

          temporaryKeys.put(id, valueBuffer);
        }
      });

      /* Handle all the saved types. NOTE: Again, due to ordering, this list may need to be processed multiple times */

      Map<Short, ByteBuffer> currentTypes = temporaryTypes;
      while (!currentTypes.isEmpty()) {
        Map<Short, ByteBuffer> delayed = new HashMap<>();
        TemporaryTypeLoop:
        for (Map.Entry<Short, ByteBuffer> entry : currentTypes.entrySet()) {
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
            if (!mShortToType.containsKey(ownerTypeId)) {
              delayed.put(id, valueBuffer);
              continue;
            }
            if (!mShortToType.containsKey(rawTypeId)) {
              delayed.put(id, valueBuffer);
              continue;
            }

            Type ownerType = decompressType(ownerTypeId);
            var raw = decompressType(rawTypeId);
            if (raw == null) throw new IllegalArgumentException("null not supported");
            Class<?> rawType = (Class<?>) raw;
            short actualTypeArgumentsLen = valueBuffer.getShort();
            if (actualTypeArgumentsLen < 0) throw new IllegalStateException("actualTypeArgumentsLen is negative");
            Type[] actualTypeArguments = new Type[actualTypeArgumentsLen];
            for (short shortI = 0; shortI < actualTypeArgumentsLen; shortI++) {
              short actualTypeId = valueBuffer.getShort();
              if (!mShortToType.containsKey(actualTypeId)) {
                delayed.put(id, valueBuffer);
                continue TemporaryTypeLoop;
              }
              var actualType = decompressType(actualTypeId);
              if (actualType == null) throw new IllegalArgumentException("null not supported");
              actualTypeArguments[shortI] = actualType;
            }
            if (ownerType != null) {
              type = TypeUtils.parameterizeWithOwner(ownerType, rawType, actualTypeArguments);
            } else {
              type = TypeUtils.parameterize(rawType, actualTypeArguments);
            }
          } else if (typeType == TYPE_GENERIC_ARRAY) {
            short gaTypeId = valueBuffer.getShort();
            if (!mShortToType.containsKey(gaTypeId)) {
              delayed.put(id, valueBuffer);
              continue;
            }
            var gaType = decompressType(gaTypeId);
            if (gaType == null) throw new IllegalArgumentException("null not supported");
            type = TypeUtils.genericArrayType(gaType);
          } else if (typeType == TYPE_VARIABLE) {
            throw new UnsupportedOperationException("Variable Types are not supported");
          } else if (typeType == TYPE_WILDCARD) {
            short lowerBoundsLen = valueBuffer.getShort();
            if (lowerBoundsLen < 0) throw new IllegalStateException("lowerBoundsLen is negative");
            Type[] lowerBounds = lowerBoundsLen == 0 ? null : new Type[lowerBoundsLen];
            short upperBoundsLen = valueBuffer.getShort();
            if (upperBoundsLen < 0) throw new IllegalStateException("upperBoundsLen is negative");
            Type[] upperBounds = upperBoundsLen == 0 ? null : new Type[upperBoundsLen];
            if (lowerBounds != null) {
              for (short shortI = 0; shortI < lowerBounds.length; shortI++) {
                short lowerBoundsTypeId = valueBuffer.getShort();
                if (!mShortToType.containsKey(lowerBoundsTypeId)) {
                  delayed.put(id, valueBuffer);
                  continue TemporaryTypeLoop;
                }
                var lowerBoundsType = decompressType(lowerBoundsTypeId);
                if (lowerBoundsType == null) throw new IllegalArgumentException("null not supported");
                lowerBounds[shortI] = lowerBoundsType;
              }
            }
            if ((upperBoundsLen > 0) && (upperBounds != null)) {
              for (short shortI = 0; shortI < upperBounds.length; shortI++) {
                short upperBoundsTypeId = valueBuffer.getShort();
                if (!mShortToType.containsKey(upperBoundsTypeId)) {
                  delayed.put(id, valueBuffer);
                  continue TemporaryTypeLoop;
                }
                var upperBoundsType = decompressType(upperBoundsTypeId);
                if (upperBoundsType == null) throw new IllegalArgumentException("null not supported");
                upperBounds[shortI] = upperBoundsType;
              }
            }
            var typeBuilder = TypeUtils.wildcardType();
            if (lowerBounds != null) typeBuilder.withLowerBounds(lowerBounds);
            if (upperBounds != null) typeBuilder.withUpperBounds(upperBounds);
            type = typeBuilder.build();
          } else {
            throw new IllegalArgumentException("Unrecognized type (" + typeType + ")");
          }
          mTypeToShort.put(type, id);
          mShortToType.put(id, type);
          if (id > mTypeCounter.get()) mTypeCounter.set(id);
        }
        if (currentTypes.size() == delayed.size()) throw new IllegalStateException("Unable to proceed decoding types");
        currentTypes = delayed;
      }

      /* Handle all the saved keys */

      for (Map.Entry<Short, ByteBuffer> entry : temporaryKeys.entrySet()) {
        short id = entry.getKey();
        ByteBuffer valueBuffer = entry.getValue();
        valueBuffer.rewind();
        int partLen = valueBuffer.limit() / 4;
        @SuppressWarnings("unchecked") KeySPI<@Nullable Object>[] parts = new KeySPI[partLen];
        for (int i = 0; i < partLen; i++) {
          var keyType = decompressType(valueBuffer.getShort());
          if (keyType == null) throw new IllegalArgumentException("null not supported");
          String keyBase = decompressString(valueBuffer.getShort());
          if (keyBase == null) throw new IllegalArgumentException("null not supported");
          parts[i] = new StaticKey<>(keyBase, keyType);
        }
        Key<?> shortKey = new CompositeKey<>(parts);
        mKeyToShort.put(shortKey, id);
        mShortToKey.put(id, shortKey);
        if (id > mKeyCounter.get()) mKeyCounter.set(id);
      }
    }
  }

  /**
   * Writes to the cache as defined in the entry
   *
   * @param pEntry the entry of data to write
   */
  protected abstract void writeToCache(CommonKeyValuePair<CACHE, SER_KEY> pEntry);

  /**
   * Reads from the primary cache
   *
   * @param pKey the key
   * @return the optional value
   */
  protected abstract Optional<?> readFromPrimaryCache(SER_KEY pKey);

  /**
   * Invalidate entries
   *
   * @param pCache the cache
   * @param pKey if provided, invalidate just this key, if null, then invalidate all keys
   */
  protected abstract void invalidate(CACHE pCache, @Nullable SER_KEY pKey);

  /**
   * Convert the given key/value into a list of CommonKeyValuePair's
   *
   * @param <V> the value type
   * @param pKey the key
   * @param pResult the cached value
   * @return the stream
   */
  protected <V extends @Nullable Object> List<CommonKeyValuePair<CACHE, SER_KEY>> serializeEntry(KeySPI<V> pKey,
    CacheResult<V> pResult) {

    String fullKey = pKey.toString();

    /* Calculate the final primary key */

    String primaryKeyStr = mValuePrefix + fullKey;
    @SuppressWarnings("unchecked") SER_KEY primaryKey = (
      mKeySerializer != null ? mKeySerializer.apply(primaryKeyStr) : (SER_KEY) primaryKeyStr);

    if (mSerializeValue) {
      /* Get the pieces that need to be serialized */

      String baseKey = pKey.getFullBaseKey();
      String serializerName = pKey.getLastSerializerName();
      String serializer = Cache.DEFAULT_SERIALIZER.equals(serializerName) ? null : serializerName;
      Type outputType = pKey.getOutputType();
      Duration overrideExpiry = pResult.getOverrideExpiry();
      boolean isNull = pResult.isNull();
      V value = isNull ? null : pResult.getValue();
      @SuppressWarnings({ "null", "unchecked" }) Class<V> valueClass =
        value != null ? (Class<V>) value.getClass() : (Class<V>) outputType;

      List<CommonKeyValuePair<CACHE, SER_KEY>> listOfEntries = new ArrayList<>();

      /* Now, compress the metadata into smaller pieces */

      short baseKeyId = compressString(baseKey, listOfEntries);
      short serializerId = compressString(serializer, listOfEntries);
      short outputTypeId = compressType(outputType, listOfEntries);
      short valueClassId = compressType(valueClass, listOfEntries);

      /* Now build the block */

      ByteBuffer valueBuffer;
      int valueBufferSize;

      if (value != null) {
        valueBuffer = mConverterManager.convert(value, ByteBuffer.class, serializer);
        valueBuffer.rewind();
        valueBufferSize = valueBuffer.limit();
      } else {
        valueBuffer = null;
        valueBufferSize = 0;
      }

      //noinspection ExtractMethodRecommender
      int size = 0;
      for (KeySPI<?> part : pKey.getParts()) {
        if (part instanceof ResolvedKeyPlaceholder<?> rkp) {
          KeySPI<?> placeholder = rkp.getPlaceholder();
          if (placeholder instanceof StaticKeyPlaceholder) {
            size += 1;
          } else if (placeholder instanceof StaticKeyPlaceholderWithDefault) {
            size += 3;
          } else {
            throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
          }
        } else if (part instanceof ResolvedAccessContextPlaceholder<?> racp) {
          KeySPI<?> placeholder = racp.getPlaceholder();
          if (placeholder instanceof StaticAccessContextPlaceholder) {
            size += 3;
          } else {
            throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
          }
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
        if (part instanceof ResolvedKeyPlaceholder<?> rkp) {
          KeySPI<?> placeholder = rkp.getPlaceholder();
          if (placeholder instanceof StaticKeyPlaceholder) {
            result.put(PART_TYPE_PLACEHOLDER);
          } else if (placeholder instanceof StaticKeyPlaceholderWithDefault skpwd) {
            result.put(PART_TYPE_PLACEHOLDER_DEFAULTS);
            result.putShort(compressKey(skpwd.getDefaultKey(), listOfEntries));
          } else {
            throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
          }
        } else if (part instanceof ResolvedAccessContextPlaceholder<?> racp) {
          KeySPI<?> placeholder = racp.getPlaceholder();
          if (placeholder instanceof StaticAccessContextPlaceholder) {
            result.put(PART_TYPE_ACCESS_CONTEXT);
          } else {
            throw new IllegalStateException("Unrecognized placeholder (" + placeholder.getClass().getName() + ")");
          }
        }
      }

      /* Write the data */

      if (valueBuffer != null && valueBufferSize > 0) result.put(valueBuffer);
      result.rewind();

      /* Calculate the final value */

      Object finalValue = convertByteBufferToSERVALUE(result);

      listOfEntries.add(new CommonKeyValuePair<>(mPrimaryCache, primaryKey, pKey, finalValue, overrideExpiry));
      return listOfEntries;
    }

    /* Since the code is not serializing, then just need to wrap the key and value into an object that can be stored */

    Object finalValue = new MemoryStorageData(pKey, pResult.isNull() ? null : pResult.getValue());

    Duration overrideExpiry = pResult.getOverrideExpiry();

    return Collections.singletonList(new CommonKeyValuePair<>(mPrimaryCache,
      primaryKey,
      pKey,
      finalValue,
      overrideExpiry
    ));
  }

  /**
   * Provides a stream of entries from the primary. NOTE: If meta is not a separate cache, then it is expected that
   * metadata may be present, so it's OK to return all keys
   *
   * @return the entries
   */
  protected abstract Stream<Map.Entry<SER_KEY, ? extends Object>> streamPrimary();

  /**
   * Provides a stream of entries from the meta. NOTE: If meta is not a separate cache, then it is expected that regular
   * data will be present, so it's OK to return all keys.
   *
   * @return the entries
   */
  protected abstract Stream<Map.Entry<SER_KEY, ? extends Object>> streamMetaEntries();

  /**
   * Deserializes a SER_KEY and SER_VALUE into a Key<?> and CacheResult<?>
   *
   * @param pKey the key
   * @param pValue the value
   * @return the entry
   */
  protected Map.Entry<Key<?>, CacheResult<?>> deserializeEntry(SER_KEY pKey, @Nullable Object pValue) {

    String fullKey = (mKeyDeserializer != null ? mKeyDeserializer.apply(pKey) : (String) pKey);

    //noinspection VariableNotUsedInsideIf
    if (mValuePrefix != null) {
      var fullKeyLen = fullKey.length();
      if (fullKeyLen <= mValuePrefixLen) throw new IllegalStateException("Key string is too short");
      fullKey = fullKey.substring(mValuePrefixLen);
    }

    if (pValue == null) return new SimpleEntry<>(new CompositeKey<>(fullKey, Object.class), CacheResult.notFound());

    if (mSerializeValue) {

      ByteBuffer buffer = convertSERVALUEtoByteBuffer(pValue);
      buffer.rewind();

      /* Get and validate the version */

      byte versionFlags = buffer.get();
      @SuppressWarnings("MagicNumber") byte version = (byte) (versionFlags & 0x0F);
      if (version != SERIALIZATION_VERSION) {
        throw new IllegalStateException(
          "The entry " + fullKey + " has an unrecognized serialization version (" + String.valueOf(version) + ")");
      }

      @SuppressWarnings("MagicNumber") int flags = (byte) ((versionFlags & 0xF0) >> 4);

      /* Is null? */

      boolean isNull = (flags & FLAG_ISNULL) == FLAG_ISNULL;

      /* Get the ids */

      short baseKeyId = buffer.getShort();
      short serializerId = buffer.getShort();
      short outputTypeId = buffer.getShort();
      short valueClassId = buffer.getShort();

      /* Decompress ids */

      String baseKey = decompressString(baseKeyId);
      if (baseKey == null) throw new IllegalArgumentException("null not supported");
      String serializer = decompressString(serializerId);
      var outputType = decompressType(outputTypeId);
      if (outputType == null) throw new IllegalArgumentException("null not supported");
      var valueClass = decompressType(valueClassId);
      if (valueClass == null) throw new IllegalArgumentException("null not supported");

      /* Now generate the Key */

      String[] baseSplit = baseKey.split("/");
      int keyLen = baseSplit.length;
      String[] fullSplit = fullKey.split("/");
      if (keyLen != fullSplit.length) {
        throw new IllegalStateException(
          "The base key (" + baseKey + ") doesn't have the same number of parts as the full key (" + fullKey + ")");
      }

      @SuppressWarnings("unchecked") KeySPI<? extends @Nullable Object>[] parts = new KeySPI[keyLen];

      for (int i = 0; i < keyLen; i++) {
        String partBaseKey = baseSplit[i];
        if (partBaseKey.startsWith("{")) {
          String strippedPartBaseKey = partBaseKey.substring(1, partBaseKey.length() - 1);
          byte placeholderType = buffer.get();
          if (placeholderType == PART_TYPE_ACCESS_CONTEXT) {
            parts[i] = new ResolvedAccessContextPlaceholder<>(new StaticAccessContextPlaceholder<>(strippedPartBaseKey,
              outputType
            ), fullSplit[i]
            );
          } else if (placeholderType == PART_TYPE_PLACEHOLDER) {
            parts[i] = new ResolvedKeyPlaceholder<>(new StaticKeyPlaceholder<>(strippedPartBaseKey, outputType),
              fullSplit[i]
            );
          } else if (placeholderType == PART_TYPE_PLACEHOLDER_DEFAULTS) {
            short defaultKeyId = buffer.getShort();
            Key<String> defaultKey = decompressKey(defaultKeyId);
            @SuppressWarnings(
              { "unchecked", "rawtypes" }) KeySPI<Object> r = (KeySPI<Object>) (KeySPI) new ResolvedKeyPlaceholder<>(new StaticKeyPlaceholderWithDefault(
              strippedPartBaseKey,
              outputType,
              defaultKey
            ), fullSplit[i]
            );
            parts[i] = r;
          } else {
            throw new IllegalStateException(
              "The placeholder part type(" + String.valueOf(placeholderType) + ") is not recognized");
          }
        } else {
          parts[i] = new StaticKey<>(fullSplit[i], outputType);
        }
      }

      Key<?> finalKey = new CompositeKey<>(parts);

      /* Now generate the value */

      Object value;
      if (isNull) {
        value = null;
      } else {
        ByteBuffer dataBuffer = buffer.slice();
        value = mConverterManager.convert(dataBuffer, valueClass, serializer);
      }

      var finalValue = new StaticCacheResult<@Nullable Object>(value, true);

      return new SimpleEntry<>(finalKey, finalValue);
    }

    /* Since this code is not deserializing from bytes, it's just a simple return */

    MemoryStorageData msd = (MemoryStorageData) pValue;

    return new SimpleEntry<>(msd.key, new StaticCacheResult<@Nullable Object>(msd.value, true));
  }

  /**
   * Compress a string
   *
   * @param pValue the string to compress
   * @param pWriteList the list to add additional key/values if necessary
   * @return the short id assigned to the string
   */
  protected short compressString(@Nullable String pValue, List<CommonKeyValuePair<CACHE, SER_KEY>> pWriteList) {
    String value = pValue == null ? "__NULL__" : pValue;
    Short id = mStringToShort.get(value);
    if (id == null) {
      synchronized (this) {
        id = mStringToShort.get(value);
        if (id == null) {
          //noinspection NumericCastThatLosesPrecision
          id = (short) mStringCounter.incrementAndGet();
          mStringToShort.put(value, id);
          mShortToString.put(id, value);
          String strKey = mStringPrefix + id;
          @SuppressWarnings("unchecked") SER_KEY idKey = (
            mKeySerializer != null ? mKeySerializer.apply(strKey) : (SER_KEY) strKey);
          Object idValue = convertByteBufferToSERVALUE(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
          pWriteList.add(new CommonKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache,
            idKey,
            null,
            idValue,
            null
          ));
        }
      }
    }
    return id;
  }

  /**
   * Decompress a string
   *
   * @param pId the short id
   * @return the string
   */
  protected @Nullable String decompressString(short pId) {
    String str = mShortToString.get(pId);
    if (str == null)
      throw new IllegalArgumentException("The string id (" + String.valueOf(pId) + ") is not recognized");
    if (str.equals("__NULL__")) return null;
    return str;
  }

  /**
   * Compress a type
   *
   * @param pType the type
   * @param pWriteList the list to write additional key/value if necessary
   * @return the short id
   */
  protected short compressType(@Nullable Type pType, List<CommonKeyValuePair<CACHE, SER_KEY>> pWriteList) {
    Type type = pType == null ? NULL_TYPE : pType;
    Short id = mTypeToShort.get(type);
    if (id == null) {
      synchronized (this) {
        id = mTypeToShort.get(type);
        if (id == null) {
          //noinspection NumericCastThatLosesPrecision
          id = (short) mTypeCounter.incrementAndGet();
          mTypeToShort.put(type, id);
          mShortToType.put(id, type);
          String typeKey = mTypePrefix + id;
          @SuppressWarnings("unchecked") SER_KEY idKey = (
            mKeySerializer != null ? mKeySerializer.apply(typeKey) : (SER_KEY) typeKey);
          int size = 1;
          byte[] extra;
          switch (type) {
            case Class<?> aClass -> {
              String className = ClassUtils.getCanonicalName(aClass);
              extra = className.getBytes(StandardCharsets.UTF_8);
              size += extra.length;
            }
            case ParameterizedType pt -> {
              short ownerTypeId = compressType(pt.getOwnerType(), pWriteList);
              short rawTypeId = compressType(pt.getRawType(), pWriteList);
              Type[] actualTypeArguments = pt.getActualTypeArguments();
              //noinspection NumericCastThatLosesPrecision
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
            case GenericArrayType gat -> {
              short gctId = compressType(gat.getGenericComponentType(), pWriteList);
              ByteBuffer extraBuffer = ByteBuffer.allocate(2);
              extraBuffer.putShort(gctId);
              extraBuffer.rewind();
              extra = extraBuffer.array();
              size += extra.length;
            }
            case WildcardType wt -> {
              Type[] lowerBounds = wt.getLowerBounds();
              //noinspection NumericCastThatLosesPrecision
              short lowerBoundsLen = (short) lowerBounds.length;
              Type[] upperBounds = wt.getUpperBounds();
              //noinspection NumericCastThatLosesPrecision
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
            default -> throw new IllegalArgumentException("Unrecognized type (" + type.getClass().getName() + ")");
          }
          ByteBuffer buffer = ByteBuffer.allocate(size);
          switch (type) {
            case Class<?> _ -> {
              buffer.put(TYPE_CLASS);
              buffer.put(extra);
            }
            case ParameterizedType _ -> {
              buffer.put(TYPE_PARAMETERIZED);
              buffer.put(extra);
            }
            case GenericArrayType _ -> {
              buffer.put(TYPE_GENERIC_ARRAY);
              buffer.put(extra);
            }
            case TypeVariable<?> _ -> {
              buffer.put(TYPE_VARIABLE);
              buffer.put(extra);
            }
            case WildcardType _ -> {
              buffer.put(TYPE_WILDCARD);
              buffer.put(extra);
            }
            default -> {
            }
          }
          buffer.rewind();
          Object idValue = convertByteBufferToSERVALUE(buffer);
          pWriteList.add(new CommonKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache,
            idKey,
            null,
            idValue,
            null
          ));
        }
      }
    }
    return id;
  }

  /**
   * Decompress a type
   *
   * @param pTypeId the type short id
   * @return the type
   */
  protected @Nullable Type decompressType(short pTypeId) {
    Type type = mShortToType.get(pTypeId);
    if (type == null)
      throw new IllegalArgumentException("The type id (" + String.valueOf(pTypeId) + ") is not recognized");
    if (type.equals(NULL_TYPE)) return null;
    return type;
  }

  /**
   * Compress a key
   *
   * @param pKey the key
   * @param pWriteList the list of key/values to write if necessary
   * @return the key short id
   */
  protected short compressKey(KeySPI<?> pKey, List<CommonKeyValuePair<CACHE, SER_KEY>> pWriteList) {
    Short id = mKeyToShort.get(pKey);
    if (id == null) {
      synchronized (this) {
        id = mKeyToShort.get(pKey);
        if (id == null) {
          //noinspection NumericCastThatLosesPrecision
          id = (short) mKeyCounter.incrementAndGet();
          mKeyToShort.put(pKey, id);
          mShortToKey.put(id, pKey);

          /* Verify that this key is just made up of static keys */

          KeySPI<? extends @Nullable Object>[] parts = pKey.getParts();
          int partsLen = parts.length;
          ByteBuffer buffer = ByteBuffer.allocate(4 * partsLen);
          for (KeySPI<?> p : parts) {
            if (!(p instanceof StaticKey)) {
              throw new IllegalArgumentException("Only purely static keys can be used for defaults that are serialized");
            }
            short typeId = compressType(p.getOutputType(), pWriteList);
            short valueId = compressString(p.getBaseKey(), pWriteList);
            buffer.putShort(typeId);
            buffer.putShort(valueId);
          }
          buffer.rewind();
          String keyKey = mKeyPrefix + id;
          @SuppressWarnings("unchecked") SER_KEY idKey = (
            mKeySerializer != null ? mKeySerializer.apply(keyKey) : (SER_KEY) keyKey);
          Object idValue = convertByteBufferToSERVALUE(buffer);
          pWriteList.add(new CommonKeyValuePair<>(mMetaCache != null ? mMetaCache : mPrimaryCache,
            idKey,
            null,
            idValue,
            null
          ));
        }
      }
    }
    return id;
  }

  /**
   * Decompress a key
   *
   * @param pId the key id
   * @param <V> the key type
   * @return the key
   */
  protected <V> Key<V> decompressKey(short pId) {
    @SuppressWarnings("unchecked") Key<V> key = (Key<V>) mShortToKey.get(pId);
    if (key == null) throw new IllegalArgumentException("The key id (" + String.valueOf(pId) + ") is not recognized");
    return key;
  }

  /**
   * Converts a byte buffer into the SER_VALUE type
   *
   * @param pValue the byte buffer
   * @return the value
   */
  protected Object convertByteBufferToSERVALUE(ByteBuffer pValue) {

    /* Shortcut if they are the same */

    if (mSerValueClass == ByteBuffer.class) return pValue;
    if (mSerValueClass == byte[].class) return pValue.array();
    return mConverterManager.<ByteBuffer, Object>convert(pValue, mSerValueClass);
  }

  /**
   * Converts a SER_VALUE into a Byte Buffer
   *
   * @param pValue the SER_VALUE
   * @return the new byte buffer
   */
  protected ByteBuffer convertSERVALUEtoByteBuffer(Object pValue) {

    if (mSerializeValue) {
      /* Shortcut if they are the same */

      if (mSerValueClass == ByteBuffer.class) {
        return (ByteBuffer) pValue;
      }
      if (mSerValueClass == byte[].class) {
        return ByteBuffer.wrap((byte[]) pValue);
      }
      return mConverterManager.convert(pValue, ByteBuffer.class);
    }

    throw new UnsupportedOperationException("Unsupported storage if serialize value is disabled");
  }

  @Override
  public <V extends @Nullable Object> void store(AccessContext pAccessContext, KeySPI<V> pKey,
    CacheResult<V> pLoadedResult) {

    /* Convert the data into the things to actually write */

    var kvPairs = serializeEntry(pKey, pLoadedResult);

    /* Mark that the code is waiting for a callback from the cache */

    var semaphore = prepareSemphore(pKey);

    /* Write them to the cache */

    for (CommonKeyValuePair<CACHE, SER_KEY> kvpair : kvPairs) {
      writeToCache(kvpair);
    }

    /* Wait for the callback to occur */

    waitSemaphore(semaphore);
  }

  /**
   * Returns whether a callback is assigned to the given key
   *
   * @param pKey the key
   * @param <O> the key type
   * @return true if there is a callback or false otherwise
   */
  protected <O extends @Nullable Object> boolean hasCallback(KeySPI<O> pKey) {
    var callbackInfoList = mCallbacks.get(pKey.toString());
    return callbackInfoList != null && !callbackInfoList.isEmpty();
  }

  /**
   * Helper method that calls any necessary callbacks
   *
   * @param pKey the key
   * @param pEvent the event
   * @param <O> the key type
   */
  @RequiresNonNull("mCacheEngine")
  private <O extends @Nullable Object> void callCallbacks(KeySPI<O> pKey, CacheKeyEvent pEvent) {
    var callbackInfoList = mCallbacks.get(pKey.toString());
    if (callbackInfoList != null) {
      for (var callbackInfo : callbackInfoList) {
        @SuppressWarnings(
          { "unchecked", "rawtypes" }) Consumer3<Key<O>, CacheKeyEvent, Optional<@NonNull O>> castedCallback = (Consumer3<Key<O>, CacheKeyEvent, Optional<@NonNull O>>) (Consumer3) callbackInfo.callback;

        var cacheEngine = mCacheEngine;
        var value = cacheEngine.getIfPresent(callbackInfo.accessContext, pKey);

        /* Check if the value is different */

        if (!value.equals(callbackInfo.lastValue)) {
          callbackInfo.lastValue = value;
          castedCallback.accept(pKey, pEvent, value);
        }
      }
    }

    if (pEvent == CacheKeyEvent.REMOVED) {
      if (!pKey.hasKeyDetails()) mCacheEngine.setupKey(pKey);
      var loader = pKey.getLoader();
      if (loader instanceof CacheInvalidator) {
        @SuppressWarnings("unchecked") CacheInvalidator<O> ci = (CacheInvalidator<O>) loader;
        ci.invalidate(mCacheEngine, pKey);
      }
    }

    var semaphore = mCallbackSemaphores.get(pKey.toString());
    if (semaphore != null) semaphore.release();
  }

  /**
   * Prepares semaphore so that the code can wait on this thread until the callback occurs
   *
   * @param pKey the key
   * @param <V> the key type
   * @return the optional semaphore
   */
  protected <V extends @Nullable Object> Optional<Semaphore> prepareSemphore(KeySPI<V> pKey) {
    String keyStr = pKey.toString();

    /* Check to see if there is a callback for this key */

    var callbackList = mCallbacks.get(keyStr);
    if (callbackList == null) return Optional.empty();
    if (callbackList.isEmpty()) return Optional.empty();

    return Optional.of(mCallbackSemaphores.computeIfAbsent(keyStr, (_) -> new Semaphore(0)));
  }

  /**
   * Waits for the semaphore to complete
   *
   * @param pSemaphoreOpt the optional semaphore
   */
  protected static void waitSemaphore(Optional<Semaphore> pSemaphoreOpt) {
    if (pSemaphoreOpt.isEmpty()) return;
    var semaphore = pSemaphoreOpt.get();
    try {
      semaphore.acquire();
    }
    catch (InterruptedException ex) {
      throw new DQRuntimeException(ex);
    }
  }

  @Override
  public <V extends @Nullable Object> void invalidate(AccessContext pAccessContext, KeySPI<V> pKey) {

    /* Get the key string */

    String keyStr = (mValuePrefix != null ? mValuePrefix + pKey : pKey.toString());

    /* Get the serialized version of the key */

    @SuppressWarnings("unchecked") SER_KEY serKey = (
      mKeySerializer != null ? mKeySerializer.apply(keyStr) : (SER_KEY) keyStr);

    /* Mark that the code is waiting for a callback from the cache */

    var semaphore = prepareSemphore(pKey);

    invalidate(mPrimaryCache, serKey);

    /* Wait for the callback to occur */

    waitSemaphore(semaphore);

  }

  @Override
  public void invalidateAll(AccessContext pAccessContext) {
    invalidate(mPrimaryCache, null);

    /* Since the code has removed everything, all the metadata can also be removed */

    CACHE metaCache = mMetaCache;
    if (metaCache != null) invalidate(metaCache, null);
  }

  @Override
  public Stream<Map.Entry<Key<?>, CacheResult<?>>> streamEntries(AccessContext pAccessContext) {

    /* Get the set of data */

    Stream<Map.Entry<SER_KEY, ? extends Object>> rawStream = streamPrimary();

    /* If there is no separate meta-cache, then the metadata may be present */

    var valuePrefix = mValuePrefix;
    if ((mMetaCache == null) && (valuePrefix != null)) {
      var keyDeserializer = mKeyDeserializer;
      if (keyDeserializer != null) {
        rawStream = rawStream.filter((entry) -> keyDeserializer.apply(entry.getKey()).startsWith(valuePrefix));
      } else {
        rawStream = rawStream.filter((entry) -> ((String) entry.getKey()).startsWith(valuePrefix));
      }
    }

    /* Convert the result back */

    return rawStream.map((entry) -> deserializeEntry(entry.getKey(), entry.getValue()));
  }

  @Override
  public <V extends @Nullable Object> CacheResult<V> queryForKey(AccessContext pAccessContext, KeySPI<V> pKey) {

    /* Get the key string */

    String keyStr = (mValuePrefix != null ? mValuePrefix + pKey : pKey.toString());

    /* Get the serialized version of the key */

    @SuppressWarnings("unchecked") SER_KEY serKey = (
      mKeySerializer != null ? mKeySerializer.apply(keyStr) : (SER_KEY) keyStr);

    /* Query the underlying primary cache */

    Optional<?> valueOpt = readFromPrimaryCache(serKey);

    /* If it's not found, then done */

    if (valueOpt.isEmpty()) return CacheResult.notFound();

    /* Deserialize the entry */

    Map.Entry<Key<?>, CacheResult<?>> result = deserializeEntry(serKey, valueOpt.get());

    /* Return the CacheResult */

    @SuppressWarnings("unchecked") CacheResult<V> cv = (CacheResult<V>) result.getValue();
    return cv;
  }

  @Override
  public <V extends @Nullable Object> void registerOnChange(AccessContext pAccessContext, KeySPI<V> pKey,
    Consumer3<Key<V>, CacheKeyEvent, Optional<@NonNull V>> pCallback) {

    var list = mCallbacks.computeIfAbsent(pKey.toString(), (_) -> new CopyOnWriteArrayList<>());
    @SuppressWarnings(
      { "unchecked", "rawtypes" }) Consumer3<Key<?>, CacheKeyEvent, Optional<?>> callback = (Consumer3<Key<?>, CacheKeyEvent, Optional<?>>) (Consumer3) pCallback;
    list.add(new CallbackInfo(pAccessContext, callback));
  }

  @Override
  public void setCacheEngine(CacheEngine pCacheEngine) {
    mCacheEngine = pCacheEngine;
  }

  @Override
  public void handleEvent(Object pKey, CacheKeyEvent pEvent, @Nullable Object pValue) {
    @SuppressWarnings("unchecked") SER_KEY serKey = (SER_KEY) pKey;

    /* If the key doesn't start with the value prefix, then this is likely metadata, so that's not relevant */

    if ((mMetaCache == null) && (mValuePrefix != null)) {
      if (mKeyDeserializer != null) {
        if (!mKeyDeserializer.apply(serKey).startsWith(mValuePrefix)) return;
      } else {
        if (!((String) serKey).startsWith(mValuePrefix)) return;
      }
    }

    var entry = deserializeEntry(serKey, pValue);

    @SuppressWarnings("unchecked") KeySPI<Object> keyObj = (KeySPI<Object>) entry.getKey();

    /* Because this might be called on a non-reentrant thread, move the querying for real data into another thread */

    mExecutorService.submit(() -> {
      if (mCacheEngine == null) throw new IllegalStateException("The cache engine has not been set");
      callCallbacks(keyObj, pEvent);
    });
  }

  private static class NULL_TYPE_CLASS {
    // empty
  }
}
