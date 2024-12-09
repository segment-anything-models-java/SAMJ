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

# Big images
SAM pre-processing includes re-sizing the image to 1024x1024, thus in images bigger than that, every detail smaller than `original _size / 1024` pixel(s) will become subpixel and thus disappear. In order to mantain the performance on big images,SAMJ adds a layer of logic on top of the SAM models.

SAMJ logic depends on the size of the input image. If both sides of the input image are smaller than 512, the image is considered to be "small", thus no extra logic will be used to process the image. The stanndard workflow first encodes the image of interest. This operation is computationally expensive and can take several seconds (up to a minute) depending on the hardware. Due to the cost of this operation, it is just done once, and the resulting encoding is used as many times as the user wants to provide real-time reactivity.

The other operation required to create a mask is the prompt encoding. This requires the user interacts and gives a prompt, in the form of a point, a list of points or a bounding box (rectangle). The prompts are encoded, combined with the image encodings and then decoded into a mask. The process if fast and lightweight, thus it can be done in real-time at the same time that the user is providing more prompts.

On the other hand, big images cannot be fully encoded at once as it would lose its resolution, leading to the smaller objects disappearing.

