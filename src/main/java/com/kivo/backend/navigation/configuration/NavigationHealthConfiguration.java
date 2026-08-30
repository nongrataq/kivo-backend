package com.kivo.backend.navigation.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NavigationReadinessProperties.class)
public class NavigationHealthConfiguration {}
