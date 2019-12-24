package com.binchencoder.common.jcommander;

import com.beust.jcommander.converters.BaseConverter;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.beust.jcommander.converters.URIConverter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ListURIConverter extends BaseConverter<List<URI>> {

  private final CommaParameterSplitter splitter = new CommaParameterSplitter();

  public ListURIConverter(String optionName) {
    super(optionName);
  }

  @Override
  public List<URI> convert(String value) {
    List<String> strUris = splitter.split(value);
    List<URI> uris = new ArrayList<>(strUris.size());
    for (String uri : strUris) {
      uris.add(new URIConverter(this.getOptionName()).convert(uri));
    }
    return uris;
  }
}
