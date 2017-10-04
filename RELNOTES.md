# Version 1.5.0

This release moves all the configuration properties from the main
application class, extracts the iteration algorithm from the viewer, and
refactors the code to use them. This enables headless rendering of an IFS
to be performed with a new IFS Renderer application, and for the IFS
Animator application to run more efficiently. The IFS Animator now
supports modifying relections, and changing the configuration values
for each rendered segment. The console output routines have also been
refactored to give a consistent look and feel to the text output.

## New Features

- New headless Renderer application
- Support for reflections in Animator
- Support configuration changes in Animator
- Consistent console text output
- Gradient colour mode

### Features added in 1.4.x

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

See <http://grkvlt.github.io/iterator/> for further documentation.

---
Copyright 2012-2017 by [Andrew Donald Kennedy](mailto:andrew.international+iterator@gmail.com) and
Licensed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
