[![License MIT][badge-license]](LICENSE.txt)

![PANDA](/panda.png)
=============================

PaNda is a Datalog compiler targeting the [LogiQL](http://www.logicblox.com/technology/) as well as the [Souffle](https://github.com/souffle-lang/souffle/) Datalog dialect. Input programs are written in a modern Datalog specification and can afterwards be compiled to valid forms for the aforementioned Datalog engines. PaNda can either be used as a tool from command line or a Java/Groovy library for creating Datalog programs from inside your app.

System requirements
-------------------

* Java Developer Kit version 8 or newer

Installation & Testing
----------------------

```bash
git clone git://github.com/gkastrinis/panda.git
cd panda
./gradlew run -Pargs=src/test/resources/t1.logic
```

Documentation
-------------

http://github.com/gkastrinis/panda/wiki

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg
