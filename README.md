[![License MIT][badge-license]](LICENSE.txt)

![DEEPDOOP](/deepdoop-logo.png)
=============================

DeepDoop is a Datalog compiler targeting the [LogiQL](http://www.logicblox.com/technology/) as well as the [Souffle](https://github.com/souffle-lang/souffle/) Datalog dialect. Input programs are written in a modern Datalog specification and can afterwards be compiled to valid forms for the aforementioned Datalog engines.

System requirements
-------------------

* Java Developer Kit version 8 or newer. Available from [Oracle's Java website](http://www.oracle.com/java)

Installation & Testing
----------------------

      $ git clone git://github.com/gkastrinis/deepdoop.git
      $ cd deepdoop
      $ ./gradlew run -Pargs=myfile.logic

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg
