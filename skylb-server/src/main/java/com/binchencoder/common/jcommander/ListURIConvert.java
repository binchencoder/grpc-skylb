package com.binchencoder.common.jcommander;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import com.beust.jcommander.converters.CommaParameterSplitter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ListURIConvert extends BaseConverter<List<URI>> {

  private final CommaParameterSplitter splitter = new CommaParameterSplitter();

  public ListURIConvert(String optionName) {
    super(optionName);
  }

  @Override
  public List<URI> convert(String value) {
    List<String> strUris = splitter.split(value);
    List<URI> uris = new ArrayList<>(strUris.size());
    for (String uri : strUris) {
      try {
        uris.add(new URI(uri));
      } catch (URISyntaxException e) {
        throw new ParameterException(getErrorString(uri, "an List<URI>"));
      }
    }
    return uris;
  }
}
