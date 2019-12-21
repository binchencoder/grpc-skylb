package com.binchencoder.common;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterInstanceFactory;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import java.time.Duration;

/**
 * Convert a string to an Duration.
 *
 * @author binchencoder
 */
public class DurationConverter extends BaseConverter<Duration> {

  public DurationConverter(String optionName) {
    super(optionName);
  }

  public Duration convert(String value) {
    try {
      return DurationStyle.detectAndParse(value);
    } catch (IllegalArgumentException ex) {
      throw new ParameterException(getErrorString(value, "an duration"));
    }
  }

  public static class DurationConverterInstanceFactory implements IStringConverterInstanceFactory {

    @Override
    public IStringConverter<?> getConverterInstance(Parameter parameter, Class<?> forType,
        String optionName) {
      if (forType == Duration.class) {
        return new DurationConverter("duration");
      }
      return null;
    }
  }
}

