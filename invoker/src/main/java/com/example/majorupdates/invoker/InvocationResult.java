package com.example.majorupdates.invoker;

import com.example.majorupdates.transport.MajorUpdate;
import com.google.common.collect.ImmutableList;

/**
 * @param updates     the updates the plugin run found
 * @param mavenOutput the full captured Maven stdout+stderr from the invocation -- always
 *                     present, not just on failure, so a caller can inspect it for debugging
 *                     without it ever having been printed to the real console. This class never
 *                     prints it itself; whether to use it at all is entirely up to the caller.
 */
public record InvocationResult(ImmutableList<MajorUpdate> updates, String mavenOutput) {}
