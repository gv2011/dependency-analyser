package com.example.majorupdates.transport;

/**
 * One dependency/plugin with a major-version update available -- a flat, Maven-API-free shape
 * for JSON output.
 *
 * <p>Deliberately NOT the same type as the plugin's own internal {@code UpdateRow} (which holds
 * a {@code Dependency}/{@code Version} domain object directly) -- this type has no dependency on
 * any Maven API at all, which is what lets {@code major-updates-shared} stay a minimal, pure
 * data module. The plugin maps its own richer, internal result into this shape at the point it
 * needs to produce JSON; see its own {@code JsonFormatter}.
 *
 * @param module         the Maven module in this reactor that declares the entry
 * @param kind           where the entry is declared -- the plain name of the domain
 *                       {@code DependencyKind} enum constant, not the enum itself
 * @param groupId        groupId of the dependency or plugin
 * @param artifactId     artifactId of the dependency or plugin
 * @param currentVersion the version currently declared, as a plain string
 * @param latestVersion  the newest available version with a different major segment, as a plain
 *                       string
 */
public record MajorUpdate(
  String module,
  String kind,
  String groupId,
  String artifactId,
  String currentVersion,
  String latestVersion
) {}
