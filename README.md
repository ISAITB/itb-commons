![Banner](https://www.itb.ec.europa.eu/files/banners/itb_banner.png?v=1)

# ITB commons

[![BuildStatus](https://github.com/ISAITB/itb-commons/actions/workflows/main.yml/badge.svg)](https://github.com/ISAITB/itb-commons/actions/workflows/main.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=coverage)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=security_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=bugs)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![licence](https://img.shields.io/github/license/ISAITB/itb-commons.svg?color=blue)](https://github.com/ISAITB/itb-commons/blob/master/LICENCE.txt)
[![Gurubase](https://img.shields.io/badge/Gurubase-Ask%20ITB%20Guru-006BFF?color=blue)](https://gurubase.io/g/itb)

**ITB commons** is the set of shared Java libraries on which all [Interoperability Test Bed](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed)
validators are built. Each module produces a separate JAR that you can include as a Maven dependency.

These validators use the libraries:

* The [RDF validator](https://github.com/ISAITB/shacl-validator), for validating RDF data using [SHACL](https://www.w3.org/TR/shacl/).
* The [XML validator](https://github.com/ISAITB/xml-validator), for validating XML data using [XML Schema](https://www.w3.org/standards/xml/schema.html) and [Schematron](https://schematron.com/).
* The [JSON validator](https://github.com/ISAITB/json-validator), for validating JSON and YAML data using [JSON Schema](https://json-schema.org/).
* The [CSV validator](https://github.com/ISAITB/csv-validator), for validating CSV data using [Table Schema](https://specs.frictionlessdata.io/table-schema/).

This library is maintained by the **European Commission's DIGIT** and specifically the **Interoperability Test Bed**,
a conformance testing service for projects involved in the delivery of cross-border public services. Learn more on the
[Interoperability Test Bed page](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

## Modules

| Module                     | Purpose                                                                                       |
|----------------------------|-----------------------------------------------------------------------------------------------|
| `validation-commons-parent` | Parent POM for the modules of this project.                                                  |
| `validator-parent`         | Parent POM for validator implementations outside this project.                                |
| `validation-commons`       | Common beans, base classes and utilities (file management, validator configuration).          |
| `validation-commons-web`   | Common web layer: base classes, JS/CSS resources and Thymeleaf templates for the validators' UI. |
| `validation-commons-war`   | Utilities for packaging validators as web applications.                                       |
| `validation-commons-jar`   | Utilities for packaging validators as command-line tools.                                     |
| `validation-commons-report`| Generation of validation reports as PDF.                                                      |
| `validation-plugins`       | Support for custom validator plugins.                                                         |

In the dependency snippets below, replace `VERSION` with the snapshot or release version you need.

### validation-commons-parent

The parent POM for this project's own modules. It is a separate module so that its build behaviour is not inherited by
validator implementations. Common build settings (properties and dependency versions) come from the root POM file.
To use it as a parent, a module's POM should contain:

```xml
<parent>
   <groupId>eu.europa.ec.itb.commons</groupId>
   <artifactId>validation-commons-parent</artifactId>
   <version>VERSION</version>
   <relativePath>../commons-parent</relativePath>
</parent>
```

### validator-parent

The parent POM for validator implementations outside this project. Common build settings (properties and
dependency versions) come from the root POM file. To use it as a parent, a validator's POM should contain:

```xml
<parent>
   <groupId>eu.europa.ec.itb.commons</groupId>
   <artifactId>validator-parent</artifactId>
   <version>VERSION</version>
   <relativePath/>
</parent>
```

### validation-commons

Common beans, base classes and utilities for all validators (the validators' "common" layer). It mainly covers
file management and validator configuration. To use it, add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons</artifactId>
    <version>VERSION</version>
</dependency>
```

### validation-commons-web

Common beans, base classes and utilities for the validators' web layer. It also includes the web resources
(JS, CSS) and Thymeleaf templates, with reusable fragments, for the validators' UI.

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-web</artifactId>
    <version>VERSION</version>
</dependency>
```

### validation-commons-war

Common utilities for packaging validators as web applications.

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-war</artifactId>
    <version>VERSION</version>
</dependency>
```

### validation-commons-jar

Common utilities for packaging validators as command-line tools, including the base classes for running validations
and reporting their results from the command line.

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-jar</artifactId>
    <version>VERSION</version>
</dependency>
```

### validation-commons-report

Support for generating validation reports as PDF documents.

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-report</artifactId>
    <version>VERSION</version>
</dependency>
```

### validation-plugins

Support for custom validator plugins.

```xml
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-plugins</artifactId>
    <version>VERSION</version>
</dependency>
```

To fully support plugins in a validator you need to do the following. The dependency itself is also brought in
transitively by `validation-commons`.

1. Define a bean of type `PluginConfigProvider`. `validation-commons` provides a default implementation,
   `DomainPluginConfigProvider`, that loads plugin configuration from the domain configuration file.
2. In the validator's own validation process, use the `PluginManager` to load the plugins and call them.

The input passed to plugins and the output expected from them depend on the type of validator. Each validator's README
documents these.

## Building

ITB commons is **not published on a public Maven repository**. To build a validator locally you must first
build and install these libraries yourself. All their dependencies are publicly available.

### Prerequisites

* A JDK installation (17+).
* Maven (3+).

### Useful Maven commands

| Action                                  | Command                                |
|-----------------------------------------|----------------------------------------|
| Build and install all libraries         | `mvn install -DskipTests=true`         |
| Run unit tests with coverage            | `mvn test`                             |
| Build javadocs                          | `mvn javadoc:javadoc javadoc:aggregate` |

## Security

To report a security vulnerability, please follow this repository's [security policy](SECURITY.md).

## Contributing

The best way to contribute is to share your feedback, questions and suggestions by [opening an issue](https://github.com/ISAITB/itb-commons/issues)
in this repository. For more elaborate contributions, and for more information, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

This software is shared using the [European Union Public Licence (EUPL) version 1.2](https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12).

Third-party library licences and attributions are listed in [NOTICE.md](NOTICE.md).

## Legal notice

The authors of this library waive any and all liability linked to its usage or the interpretation of results produced
by its downstream validators.

## Contact

For feedback or questions about this library, please post an issue in this repository. You can also contact the Test Bed
team by email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

## See also

ITB commons is used in the Test Bed's validators. Check these out for more information:

* The **RDF validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/rdf-validator), [source](https://github.com/ISAITB/shacl-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/) and [Docker Hub image](https://hub.docker.com/r/isaitb/shacl-validator).
* The **XML validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/xml-validator), [source](https://github.com/ISAITB/xml-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingXML/) and [Docker Hub image](https://hub.docker.com/r/isaitb/xml-validator).
* The **JSON validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/json-validator), [source](https://github.com/ISAITB/json-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingJSON/) and [Docker Hub image](https://hub.docker.com/r/isaitb/json-validator).
* The **CSV validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/csvvalidator), [source](https://github.com/ISAITB/csv-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingCSV/) and [Docker Hub image](https://hub.docker.com/r/isaitb/csv-validator).
