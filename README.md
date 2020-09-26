[![License MIT][badge-license]](LICENSE.txt)
![Travis CI w/ Logo](https://img.shields.io/travis/gkastrinis/panda/master.svg?logo=travis)
![CI](https://github.com/gkastrinis/panda/workflows/CI/badge.svg)
[![GitHub Release](https://img.shields.io/github/release/gkastrinis/panda.svg)](https://github.com/gkastrinis/panda/releases)
[![Discord Chat](https://img.shields.io/discord/758049851660304387)](https://discord.gg/KQDS83t)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/paNda-datalog/Lobby)
[![Open Source](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://opensource.org/)

![PANDA](/panda.png)
=============================

PaNda is a Datalog compiler targeting ~~the [LogiQL](http://www.logicblox.com/technology/) as well as~~
the [Souffle](https://github.com/souffle-lang/souffle/) Datalog dialect.
Input programs are written in a modern Datalog specification and can afterwards be compiled to valid forms
for the aforementioned Datalog engines.
PaNda can either be used as a tool from command line
or a Java/Groovy library for creating Datalog programs integrated to your app.

System requirements
-------------------

* Java Developer Kit version 8 or newer

Installation & Testing
----------------------

```bash
git clone git://github.com/gkastrinis/panda.git
cd panda
./gradlew run -Pargs=src/test/resources/t0-basics.pnd
```

Documentation
-------------

http://github.com/gkastrinis/panda/wiki

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg
