package com.diamondq.cachly.micronaut.ehcache;

import com.diamondq.cachly.Cache;
import com.diamondq.cachly.micronaut.ValueName;
import com.diamondq.common.converters.ConverterManager;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.ehcache.spi.persistence.StateHolder;
import org.ehcache.spi.persistence.StateRepository;
import org.ehcache.spi.serialization.SerializerException;
import org.ehcache.spi.serialization.StatefulSerializer;

@Singleton
public class CachlyEhcacheSerializer implements StatefulSerializer<ValueName<?>> {

  private final ConverterManager                          mConverterManager;

  private @Nullable StateHolder<String, Integer>          mGroupStateHolder   = null;

  private final ConcurrentMap<String, Integer>            mGroupNameToInt     = new ConcurrentHashMap<>();

  private final ConcurrentMap<Integer, String>            mIntToGroupName     = new ConcurrentHashMap<>();

  private final ConcurrentMap<String, Integer>            mClassNameToInt     = new ConcurrentHashMap<>();

  private final ConcurrentMap<Integer, String>            mIntToClassName     = new ConcurrentHashMap<>();

  private final ConcurrentMap<Integer, Class<@NonNull ?>> mIntToClass         = new ConcurrentHashMap<>();

  private final AtomicInteger                             mGroupIdCounter     = new AtomicInteger(0);

  private final AtomicInteger                             mClassNameIdCounter = new AtomicInteger(0);

  private @Nullable StateHolder<String, Integer>          mClassNameStateHolder;

  @Inject
  public CachlyEhcacheSerializer(ConverterManager pConverterManager) {
    mConverterManager = pConverterManager;
  }

  /**
   * @see org.ehcache.spi.serialization.Serializer#serialize(java.lang.Object)
   */
  @Override
  public ByteBuffer serialize(@Nullable ValueName<?> pObject) throws SerializerException {
    if (pObject == null)
      throw new IllegalStateException();
    String groupName = Cache.DEFAULT_SERIALIZER.equals(pObject.name) ? null : pObject.name;
    Integer groupId;
    if (groupName == null)
      groupId = 0;
    else {
      groupId = mGroupNameToInt.get(groupName);
      if (groupId == null) {
        synchronized (this) {
          groupId = mGroupNameToInt.get(groupName);
          if (groupId == null) {
            groupId = mGroupIdCounter.incrementAndGet();
            mGroupNameToInt.put(groupName, groupId);
            mIntToGroupName.put(groupId, groupName);
            Objects.requireNonNull(mGroupStateHolder).putIfAbsent(groupName, groupId);
          }
        }
      }
    }
    Object value = pObject.value;
    if (value == null)
      throw new IllegalStateException();
    Class<@NonNull ?> clazz = value.getClass();
    String className = clazz.getName();
    Integer classNameId = mClassNameToInt.get(className);
    if (classNameId == null) {
      synchronized (this) {
        classNameId = mClassNameToInt.get(className);
        if (classNameId == null) {
          classNameId = mClassNameIdCounter.incrementAndGet();
          mClassNameToInt.put(className, classNameId);
          mIntToClassName.put(classNameId, className);
          mIntToClass.put(classNameId, clazz);
          Objects.requireNonNull(mClassNameStateHolder).putIfAbsent(className, classNameId);
        }
      }
    }

    ByteBuffer buffer = mConverterManager.convert(value, ByteBuffer.class, groupName);
    buffer.rewind();
    ByteBuffer result = ByteBuffer.allocate(buffer.limit() + 4 + 4);
    result.putInt(groupId);
    result.putInt(classNameId);
    result.put(buffer);
    result.rewind();
    return result;
  }

  /**
   * @see org.ehcache.spi.serialization.Serializer#read(java.nio.ByteBuffer)
   */
  @Override
  public @Nullable ValueName<?> read(ByteBuffer pBinary) throws ClassNotFoundException, SerializerException {
    int groupId = pBinary.getInt();
    int classNameId = pBinary.getInt();
    String groupName;
    if (groupId == 0)
      groupName = null;
    else {
      groupName = mIntToGroupName.get(groupId);
      if (groupName == null)
        return null;
    }
    Class<@NonNull ?> clazz = mIntToClass.get(classNameId);
    if (clazz == null) {
      String className = mIntToClassName.get(classNameId);
      if (className == null)
        return null;
      clazz = Class.forName(className);
    }
    ByteBuffer data = pBinary.slice();
    Object obj = mConverterManager.convert(data, clazz, groupName);
    return new ValueName<Object>(obj, groupName == null ? Cache.DEFAULT_SERIALIZER : groupName);
  }

  /**
   * @see org.ehcache.spi.serialization.Serializer#equals(java.lang.Object, java.nio.ByteBuffer)
   */
  @Override
  public boolean equals(ValueName<?> pObject, ByteBuffer pBinary) throws ClassNotFoundException, SerializerException {
    ByteBuffer dup = pBinary.duplicate();
    ValueName<?> vn = read(dup);
    return Objects.equals(pObject, vn);
  }

  /**
   * @see org.ehcache.spi.serialization.StatefulSerializer#init(org.ehcache.spi.persistence.StateRepository)
   */
  @Override
  public void init(StateRepository pStateRepository) {

    /* Get the group name state */

    synchronized (this) {
      StateHolder<String, Integer> groupNameStateHolder = pStateRepository.getPersistentStateHolder("CachlyGroupNames",
        String.class, Integer.class, (p) -> true, CachlyEhcacheSerializer.class.getClassLoader());
      mGroupStateHolder = groupNameStateHolder;
      int largestGroupId = 0;
      for (Map.Entry<String, Integer> pair : groupNameStateHolder.entrySet()) {
        int groupId = pair.getValue();
        if (groupId > largestGroupId)
          largestGroupId = groupId;
        mGroupNameToInt.put(pair.getKey(), groupId);
        mIntToGroupName.put(groupId, pair.getKey());
      }
      mGroupIdCounter.set(largestGroupId);

      /* Get the class name state */

      StateHolder<String, Integer> classNameStateHolder = pStateRepository.getPersistentStateHolder("CachlyClassNames",
        String.class, Integer.class, (p) -> true, CachlyEhcacheSerializer.class.getClassLoader());
      mClassNameStateHolder = classNameStateHolder;
      int largestClassNameId = 0;
      for (Map.Entry<String, Integer> pair : classNameStateHolder.entrySet()) {
        int classNameId = pair.getValue();
        if (classNameId > largestClassNameId)
          largestClassNameId = classNameId;
        mClassNameToInt.put(pair.getKey(), classNameId);
        mIntToClassName.put(classNameId, pair.getKey());
      }
      mClassNameIdCounter.set(largestClassNameId);
    }

  }

}
