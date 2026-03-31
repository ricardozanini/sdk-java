/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public final class CollectionConversionUtils {
  private CollectionConversionUtils() {}

  /**
   * Safely converts a base Collection into the requested List, Set, or Array type.
   *
   * @param elements The base collection of elements.
   * @param clazz The target class to convert to.
   * @param primitiveConverter Strategy for converting items to primitives if an array is requested.
   */
  public static <T> Optional<T> as(
      Collection<?> elements,
      Class<T> clazz,
      BiFunction<Object, Class<?>, Object> primitiveConverter) {
    if (clazz.isAssignableFrom(List.class))
      return Optional.of(clazz.cast(new ArrayList<>(elements)));
    else if (clazz.isAssignableFrom(Set.class))
      return Optional.of(clazz.cast(new HashSet<>(elements)));

    if (clazz.isArray()) {
      Class<?> componentType = clazz.getComponentType();

      if (!componentType.isPrimitive()) {
        Object[] typedArray = (Object[]) Array.newInstance(componentType, 0);
        return Optional.of(clazz.cast(elements.toArray(typedArray)));
      }

      Object primitiveArray = Array.newInstance(componentType, elements.size());

      int i = 0;
      for (Object item : elements)
        Array.set(primitiveArray, i++, primitiveConverter.apply(item, componentType));

      return Optional.of(clazz.cast(primitiveArray));
    }

    return Optional.empty();
  }

  /**
   * @see #as(Collection, Class, BiFunction)
   */
  public static <T> Optional<T> as(Collection<?> elements, Class<T> clazz) {
    return as(elements, clazz, (item, type) -> item);
  }
}
