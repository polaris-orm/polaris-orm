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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Class that exposes the version. Fetches the
 * "Implementation-Version" manifest attribute from the jar file.
 *
 * @author TODAY 2021/10/11 23:28
 * @since 1.0
 */
public record Version(int major, int minor, int micro, String type, int step,
        @Nullable String extension, String implementationVersion) {

  public static final String Draft = "Draft";
  public static final String Alpha = "Alpha";
  public static final String Beta = "Beta";
  public static final String RELEASE = "RELEASE";
  public static final String SNAPSHOT = "SNAPSHOT";

  public static final Version instance;

  static {
    String implementationVersion = forClass(Version.class);
    if (implementationVersion != null) {
      instance = parse(implementationVersion);
    }
    else {
      instance = new Version(0, 0, 0, RELEASE, 0, null, "Unknown");
      System.err.println("Version cannot get 'implementationVersion' in manifest.");
    }
  }

  /**
   * parse {@link Version},
   * version format: {major}.{minor}.{micro}-{type}.{step}-{extension}
   *
   * @param implementationVersion 'implementationVersion' in manifest
   */
  static Version parse(String implementationVersion) {
    String type;
    String extension = null;
    int major;
    int minor;
    int micro;
    int step = 0;

    String[] split = implementationVersion.split("-");

    if (split.length == 1) {
      type = RELEASE;
    }
    else {
      if (split.length == 3) {
        extension = split[2]; // optional
      }

      type = split[1];
      String[] typeSplit = type.split("\\.");
      if (typeSplit.length == 2) {
        type = typeSplit[0];
        step = Integer.parseInt(typeSplit[1]);
      }
    }

    String ver = split[0];
    String[] verSplit = ver.split("\\.");
    major = Integer.parseInt(verSplit[0]);
    minor = Integer.parseInt(verSplit[1]);
    micro = Integer.parseInt(verSplit[2]);

    return new Version(major, minor, micro, type, step, extension, implementationVersion);
  }

  @Override
  public String toString() {
    return "v" + implementationVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Version version))
      return false;
    return Objects.equals(implementationVersion, version.implementationVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(implementationVersion);
  }

  /**
   * @see Package#getImplementationVersion()
   */
  public static Version get() {
    return instance;
  }

  /**
   * Return the version information for the provided {@link Class}.
   *
   * @param cls the Class to retrieve the version for
   * @return the version, or {@code null} if a version can not be extracted
   */
  @Nullable
  public static String forClass(Class<?> cls) {
    String implementationVersion = cls.getPackage().getImplementationVersion();
    if (implementationVersion != null) {
      return implementationVersion;
    }
    URL codeSourceLocation = cls.getProtectionDomain().getCodeSource().getLocation();
    try {
      URLConnection connection = codeSourceLocation.openConnection();
      if (connection instanceof JarURLConnection jarURLConnection) {
        return getImplementationVersion(jarURLConnection.getJarFile());
      }
      try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
        return getImplementationVersion(jarFile);
      }
    }
    catch (Exception ex) {
      return null;
    }
  }

  private static String getImplementationVersion(JarFile jarFile) throws IOException {
    return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
  }
}
