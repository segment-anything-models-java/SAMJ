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

# Available SAM variants
The original SAM is a heavy and computationally expensive model. Its original version requires a GPU as it might take up to several minutes in standard CPUs and might completely freeze lower end hardware. In order to make SAMJ supported by any hardware, we decided to rely in variants of SAM that are light enough to be runnable on any kind of CPU but still keep most of the performance. These variants are:
* [**SAM-2**](https://github.com/facebookresearch/sam2) (tiny, small and large): newest and improved version of SAM released by Meta. Not only this version reduces a lot the computation cost, it also improves the performance. To do so, this model uses a new variant of Vision Transofrmer (ViT), the [Hiera ViT](https://github.com/facebookresearch/hiera).
* [**EfficientSAM**](https://github.com/yformer/EfficientSAM): small Vision Transformer trained by distilling the original SAM Huge transformer using Masked Autoencoders.
* [**EfficientViTSAM**](https://github.com/mit-han-lab/efficientvit): SAM like model that uses a special lightweigth image encoder, EfficientViT.
