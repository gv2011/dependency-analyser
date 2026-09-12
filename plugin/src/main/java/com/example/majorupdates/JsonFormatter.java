package com.example.majorupdates;

import java.util.Optional;

import com.example.majorupdates.transport.MajorUpdate;
import com.example.majorupdates.transport.UpdateResponse;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

final class JsonFormatter {

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  String format(final Optional<String> pluginVersion, final ImmutableList<UpdateRow> rows) {
    final ImmutableList<MajorUpdate> results = rows.stream()
      .map(JsonFormatter::toResult)
      .collect(ImmutableList.toImmutableList())
    ;
    return gson.toJson(new UpdateResponse(pluginVersion.orElse(null), results));
  }

  private static MajorUpdate toResult(final UpdateRow row) {
    return new MajorUpdate(
      row.module(),
      row.dependency().kind().name(),
      row.dependency().groupId(),
      row.dependency().artifactId(),
      row.dependency().version().map(Version::toString).orElse(""),
      row.latestMajorVersion().toString()
    );
  }

}