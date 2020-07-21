package com.binchencoder.skylb.demo.config;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterInstanceFactory;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

/**
 * Convert a string to an Level.
 *
 * @author binchencoder
 */
public class LevelConverter extends BaseConverter<Level> {

  public LevelConverter(String optionName) {
    super(optionName);
  }

  public Level convert(String value) {
    try {
      return Level.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ParameterException(getErrorString(value, "an Level"));
    }
  }

  public static class LevelConverterInstanceFactory implements IStringConverterInstanceFactory {

    @Override
    public IStringConverter<?> getConverterInstance(Parameter parameter, Class<?> forType,
        String optionName) {
      if (forType == Level.class) {
        return new LevelConverter(optionName);
      }
      return null;
    }
  }
}
