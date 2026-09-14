package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.util.BeanUtils;
import com.github.gv2011.util.StringUtils;

public class DependencyListParser {

//    public record Dependency(
//            String group,
//            String id,
//            String type,
//            String classifier,
//            String version,
//            String scope
//    ) {}

    // Regex matching standard Maven output format: groupId:artifactId:type[:classifier]:version:scope
    private static final Pattern DEP_PATTERN = Pattern.compile(
            "^\\s*([^:\\s]+):([^:\\s]+):([^:\\s]+)(?::([^:\\s]+))?:([^:\\s]+):([^:\\s]+)"
    );

    public static List<Dependency> parseDependencyListFile(final Path filePath) throws IOException {
        try (var lines = Files.lines(filePath)) {
            return lines
                    .map(DependencyListParser::parseLine)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private static Dependency parseLine(final String line) {
        final Matcher matcher = DEP_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        final String group = matcher.group(1);
        final String id = matcher.group(2);
        final String type = matcher.group(3);
        final String pos4 = matcher.group(4);
        final String pos5 = matcher.group(5);
        final String pos6 = matcher.group(6);

        String classifier;
        String version;
        String scope;

        if (pos6 != null) {
            // All 6 parts present: groupId:artifactId:type:classifier:version:scope
            classifier = pos4;
            version = pos5;
            scope = pos6;
        } else {
            // 5 parts present: groupId:artifactId:type:version:scope
            classifier = null;
            version = pos4;
            scope = pos5;
        }

        return BeanUtils.beanBuilder(Dependency.class)
          .set(Dependency::coordinates)
          .to(BeanUtils.beanBuilder(MavenCoordinates.class)

            .build()
          )
          .set(Dependency::scope).to(MavenScope.valueOf(StringUtils.toUpperCase(scope)))
          .build()
        ;
//        new Dependency(group, id, type, classifier, version, scope);
    }
}