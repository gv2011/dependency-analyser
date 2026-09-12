package com.example.majorupdates.transport;

import java.util.List;

import javax.annotation.Nullable;

/**
 * The whole report's list of {@link MajorUpdate}s, wrapped in a named field rather than emitted
 * as a bare top-level JSON array. A bare top-level array can't have sibling metadata added later
 * without a breaking change; a named field inside a top-level object can -- {@link #pluginVersion}
 * is exactly that kind of addition: metadata about the run, not about any individual update.
 *
 * <p>{@link #pluginVersion} is {@code @Nullable String}, not {@code Optional<String>} -- the
 * same choice, and for the same reason, as {@code MajorUpdatesMojo}'s own fields in the plugin
 * module: this is a boundary type crossing into a mechanism (here, Gson JSON serialization; there,
 * Maven's field-injection) that doesn't understand {@code Optional}. Gson omits a {@code null}
 * field from the JSON entirely rather than writing it as {@code null} (default behavior, no
 * {@code serializeNulls()} configured), so a missing plugin version genuinely means the property
 * is absent from the JSON, not present-with-null -- callers building or reading one of these
 * convert to/from {@code Optional<String>} right at this boundary (see {@code JsonFormatter} in
 * the plugin module, and {@code MajorUpdatesInvoker} in the invoker module).
 *
 * <p>{@code List}, not {@code ImmutableList} -- this module stays dependency-light (Gson only),
 * and Gson serializes a plain {@link List} field natively either way, so nothing is gained by
 * pulling in Guava just for this. A caller building one from an {@code ImmutableList} can pass
 * it directly; {@code ImmutableList} already implements {@link List}.
 *
 * @param pluginVersion the actual version of major-updates-maven-plugin that produced this
 *                       report -- read from that plugin's own {@code pom.properties} at the
 *                       point it runs, not supplied by the caller. Absent (and so, omitted from
 *                       the JSON) when that plugin's own pom.properties isn't available yet --
 *                       e.g. before the package phase; see JsonFormatter's own javadoc. Lets a
 *                       consumer (see major-updates-invoker) verify it actually got results from
 *                       the version it expected, rather than silently trusting whatever plugin
 *                       version happened to be resolved.
 * @param updates       the updates found, in whatever order they were produced
 */
public record UpdateResponse(@Nullable String pluginVersion, List<MajorUpdate> updates) {}