package com.diamondq.cachly;

import org.jspecify.annotations.Nullable;

/**
 * Represents a placeholder filled by an Access Context
 *
 * @param <O> the key type
 */
public interface AccessContextPlaceholder<O extends @Nullable Object> extends Key<O> {
}
