/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.polaris;

import cn.taketoday.polaris.util.Nullable;

/**
 * for page query
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/3/31 15:09
 * @see Page
 */
public interface Pageable {

  /**
   * Returns the current page to be returned.
   *
   * @return the current page to be returned.
   */
  int pageNumber();

  /**
   * Returns the number of items to be returned.
   *
   * @return the number of items of that page
   */
  int pageSize();

  /**
   * Returns the number of offset in database
   *
   * @return the number of offset in database
   */
  default int offset() {
    return (pageNumber() - 1) * pageSize();
  }

  /**
   * Returns the number of offset in database
   *
   * @return the number of offset in database
   */
  default int offset(int max) {
    return (pageNumber() - 1) * pageSize(max);
  }

  /**
   * Returns the number of items with max limit to be returned.
   *
   * @return the number of items with max limit of that page
   */
  default int pageSize(int max) {
    return Math.min(pageSize(), max);
  }

  /**
   * Create Simple pageable instance
   *
   * @param pageSize page size
   * @param pageNumber current page number
   */
  static Pageable of(int pageNumber, int pageSize) {
    return new SimplePageable(pageNumber, pageSize);
  }

  /**
   * unwrap
   */
  @Nullable
  static Pageable unwrap(@Nullable Object source) {
    return source instanceof Pageable pageable ? pageable : null;
  }

}
