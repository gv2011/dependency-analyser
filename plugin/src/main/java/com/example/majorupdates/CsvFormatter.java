package com.example.majorupdates;

/**
 * Renders {@link UpdateRow}s as CSV. The only class in this plugin that knows the output is CSV --
 * everything else works with {@link UpdateRow} and doesn't care how it's eventually written.
 *
 * <p>Example data line:
 * {@code payment-service,DEPENDENCY,com.fasterxml.jackson.core,jackson-databind,2.15.2,3.0.0}
 */
final class CsvFormatter {

  String header() {
      return "module,kind,groupId,artifactId,current,latest";
  }

  String format(final UpdateRow row) {
    return String.join(
      ",",
      row.module(),
      row.dependency().kind().name(),
      row.dependency().groupId(),
      row.dependency().artifactId(),
      row.dependency().version().map(Version::toString).orElse(""),
      row.latestMajorVersion().toString()
    );
  }
}
