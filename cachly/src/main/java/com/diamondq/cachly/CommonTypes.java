package com.diamondq.cachly;

import com.diamondq.common.TypeReference;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class CommonTypes {

  /**
   * Constant for string argument.
   */
  public static final Type STRING               = new TypeReference<String>() {
                                                }.getType();

  /**
   * Constant for int argument. Used by generated code, do not remove.
   */
  public static final Type INT                  = new TypeReference<Integer>() {
                                                }.getType();

  /**
   * Constant for long argument. Used by generated code, do not remove.
   */
  public static final Type LONG                 = new TypeReference<Long>() {
                                                }.getType();

  /**
   * Constant for float argument. Used by generated code, do not remove.
   */
  public static final Type FLOAT                = new TypeReference<Float>() {
                                                }.getType();

  /**
   * Constant for double argument. Used by generated code, do not remove.
   */
  public static final Type DOUBLE               = new TypeReference<Double>() {
                                                }.getType();

  /**
   * Constant for void argument. Used by generated code, do not remove.
   */
  public static final Type VOID                 = new TypeReference<Void>() {
                                                }.getType();

  /**
   * Constant for byte argument. Used by generated code, do not remove.
   */
  public static final Type BYTE                 = new TypeReference<Byte>() {
                                                }.getType();

  /**
   * Constant for boolean argument. Used by generated code, do not remove.
   */
  public static final Type BOOLEAN              = new TypeReference<Boolean>() {
                                                }.getType();

  /**
   * Constant char argument. Used by generated code, do not remove.
   */
  public static final Type CHAR                 = new TypeReference<Character>() {
                                                }.getType();

  /**
   * Constant short argument. Used by generated code, do not remove.
   */
  public static final Type SHORT                = new TypeReference<Short>() {
                                                }.getType();

  /**
   * Default Object argument. Used by generated code, do not remove.
   */
  public static final Type OBJECT_ARGUMENT      = new TypeReference<Object>() {
                                                }.getType();

  /**
   * Constant for List<String> argument.
   */
  public static final Type LIST_OF_STRING       = new TypeReference<List<String>>() {
                                                }.getType();

  public static final Type MAP_STRING_TO_STRING = new TypeReference<Map<String, String>>() {
                                                }.getType();
}
