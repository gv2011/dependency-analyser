package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.ex.Exceptions.notYetImplemented;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.Opt;
import com.github.gv2011.util.tstr.AbstractTypedString;

final class VersionImpl extends AbstractTypedString<Version> implements Version{

  static Version parse(final String string) {
    return new VersionImpl(string);
  }

  private final String version;

  private VersionImpl(final String version){
    this.version = version;
  }

  @Override
  public Version self() {
    return this;
  }

  @Override
  public Class<Version> clazz() {
    return Version.class;
  }

  @Override
  public Opt<Integer> major() {
    return notYetImplemented();
  }

  @Override
  public String toString() {
    return version;
  }

}
