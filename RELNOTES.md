# Version 1.4.1

The main feature added in this release is co-ordinate transformations,
which modify the IFS display by applying a function to the result of each
iteration.

This release also updates the threading and concurrency code, which has
increased the performance of the IFS rendering engine. Configuration
preferences can now be saved, and extra properties including a gamma
correction factor and an iteration limit have been added.

The code has been refactored extensively to use more Java 8 idioms, and
various display and other issues have been fixed.

## Issues resolved

- Display correct cursors for transform corners
- Format doubles with no trailing zeros
- Better name capitalisation
- Fix grid line drawing when scaled
- Transform matrix field now uses copy of array
- Update name without reset of view

## New Features

- Final co-ordinate transforms
- Gamma correction
- Thread management
- Saving configuration
- Performance improvements
- Launch script memory options
- Gestures in OSX

### Features added in 1.3.0

- More rendering modes
- Bug fixes in display and editing
- Updated documentation

### Features added in 1.2.0

- Reflections added as function type
- Density estimation rendering modes
- Context menu for viewer pane
- Zoom editing dialog

See <http://grkvlt.github.io/iterator/> for further documentation.
----
Copyright 2012-2017 by [Andrew Donald Kennedy](mailto:andrew.international+iterator@gmail.com) and
Licensed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
