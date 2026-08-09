package com.kivo.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.kivo.backend");

  @Test
  void commonDoesNotDependOnFeatureModules() {
    noClasses()
        .that()
        .resideInAPackage("..common..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..catalog..", "..kiosks..", "..maps..")
        .check(CLASSES);
  }

  @Test
  void applicationDoesNotDependOnApi() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .check(CLASSES);
  }

  @Test
  void controllersDoNotDependOnPersistenceEntities() {
    noClasses()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..persistence.entity..")
        .check(CLASSES);
  }

  @Test
  void repositoriesArePrivateToTheirFeatureModule() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..catalog..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..catalog.persistence.repository..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideOutsideOfPackage("..kiosks..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..kiosks.persistence.repository..")
        .check(CLASSES);
  }

  @Test
  void featurePackagesAreFreeOfCycles() {
    slices()
        .matching("com.kivo.backend.(*)..")
        .should()
        .beFreeOfCycles()
        .ignoreDependency(
            "com.kivo.backend.KivoBackendApplication", "org.springframework.boot.SpringApplication")
        .check(CLASSES);
  }
}
