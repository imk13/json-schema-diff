package com.github.jsonschemadiff.utils;

import java.util.Objects;

public class Edge<V, T> {

  private final V source;
  private final V target;
  private final T value;

  public Edge(V source, V target, T value) {
    this.source = source;
    this.target = target;
    this.value = value;
  }

  public Edge<V, T> reverse() {
    return new Edge<>(target(), source(), value());
  }

  public V source() {
    return source;
  }

  public V target() {
    return target;
  }

  public T value() {
    return value;
  }

  @Override
  public String toString() {
    return "Edge{src=" + source + ",tgt=" + target + ",val=" + value + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Edge<?, ?> that = (Edge<?, ?>) o;
    return Objects.equals(source, that.source)
        && Objects.equals(target, that.target)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target, value);
  }
}
