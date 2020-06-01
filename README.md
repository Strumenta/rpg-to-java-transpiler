# RPG to Java transpiler

This repository contains a simple transpiler from RPG code to Java.

This code was written for the tutorial on transpilers at  [https://tomassetti.me/how-to-write-a-transpiler/]( https://tomassetti.me/how-to-write-a-transpiler/).

This project takes advantage of:

* The RPG parser from [Jariko](https://github.com/smeup/jariko). Jariko is a JVM interpreter for RPG. To learn more about Jariko you can read [this article](https://tomassetti.me/jariko-an-rpg-interpreter-in-kotlin/)
* [JavaParser](https://javaparser.org), a library to parse and process Java code
* [Kolasu](https://github.com/Strumenta/kolasu), a library to define ASTs

## Acknowledgements

Thanks to Sme.UP and Franco Lombardo for their work on Jariko, the JVM interpreter for RPG.
We have derived examples from that project.