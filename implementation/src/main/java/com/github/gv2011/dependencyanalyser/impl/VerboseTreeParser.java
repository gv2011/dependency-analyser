package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.DependencyNode;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

public class VerboseTreeParser {

    private static final Pattern NODE_PATTERN = Pattern.compile(
            "^(?<indent>(?:[| ]\\s+)*)(?<connector>[+\\\\-]- )?(?<raw>[^\\s]+)"
    );

    public DependencyNode parseVerboseTree(final Path filePath) throws IOException {
        return parseVerboseTree(Files.readAllLines(filePath));
    }

    public DependencyNode parseVerboseTree(final String content) {
        try (final BufferedReader reader = new BufferedReader(new StringReader(content))) {
            return parseVerboseTree(reader.lines().toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException("Could not read tree content", e);
        }
    }

    public DependencyNode parseVerboseTree(final List<String> lines) {
        final Deque<NodeBuilder> stack = new ArrayDeque<>();
        NodeBuilder root = null;

        for (final String line : lines) {
            if (line.trim().startsWith("[INFO]")) {
                throw new IllegalArgumentException("Line contains unsupported '[INFO]' prefix: " + line);
            }

            final Matcher matcher = NODE_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            final String indent = matcher.group("indent");
            final String connector = matcher.group("connector");
            final int depth = calculateDepth(indent, connector);
            final String raw = matcher.group("raw");

            final ParsedNodeTokens tokens = parseTokens(raw);

            final boolean isDirect = (depth == 1);
            final boolean isInherited = isDirect && line.contains("(inherited)");

            final NodeBuilder node = new NodeBuilder(
                    tokens.coordinates(),
                    tokens.scope(),
                    isDirect,
                    isInherited
            );

            if (depth == 0) {
                root = node;
                stack.push(node);
            } else {
                while (stack.size() > depth) {
                    stack.pop();
                }
                if (!stack.isEmpty()) {
                    stack.peek().addChild(node);
                }
                stack.push(node);
            }
        }

        if (root == null) {
            throw new IllegalArgumentException("Could not parse a valid root DependencyNode");
        }

        return root.build();
    }

    static int calculateDepth(final String indent, final String connector) {
        final int baseIndentDepth = (indent == null || indent.isEmpty()) ? 0 : indent.length() / 3;
        final int connectorDepth = (connector != null && !connector.isEmpty()) ? 1 : 0;
        return baseIndentDepth + connectorDepth;
    }

    private static ParsedNodeTokens parseTokens(final String raw) {
        final String[] parts = raw.split(":");
        final String groupId = parts[0];
        final String artifactId = parts[1];
        final String type = parts[2];

        final String classifier;
        final String versionStr;
        final Opt<MavenScope> scope;

        if (parts.length == 4) {
            classifier = null;
            versionStr = parts[3];
            scope = Opt.empty();
        } else if (parts.length == 5) {
            classifier = null;
            versionStr = parts[3];
            scope = Opt.of(MavenScope.valueOf(parts[4].toUpperCase()));
        } else if (parts.length == 6) {
            classifier = parts[3];
            versionStr = parts[4];
            scope = Opt.of(MavenScope.valueOf(parts[5].toUpperCase()));
        } else {
            throw new IllegalArgumentException("Unrecognized Maven coordinate format: " + raw);
        }

        final ArtifactIdentity identity = beanBuilder(ArtifactIdentity.class)
                .set(ArtifactIdentity::groupId).to(groupId)
                .set(ArtifactIdentity::artifactId).to(artifactId)
                .set(ArtifactIdentity::type).to(type)
                .set(ArtifactIdentity::classifier).to(Opt.ofNullable(classifier))
                .build();

        final Version version = VersionImpl.parse(versionStr);

        final MavenCoordinates coordinates = beanBuilder(MavenCoordinates.class)
                .set(MavenCoordinates::identity).to(identity)
                .set(MavenCoordinates::version).to(version)
                .build();

        return new ParsedNodeTokens(coordinates, scope);
    }

    private record ParsedNodeTokens(MavenCoordinates coordinates, Opt<MavenScope> scope) {}

    private static class NodeBuilder {
        private final MavenCoordinates coordinates;
        private final Opt<MavenScope> scope;
        private final boolean direct;
        private final boolean inherited;
        private final List<NodeBuilder> childBuilders = new ArrayList<>();

        private NodeBuilder(final MavenCoordinates coordinates, final Opt<MavenScope> scope, final boolean direct, final boolean inherited) {
            this.coordinates = coordinates;
            this.scope = scope;
            this.direct = direct;
            this.inherited = inherited;
        }

        public void addChild(final NodeBuilder child) {
            this.childBuilders.add(child);
        }

        private DependencyNode build() {
            final ISet<DependencyNode> children =
              childBuilders.stream().map(NodeBuilder::build).collect(ICollections.toISet());

            return beanBuilder(DependencyNode.class)
                    .set(DependencyNode::coordinates).to(coordinates)
                    .set(DependencyNode::scope).to(scope)
                    .set(DependencyNode::isDirect).to(direct)
                    .set(DependencyNode::isInherited).to(inherited)
                    .set(DependencyNode::children).to(children)
                    .build();
        }
    }
}