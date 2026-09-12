package com.example.majorupdates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.majorupdates.Dependency.DefaultDependency;
import com.example.majorupdates.Dependency.DependencyKind;
import org.junit.jupiter.api.Test;

class CsvFormatterTest {

    @Test
    void formatsRowAsCommaSeparatedLine() {
        final org.apache.maven.model.Dependency mavenDependency = new org.apache.maven.model.Dependency();
        mavenDependency.setGroupId("com.fasterxml.jackson.core");
        mavenDependency.setArtifactId("jackson-databind");
        mavenDependency.setVersion("2.15.2");

        final Dependency dependency = new DefaultDependency(DependencyKind.DEPENDENCY, mavenDependency);
        final UpdateRow row = new UpdateRow("payment-service", dependency, Version.parse("3.0.0"));

        assertEquals("payment-service,DEPENDENCY,com.fasterxml.jackson.core,jackson-databind,2.15.2,3.0.0",
                new CsvFormatter().format(row));
    }

    @Test
    void headerMatchesFieldOrderInFormat() {
        assertEquals("module,kind,groupId,artifactId,current,latest", new CsvFormatter().header());
    }
}