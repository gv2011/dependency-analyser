package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.DependencyNode;

class VerboseTreeParserTest {

    @Test
    void testParseRootOnly() {
        final String input = "com.example:my-app:jar:1.0.0";

        final VerboseTreeParser parser = new VerboseTreeParser();
        final DependencyNode root = parser.parseVerboseTree(input);

        assertThat(
            "Parsed root artifactId should be my-app",
            root.coordinates().identity().artifactId(),
            equalTo("my-app")
        );
    }

    @Test
    void testThrowsOnInfoPrefix() {
        final String input = "[INFO] com.example:my-app:jar:1.0.0";
        final VerboseTreeParser parser = new VerboseTreeParser();

        assertThrows(
            IllegalArgumentException.class,
            () -> parser.parseVerboseTree(input),
            "Parser should throw IllegalArgumentException when line starts with [INFO]"
        );
    }

    @Test
    void testParseCleanTree() {
        final String input = """
            com.example:my-app:jar:1.0.0
            +- org.mockito:mockito-core:jar:5.23.0:test
            |  \\- net.bytebuddy:byte-buddy:jar:1.14.12:test
            """;

        final VerboseTreeParser parser = new VerboseTreeParser();
        final DependencyNode root = parser.parseVerboseTree(input);

        assertThat(
            "Root artifactId should be my-app",
            root.coordinates().identity().artifactId(),
            equalTo("my-app")
        );

        assertThat(
            "Root should have 1 child dependency",
            root.children(),
            hasSize(1)
        );
    }

@Test
    void testCalculateDepthForRootAndChildren() {
        assertThat(
            "Root line with no indentation or connector should be depth 0",
            VerboseTreeParser.calculateDepth("", null),
            equalTo(0)
        );

        assertThat(
            "Direct dependency with '+- ' connector should be depth 1",
            VerboseTreeParser.calculateDepth("", "+- "),
            equalTo(1)
        );

        assertThat(
            "Transitive dependency with '|  \\- ' prefix should be depth 2",
            VerboseTreeParser.calculateDepth("|  ", "\\- "),
            equalTo(2)
        );

        assertThat(
            "Transitive dependency nested two levels deep '|  |  +- ' should be depth 3",
            VerboseTreeParser.calculateDepth("|  |  ", "+- "),
            equalTo(3)
        );
    }
}