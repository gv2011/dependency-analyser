package com.example.majorupdates;

/**
 * One dependency/plugin with a major-version update available.
 * @param module the Maven module in this reactor where the dependency is declared
 * @param latestMajorVersion the latest major version, newer than the dependencies version
 */
record UpdateRow(
  String module,
  Dependency dependency,
  Version latestMajorVersion
) {}
