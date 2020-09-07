<h1 align="center">
    Packages Extraction Toolchain
</h1>

<p align="center">
    <a href="https://github.com/concision/packages-unpacker/blob/master/LICENSE">
        <img alt="repository license" src="https://img.shields.io/github/license/concision/packages-unpacker?style=for-the-badge"/>
    </a>
    <a href="https://github.com/concision/unpacker-api/releases">
        <img alt="release version" src="https://img.shields.io/github/v/tag/concision/packages-unpacker?style=for-the-badge&logo=apache-maven"/>
    </a>
    <img alt="supported java versions" src="https://img.shields.io/badge/java-%5E1.8-informational?style=for-the-badge&logo=java"/>
    <a href="https://github.com/concision/unpacker-api/graphs/contributors">
        <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/concision/unpacker-api?color=green&logo=github&style=for-the-badge"/>
    </a>
</p>

<p align="center">
    <i>A developer toolchain for extracting internal data from an unnamed game's files.</i>
</p>


## Table of Contents
- [About](#about)
- [Intentional Ambiguity](#intentional-ambiguity)
- [Motivations](#motivations)
- [Toolchain Modules](#toolchain-modules)
  - [CLI Usage](#cli-usage)
- [Compilation](#compilation)
- [Acknowledgements](#acknowledgements)


## About
This repository features a developer toolchain for extracting useful internal data from an unnamed game's files by reading proprietary file formats.
 
Accordingly, this enables:
- developing upstream applications with data that is not fully available from any other source (i.e. exclusive in nature).
- retrieving precise values used in internal game calculations or for various game mechanics.
- generating automated changelogs by tracking diffs from successive updates.


## Intentional Ambiguity
There is a certain level of intentional ambiguity present in this project's documentation to mitigate *Search Engine Optimization* (SEO). While the public release of extracted data is not necessarily harmful, reversing the algorithms to write (rather than read) the proprietary file formats may enable malicious behavior from a bad actor. Used irresponsibly, a problematic cat and mouse security game might initiate between reverse engineers and the unnamed game's development team. Therefore, several game references have been mildly obfuscated (but still quite reversible) in the codebase to prevent an actor from being able to find this project using certain keywords related to the unnamed game.

This repository is publicly available for responsible developers who are made aware of its existence.


## Motivations
The unnamed game provides a public API for obtaining game data that allows developers to develop useful tools and applications for the game's community. However, there are several distinct issues with the current support provided:
- **coverage**: only a very limited subset of useful game data is publicly available.
- **out-of-date**: provided data is may be outdated by months or, in some cases, years.
- **usability**: available data is typically in less than developer-friendly formats and may require significant parsing.
- **disinterest**: the development team have no particular interest in devoting time to build a more expansive API.

A small portion of this data can be manually obtained by analytical testing in-game; however, this model is unsustainable since it is rather time-intensive and not (easily) automated.

> Note: The data referred to here should not be confused with any association of user data - it is solely related to data used in various game mechanics.

The game itself contains a plethora of useful internal data within the 40GiB+ installation, which is obtainable by reading the game's proprietary file formats. However, there is currently no publicly available (and working) tool that is able to extract this desirable data and output the data in a standardized output format. Upon request by a few acquaintances, this project was designed and implemented, and has been maintained since.


## Toolchain Modules
The developed toolchain was designed to address a few issues from previous known (but not necessarily distributed) implementations:
- ease of portability (for most features) - the project is implemented in Java, a popular platform-independent runtime.
- reduced memory footprint - only a working subset of the data is streamed into memory.
- reduce storage requirements - the host machine is not required to have a full game installation (40GiB+) to extract data.
- standardized file format output - JSON exportation is supported (the internal data format has a near 1-to-1 mapping to JSON).
- increased code readability - there is sufficient internal documentation defining high level control flow, enabling ease of translating to other languages.

The toolchain is provided in 2 distinct modules, implemented in Java:
- **I/O api**: A Java I/O stream API for reading internal data from the game's proprietary file formats.
- **CLI**: A command-line application that automates the data extraction process for humans and upstream applications.

> Note: A third module encapsulating this functionality in a dockerized HTTP server is in development under the `development` branch.


### CLI Usage
The CLI application is sufficiently self documented with a built in `--help` command, and general usage will be omitted from here. Several various input sources are supported (e.g. local game install, cached CDN servers, game updater, etc) as well as several various output sources (files, standard out, JSON vs raw, etc).
 
Invoking the CLI application is dependent on the build distribution of choice:
- **Windows**: `unpacker.exe --help` or with Cygwin derivatives: `./unpacker.exe --help`
- **Java**: `java -jar unpacker.jar --help`

> Note: The Windows build is a helpful wrapper around Java invocation for humans, and still requires an installed compatible Java runtime.

Requirements (warning: subject to changes based on the game's development team):
- **Minimal**:
  - 256MB of RAM
  - 256MB of disk storage for file outputs
  - x86_64 CPU architecture support with Windows (or Wine for Linux & macOS) support for fetching data using the game updater.
- **Recommended**:
  - 512MB+ of RAM
  - 256MB+ of disk storage for file outputs
  - x86_64 CPU architecture support with Windows (or Wine for Linux & macOS) support for fetching data using the game updater.


## Compilation
As a further extension to the intentional ambiguity on this project, compilation instructions have been excluded to circumvent a layman from using or distributing the toolchain. Regardless, compiling should be relatively straightforward to a developer with previous experience with standardized build systems.

Effectively, it can be noted that *project compilation has been left as an exercise to the reader*.


## Acknowledgements
This project's ability to read proprietary data formats has been derived from the following two developers; their contributions have been indisputably invaluable for creating the tool.
- [@cheahjs](https://github.com/cheahjs)' public reverse engineering work
- a developer acquaintance who wishes to remain anonymous
