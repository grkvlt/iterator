IFS Explorer
============

Iterated Function System Explorer.

![viewer](http://grkvlt.github.io/iterator/images/viewer-overlay-grid.png)

See <http://grkvlt.github.io/iterator/> for further documentation.

## Features

- Live transform editor
  - Move, scale and rotate with mouse
  - Edit transform properties
  - Set affine matrix
  - Update configuration
- Live reflection editor
- IFS renderer
  - Colour and greyscale
  - IFS, measure set and fractal top modes
  - Density estimation and Fractal Flame
  - Printing and export to PNG
- IFS details view
  - Transform matrix and reflection co-ordinates
  - HTML rendering
- Save and load as XML
- Animated changes to IFS

## Program Requirements

- Java 1.8.x Runtime Environment
- Windows, Linux or OSX Operating System
- Maven and 1.8.x JDK to build

## TODO

- Extract startup and config code to separate class
- Better Operating-System integration
  - Native full-screen mode
  - Support native windowing system features
- Improved co-ordinate system
  - Allow non-square aspect ratio
  - Use unit vectors as basis
  - Support co-ordinate transforms (polar or log)
- Extend platform support
  - Embedded applet
  - Android application (?)
- Animator enhancements
  - Document usage
  - JSON configuration file parser
  - Headless operation
  - Add camera movements (zoom and pan)
  - Graphical sequence editor

Alongside these enhancements and new features, some of which are
currently work-in-progress, both performance and UX improvements
are always on-going.

----
Copyright 2012-2017 by [Andrew Donald Kennedy](mailto:andrew.international+iterator@gmail.com) and
Licensed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
