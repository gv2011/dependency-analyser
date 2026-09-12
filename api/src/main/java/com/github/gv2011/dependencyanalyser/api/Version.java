package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.icol.Opt;
import com.github.gv2011.util.tstr.TypedString;

public interface Version extends TypedString<Version>{

  Opt<Integer> major();

}
