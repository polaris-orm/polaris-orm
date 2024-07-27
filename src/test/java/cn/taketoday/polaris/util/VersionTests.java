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

package cn.taketoday.polaris.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/26 21:41
 */
class VersionTests {

  @Test
  void parse() {
    Version.get();

    // 4.0.0-Draft.1  latest  4.0.0-Beta.1 -Alpha.1 -Draft.1 -SNAPSHOT
    Version version = Version.parse("4.0.0-Draft.1");

    assertThat(version.type()).isEqualTo(Version.Draft);
    assertThat(version.step()).isEqualTo(1);
    assertThat(version.major()).isEqualTo(4);
    assertThat(version.minor()).isEqualTo(0);
    assertThat(version.micro()).isEqualTo(0);
    assertThat(version.extension()).isNull();

    // release
    version = Version.parse("4.0.0");
    assertThat(version.type()).isEqualTo(Version.RELEASE);
    assertThat(version.step()).isEqualTo(0);

    // Beta
    version = Version.parse("4.0.0-Beta");
    assertThat(version.type()).isEqualTo(Version.Beta);
    assertThat(version.step()).isEqualTo(0);

    // Beta with step
    version = Version.parse("4.0.0-Beta.3");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Beta);

    // Alpha
    version = Version.parse("4.0.0-Alpha");
    assertThat(version.type()).isEqualTo(Version.Alpha);

    // Alpha with step
    version = Version.parse("4.0.0-Alpha.3");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);

    // extension
    version = Version.parse("4.0.0-Alpha.3-jdk8");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);
    assertThat(version.extension()).isEqualTo("jdk8");

    // extension
    version = Version.parse("4.0.0-Alpha.3-SNAPSHOT");
    assertThat(version.step()).isEqualTo(3);
    assertThat(version.type()).isEqualTo(Version.Alpha);
    assertThat(version.extension()).isEqualTo(Version.SNAPSHOT);

  }

  @Test
  void toString_() {
    assertThat(Version.parse("4.0.0-Draft.1").toString()).isEqualTo("v4.0.0-Draft.1");
  }

  @Test
  void hashCode_() {
    Set<Version> versions = new HashSet<>(Set.of(Version.parse("4.0.0-Draft.1")));
    assertThat(versions.contains(Version.parse("4.0.0-Draft.1"))).isTrue();
    assertThat(versions.contains(Version.parse("4.0.1-Draft.1"))).isFalse();
  }

}