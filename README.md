# Packages Extractor
[![license](https://img.shields.io/github/license/concision/packages-unpacker?style=flat-square)](https://github.com/concision/packages-unpacker/blob/master/LICENSE)
![version](https://img.shields.io/github/v/tag/concision/packages-unpacker?style=flat-square)

A CLI tool designed to easily extract data manifests from an unnamed game's package cache.

> Note: While reading through the following documentation, you may sense a significant amount of ambiguity or abstractness - this is intentional, due to the nature of the project. 

## Motivations
The associated unnamed game provides a public API for obtaining game data that allows the community to develop useful tools for the playerbase. However, there are several distinct issues with the current support:
- **coverage**: there is only a very limited subset of useful game data publicly available
- **missing information**: for the subset of publicly available data, some data attributes are arbitrarily missing
- **outdated**: exposed data is, more than often, outdated by months or even years
- **disinterest**: Unnamed game's developers are not particularly interested in devoting time to build a more exhaustive API

> Note: The data referred to here relates to various aspects of the game's mechanics, not to be confused with any association with user data.

This data exists within each installation of the game; although, it may only be obtained by reading the game's proprietary package format. There currently exists no (working) tool that is built that is able to extract this desired data. Ideally, a flexible tool could automate this extraction process, allowing developers to build useful applications from the extracted data. 

Upon request by a few acquaintances, this project was designed and implemented.

## Solution
A platform-independent CLI tool was promptly written that supports data extraction for manifest files. 

This project's implementation solves a few issues in previously known implementations:
- ease of portability (Java is a quite popular platform-independent runtime)
- reduced memory footprint (only a working subset of assets are loaded into memory)
- support for exporting the data to JSON, a more well known and usable data format (the internal manifests have a near 1-to-1 mapping to JSON)
- increased code readability (e.g. control structure)

This Java project is broken up into two submodules:
- **io-api**: An api to directly manipulate data formats from I/O streams
- **cli**: A tool that wraps the I/O api in an easy-to-use CLI application

> Note: This project's ability to read the proprietary data formats is based off of [cheahjs](https://github.com/cheahjs)'s work. Their public contributions in the community are indisputably invaluable for creating this tool.

## Ambiguity
As noted before, this documentation is rather abstract and ambiguous; this is intentional to prevent SEO of this repository. As such, most references to the game have been sanitized or obfuscated such that they do not appear when using certain terms on a search engine.

The effective goal here is to prevent the project from being accidentally stumbled upon by an irresponsible individual, but is still publicly available to individuals who are made aware of the project.

### Compilation
Instructions are intentionally not included for compiling this project. Regardless, this task should be relatively straightforward to anyone with experience with build systems.

Effectively, project compilation is left as an exercise to the reader.

## CLI Usage
Command usage is available with ```unpacker -h```; however, it is provided below for convenience:
```
usage: unpacker [-h] [--verbose] --source-type {ORIGIN,INSTALL,FOLDER,BINARY} [--source-location [PATH]]
                [--output-location [PATH]] --output-format FORMAT [--output-raw]
                [/glob/**/pattern/*file* [/glob/**/pattern/*file* ...]]

Extracts and processes data from Packages.bin

positional arguments:
  /glob/**/pattern/*file*
                         List of packages to extract using glob patterns (default: "**/*")

named arguments:
  -h, --help             show this help message and exit

flags:
  --verbose              Verbosely logs information to standard error stream

source:
  --source-type {ORIGIN,INSTALL,FOLDER,BINARY}
                         Method of obtaining a Packages.bin data source
                         ORIGIN: Streams directly from update servers
                         INSTALL: Searches for last used install location
                         FOLDER: Specifies [REDACTED] folder (specify a --source-location DIRECTORY)
                         BINARY: Specifies a raw extracted Packages.bin file (specify a --source-location FILE)
  --source-location [PATH]
                         A path to a source location on the filesystem, if required by the specified --source-type

output:
  Specifies how output is controlled; there are two types of outputs:
  SINGLE: Outputs all relevant package records into single output (e.g. file, stdout)
          if '--output-location FILE' is specified, writes to file, otherwise to stdout
  MULTIPLE: Outputs relevant package records into multiple independent files
            '--output-location DIRECTORY' argument must be specified

  --output-location [PATH]
                         Output path destination
  --output-format FORMAT
                         Specifies the output format
                         SINGLE:
                           PATHS: Outputs matching package paths on each line
                                  (e.g. /Path/.../PackageName\r\n)
                           RECORDS: Outputs a matching package JSON record on each line
                                    (e.g. {"path": "/Path/...", "package": ...}\r\n)
                           MAP: Outputs all matching packages into a JSON map
                                (e.g. {"/Path/...": ..., ...})
                           LIST: Outputs all matching packages into a JSON array
                                 (e.g. [{"path": "/Path/...", "package": ...}, ...])
                         MULTIPLE:
                           RECURSIVE: Outputs each matching package as a file with replicated directory structure
                                      (e.g. ${--output-location}/Path/.../PackageName)
                           FLATTENED: Outputs each matching package as a file without replicating directory structure
                                      (e.g. ${--output-location}/PackageName)
  --output-raw           Skips conversion of LUA Tables to JSON (default: false)

In lieu of a package list, a file containing a list may be specified with "@file"
```
