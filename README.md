![Banner](https://www.itb.ec.europa.eu/files/banners/itb_banner.png?v=1)

# ITB commons

![BuildStatus](https://github.com/ISAITB/itb-commons/actions/workflows/main.yml/badge.svg)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=coverage)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=security_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_itb-commons&metric=bugs)](https://sonarcloud.io/summary/overall?id=ISAITB_itb-commons)
[![licence](https://img.shields.io/github/license/ISAITB/itb-commons.svg?color=blue)](https://github.com/ISAITB/itb-commons/blob/master/LICENCE.txt)
[![Gurubase](https://img.shields.io/badge/Gurubase-Ask%20ITB%20Guru-006BFF?color=blue)](https://gurubase.io/g/itb)

The **ITB commons** project includes the modules used to define common libraries shared across ITB validators. Each module is set to 
produce a separate JAR that can be included as a dependency via Maven.

The following validator implementations currently make use this library:
* The [RDF validator](https://github.com/ISAITB/shacl-validator), for the validation of RDF data using [SHACL](https://www.w3.org/TR/shacl/).
* The [XML validator](https://github.com/ISAITB/xml-validator), for the validation of XML data using [XML Schema](https://www.w3.org/standards/xml/schema.html) and [Schematron](https://schematron.com/).
* The [JSON validator](https://github.com/ISAITB/json-validator), for the validation of JSON data using [JSON Schema](https://json-schema.org/).
* The [CSV validator](https://github.com/ISAITB/csv-validator), for the validation of CSV data using [Table Schema](https://specs.frictionlessdata.io/table-schema/).

This library is maintained by the **European Commission's DIGIT** and specifically the **Interoperability Test Bed**,
a conformance testing service for projects involved in the delivery of cross-border public services. Find out more 
[here](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

# Usage

Each module's library is intended to be used as a dependency for the modules of specific validator projects. See the
following sections on the purpose and usage of each library. In the illustrated dependencies you should replace 
`VERSION` with the specific snapshot or release version needed.

## commons-parent

Module that acts as the parent POM for the current project's modules. A specific module is used for this purpose
to restrict build behaviours to the current project only, without being inherited by specific validator
implementations. Common build settings (properties and dependency versions) are inherited from the root POM file.
To use this as a parent, module POMs should define it as follows:

```
<parent>
   <groupId>eu.europa.ec.itb.commons</groupId>
   <artifactId>validation-commons-parent</artifactId>
   <version>VERSION</version>
   <relativePath>../commons-parent</relativePath>
</parent>
```

## validator-parent

Module that acts as the parent POM for validator implementations external to the current project.  Common build 
settings (properties and dependency versions) are inherited from the root POM file. To use this as a parent, validator 
POMs should define it as follows:

```
<parent>
   <groupId>eu.europa.ec.itb.commons</groupId>
   <artifactId>validator-parent</artifactId>
   <version>1.2.0-SNAPSHOT</version>
   <relativePath/>
</parent>
```

## validation-commons

Common beans, base classes and utilities for all validators (i.e. the validators' "common" layer/module). The main 
points addressed by this library are file management operations and validator configuration. To include define in 
your `pom.xml` the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons</artifactId>
    <version>VERSION</version>
</dependency>
```

## validation-commons-war

Common utilities linked to packaging validators as web applications (wars). To include define in your `pom.xml` 
the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-war</artifactId>
    <version>VERSION</version>
</dependency>
```

## validation-commons-web

Common beans, base classes and utilities for the web layer of validators. This library also includes the web resources
(JS, CSS) and Thymeleaf templates (plus reusable fragments) for the validators' UI. To include define in your `pom.xml`
the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-web</artifactId>
    <version>VERSION</version>
</dependency>
```

## validation-plugins

Provides custom plugin support for validators. To include define in your `pom.xml` the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-plugins</artifactId>
    <version>VERSION</version>
</dependency>
```

To fully support plugins in a validator, apart from including this dependency (which by the way would be transitively brought in also through the `validation-commons` library),
you need to do the following:
1. Define a bean of type `PluginConfigProvider`. A default implementation for this is also provided by `DomainPluginConfigProvider` from `validation-commons` that loads plugin
   configuration from the domain configuration file.
2. As part of the validator's specific validation process, use the `PluginManager` to load the plugins to execute and call them.

The approach to pass input and the expected output from the plugin are specific to the type of validator. These need to be documented in the specific validator's README file.

# Building

You can clone and build the ITB commons libraries locally as all dependencies are publicly available. The typical reason
you would do this would be in the context of making a local validator build for which you would need to already have 
installed the ITB commons dependencies. Currently, ITB commons libraries are not published on a public Maven repository.  

## Prerequisites

To build this project's libraries you require:
* A JDK installation (17+).
* Maven (3+)

## Useful Maven commands

Use the following Maven commands to carry out typical actions:
* Build and install all libraries: `mvn install -DskipTests=true`
* Run unit tests with coverage: `mvn test`
* Build javadocs: `mvn javadoc:javadoc javadoc:aggregate`

# Licence

This software is shared using the [European Union Public Licence (EUPL) version 1.2](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12).

Third-party library licences and attributions are listed in [NOTICE.md](NOTICE.md).

# Legal notice

The authors of this library waive any and all liability linked to its usage or the interpretation of results produced
by its downstream validators.

# Contact

For feedback or questions regarding this library you are invited to post issues in the current repository. In addition,
feel free to contact the Test Bed team via email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

# See also

The ITB commons libraries are used in the Test Bed's validators. Check these out for more
information:
* The **RDF validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/rdf-validator), [source](https://github.com/ISAITB/shacl-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/) and [Docker Hub image](https://hub.docker.com/r/isaitb/shacl-validator).
* The **XML validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/xml-validator), [source](https://github.com/ISAITB/xml-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingXML/) and [Docker Hub image](https://hub.docker.com/r/isaitb/xml-validator).
* The **JSON validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/json-validator), [source](https://github.com/ISAITB/json-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingJSON/) and [Docker Hub image](https://hub.docker.com/r/isaitb/json-validator).
* The **CSV validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/csv-validator), [source](https://github.com/ISAITB/csv-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingCSV/) and [Docker Hub image](https://hub.docker.com/r/isaitb/csv-validator).