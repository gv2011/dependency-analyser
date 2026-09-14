package com.github.gv2011.dependencyanalyser.impl;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.DependencyNode;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;

public final class Formatter {


    /**
     * Formats ArtifactIdentity into "groupId:artifactId:type[:classifier]"
     */
    public String formatArtifactIdentity(final ArtifactIdentity identity) {
        final StringBuilder sb = new StringBuilder();
        sb.append(identity.groupId()).append(":")
          .append(identity.artifactId()).append(":")
          .append(identity.type());

        identity.classifier().ifPresentDo(c -> sb.append(":").append(c));

        return sb.toString();
    }

    /**
     * Formats MavenCoordinates into "groupId:artifactId:type[:classifier]:version"
     */
    public String formatMavenCoordinates(final MavenCoordinates coordinates) {
        return formatArtifactIdentity(coordinates.identity()) + ":" + coordinates.version();
    }

    /**
     * Formats a single DependencyNode into "groupId:artifactId:type[:classifier]:version[:scope][ (inherited)]"
     * (excluding child dependencies).
     */
    public String formatNodeSingleLine(final DependencyNode node) {
        final StringBuilder sb = new StringBuilder();
        sb.append(formatMavenCoordinates(node.coordinates()));

        node.scope().ifPresentDo(scope -> sb.append(":").append(scope.name().toLowerCase()));

        if (node.isInherited()) {
            sb.append(" (inherited)");
        }

        return sb.toString();
    }
}