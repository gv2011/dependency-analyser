package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.ex.Exceptions.call;

import java.nio.file.Path;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;

/**
 * Reads the effective settings the way {@code mvn} would find them without
 * command-line options:
 * <ul>
 *   <li>user settings: {@code ${user.home}/.m2/settings.xml}</li>
 *   <li>global settings: {@code ${maven.home}/conf/settings.xml}, only if the
 *     system property {@code maven.home} is set</li>
 *   <li>local repository: from the settings, else
 *     {@code ${user.home}/.m2/repository}</li>
 * </ul>
 * Missing files are skipped. Encrypted passwords are not decrypted, repositories
 * from settings profiles are not used.
 */
final class SettingsReader {

  Settings read() {
    final Path userHome = Path.of(System.getProperty("user.home"));
    final String mavenHome = System.getProperty("maven.home");
    final DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
    request.setUserSettingsFile(userHome.resolve(".m2").resolve("settings.xml").toFile());
    request.setGlobalSettingsFile(
      mavenHome == null ? null : Path.of(mavenHome, "conf", "settings.xml").toFile()
    );
    request.setSystemProperties(System.getProperties());
    final Settings settings = call(() -> new DefaultSettingsBuilderFactory().newInstance().build(request))
      .getEffectiveSettings()
    ;
    settings.setLocalRepository(
      settings.getLocalRepository() == null
      ? userHome.resolve(".m2").resolve("repository").toString()
      : settings.getLocalRepository()
    );
    return settings;
  }

}
