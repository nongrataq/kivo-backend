package com.kivo.backend.common.configuration;

import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class WebMvcConfiguration {

  @Bean
  public MessageSource exceptionsMessageSource() {
    return getMessageSource("exceptions");
  }

  @Bean
  public MessageSource validationMessageSource() {
    return getMessageSource("validation");
  }

  @Bean
  public AcceptHeaderLocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(Locale.of("ru", "RU"));
    localeResolver.setSupportedLocales(List.of(Locale.of("ru", "RU"), Locale.of("en", "US")));
    return localeResolver;
  }

  private MessageSource getMessageSource(String basename) {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:localization/%s/%s".formatted(basename, basename));
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setCacheSeconds(3600);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }
}
