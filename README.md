[![License MIT][badge-license]](LICENSE.txt)

![DEEPDOOP](/deepdoop-logo.png)
=============================

DeepDoop is a Datalog compiler targeting the [LogiQL](http://www.logicblox.com/technology/) as well as the [Souffle](https://github.com/souffle-lang/souffle/) Datalog dialect. Input programs are written in a modern Datalog specification and can afterwards be compiled to valid forms for the aforementioned Datalog engines. DeepDoop can either be used as a tool from command line or a Java/Groovy library for creating Datalog programs from inside your app.

System requirements
-------------------

* Java Developer Kit version 8 or newer

Installation & Testing
----------------------

```bash
git clone git://github.com/gkastrinis/deepdoop.git
cd deepdoop
./gradlew run -Pargs=src/test/resources/t1.logic
```

Documentation
-------------

http://github.com/gkastrinis/deepdoop/wiki

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg
