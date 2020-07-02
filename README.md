# ITB commons

This project includes the modules used to define common libraries shared across ITB projects. Each module is set to 
produce a separate JAR that can be included as a dependency via maven.

# Library use

The defines modules (libraries) are listed in the following sections. Specific releases are not currently produced
given that validators are operated with a "always latest" approach. As such all version of included libraries are
set to `1.0.0-SNAPSHOT`.

## validation-commons

Common beans, base classes and utilities for all validators (i.e. the validators' "common" layer/module). The main 
points addressed by this library are file management operations and validator configuration. To include define in 
your `pom.xml` the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## validation-commons-war

Common utilities linked to packaging validators as web applications (wars). To include define in your `pom.xml` 
the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-war</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## validation-commons-web

Common beans, base classes and utilities for the web layer of validators. This library also includes the web resources
(JS, CSS) and Thymeleaf templates (plus reusable fragments) for the validators UI. To include define in your `pom.xml`
the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-commons-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## validation-plugins

Provides custom plugin support for validators. To include define in your `pom.xml` the following dependency:

```
<dependency>
    <groupId>eu.europa.ec.itb.commons</groupId>
    <artifactId>validation-plugins</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

To fully support plugins in a validator, apart from including this dependency (which by the way would be transitively brought in also through the `validation-commons` library),
you need to do the following:
1. Define a bean of type `PluginConfigProvider`. A default implementation for this is also provided by `DomainPluginConfigProvider` from `validation-commons` that loads plugin
   configuration from the domain configuration file.
2. As part of the validator's specific validation process, use the `PluginManager` to load the plugins to execute and call them.

The approach to pass input and the expected output from the plugin are specific to the type of validator. These need to be documented in the specific validator's README file.

# Library development

Development of the library requires eventually publishing built artifacts to the ITB development repository. For 
information on the configuration needed to do this (i.e. the distribution repository) check the project's Confluence 
space (https://citnet.tech.ec.europa.eu/CITnet/confluence/display/ITB).