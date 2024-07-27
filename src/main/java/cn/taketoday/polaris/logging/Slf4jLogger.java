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

package cn.taketoday.polaris.logging;

import org.slf4j.spi.LocationAwareLogger;

import java.io.Serial;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
class Slf4jLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  protected final String name;

  private final transient org.slf4j.Logger target;

  Slf4jLogger(org.slf4j.Logger target) {
    super(target.isDebugEnabled());
    this.target = target;
    this.name = target.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && target.isTraceEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return target.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return target.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return target.isErrorEnabled();
  }

  @Override
  public String getName() {
    return target.getName();
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
    final String msg = MessageFormatter.format(format, args);
    switch (level) {
      case DEBUG -> target.debug(msg, t);
      case ERROR -> target.error(msg, t);
      case TRACE -> target.trace(msg, t);
      case WARN -> target.warn(msg, t);
      default -> target.info(msg, t);
    }
  }

  @Serial
  protected Object readResolve() {
    return Slf4jLoggerFactory.createLog(this.name);
  }
}

final class LocationAwareSlf4jLogger extends Slf4jLogger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final LocationAwareLogger log;

  public LocationAwareSlf4jLogger(LocationAwareLogger log) {
    super(log);
    this.log = log;
  }

  private static int getLevel(Level level) {
    return switch (level) {
      case DEBUG -> LocationAwareLogger.DEBUG_INT;
      case ERROR -> LocationAwareLogger.ERROR_INT;
      case TRACE -> LocationAwareLogger.TRACE_INT;
      case WARN -> LocationAwareLogger.WARN_INT;
      default -> LocationAwareLogger.INFO_INT;
    };
  }

  @Override
  protected void logInternal(Level level, String format, Throwable t, Object[] args) {
    log.log(null, FQCN, getLevel(level), format, args, t);
  }
}

final class Slf4jLoggerFactory extends LoggerFactory {

  Slf4jLoggerFactory() {
    org.slf4j.Logger.class.getName();
  }

  @Override
  protected Logger createLogger(String name) {
    return createLog(name);
  }

  static Logger createLog(String name) {
    org.slf4j.Logger target = org.slf4j.LoggerFactory.getLogger(name);
    return target instanceof LocationAwareLogger ?
            new LocationAwareSlf4jLogger((LocationAwareLogger) target) : new Slf4jLogger(target);
  }

}
