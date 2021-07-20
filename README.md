![BuildStatus](https://github.com/ISAITB/itb-commons/actions/workflows/main.yml/badge.svg)
![Coverage](.github/badges/jacoco.svg)
[![licence](https://img.shields.io/github/license/ISAITB/itb-commons.svg)](https://github.com/ISAITB/itb-commons/blob/master/LICENCE.txt)

# ITB commons

The **ITB commons** project includes the modules used to define common libraries shared across ITB validators. Each module is set to 
produce a separate JAR that can be included as a dependency via Maven.

The following validator implementations currently make use this library:
* The [RDF validator](https://hub.docker.com/r/isaitb/shacl-validator), for the validation of RDF data using [SHACL](https://www.w3.org/TR/shacl/).
* The [XML validator](https://hub.docker.com/r/isaitb/xml-validator), for the validation of XML data using [XML Schema](https://www.w3.org/standards/xml/schema.html) and [Schematron](https://schematron.com/).
* The [JSON validator](https://hub.docker.com/r/isaitb/json-validator), for the validation of JSON data using [JSON Schema](https://json-schema.org/).
* The [CSV validator](https://hub.docker.com/r/isaitb/csv-validator), for the validation of CSV data using [Table Schema](https://specs.frictionlessdata.io/table-schema/).

This library is maintained by the **European Commission's DIGIT** and specifically the **Interoperability Test Bed**,
a conformance testing service for projects involved in the delivery of cross-border public services. Find out more 
[here](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

# Usage

Each module's library is intended to be used as a dependency for the modules of specific validator projects. See the
following sections on the purpose and usage of each library. In the illustrated dependencies you should replace 
`VERSION` with the specific snapshot or release version needed.

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
* A JDK installation (11+).
* Maven (3+)

## Useful Maven commands

Use the following Maven commands to carry out typical actions:
* Build and install all libraries: `mvn install -DskipTests=true`
* Run unit tests with coverage: `mvn test`
* Build javadocs: `mvn javadoc:javadoc javadoc:aggregate`

# Licence

This software is shared using the [European Union Public Licence (EUPL) version 1.2](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12).

# Legal notice

The authors of this library waive any and all liability linked to its usage or the interpretation of results produced
by its downstream validators.

# Contact

For feedback or questions regarding this library you are invited to post issues in the current repository. In addition,
feel free to contact the Test Bed team via email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

# See also

The ITB commons libraries are used in the Test Bed's validators. Check these out for more
information:
* The **RDF validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/rdf-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/) and [Docker Hub image](https://hub.docker.com/r/isaitb/shacl-validator).
* The **XML validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/xml-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingXML/) and [Docker Hub image](https://hub.docker.com/r/isaitb/xml-validator).
* The **JSON validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/json-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingJSON/) and [Docker Hub image](https://hub.docker.com/r/isaitb/json-validator).
* The **CSV validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/csv-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingCSV/) and [Docker Hub image](https://hub.docker.com/r/isaitb/csv-validator).