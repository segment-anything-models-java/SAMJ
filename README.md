[![Build Status](https://github.com/segment-anything-models-java/SAMJ/actions/workflows/build.yml/badge.svg)](https://github.com/segment-anything-models-java/SAMJ/actions/workflows/build.yml)



# SAMJ: Segment-Anything-Model for Java software
SAMJ is a Java based package that brings the [Segment-Anything-Model (SAM)](https://github.com/facebookresearch/segment-anything) to Java software. Thanks to [Appose](https://github.com/apposed/appose) SAMJ enables the installation and use of different variants of SAM.

SAMJ provides a simple API for installation and inference of SAM models.

# Quickstart
The inly thing you need to incorporate SAMJ to your software is to add the dependency with Maven to the `pom.xml` file:

```
<dependency>
	<groupId>ai.nets</groupId>
	<artifactId>samj</artifactId>
	<version>0.0.3-SNAPSHOT</version>
</dependency>
```

