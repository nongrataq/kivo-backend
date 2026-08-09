package com.kivo.backend.common.localization;

import java.util.Locale;

public final class LanguageResolver {

  private LanguageResolver() {}

  public static String resolve(String requestedLanguage, String defaultLanguage) {
    String language = requestedLanguage == null ? defaultLanguage : requestedLanguage;
    return language.toLowerCase(Locale.ROOT);
  }
}
