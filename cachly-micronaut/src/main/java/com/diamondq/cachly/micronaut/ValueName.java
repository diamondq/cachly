package com.diamondq.cachly.micronaut;

public class ValueName<V> {

  public V      value;

  public String name;

  @SuppressWarnings("null")
  public ValueName() {
  }

  public ValueName(V pValue, String pName) {
    value = pValue;
    name = pName;
  }
}
