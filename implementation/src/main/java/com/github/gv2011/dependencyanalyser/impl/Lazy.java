package com.github.gv2011.dependencyanalyser.impl;

import java.util.function.Supplier;

/**
 * Computes a value at most once - on first access, from whichever thread
 * gets there first - and caches it for every call after that, including
 * from other threads. Standard double-checked-locking lazy
 * initialization: safe under the Java Memory Model because value is
 * volatile, and the second, synchronized check prevents two threads
 * racing to compute it both actually doing so.
 */
final class Lazy<T> {

  private final Supplier<T> supplier;
  private volatile T value;

  Lazy(final Supplier<T> supplier) {
    this.supplier = supplier;
  }

  T get() {
    T result = value;
    if(result==null) {
      synchronized(this) {
        result = value;
        if(result==null) {
          value = result = supplier.get();
        }
      }
    }
    return result;
  }

}
