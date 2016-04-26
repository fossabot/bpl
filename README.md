#BPL Programming Language
A hackish attempt to bring forth a simple, yet non-restrictive programming
language.

##Warning
This project is in very early development. The current master branch runs fine
on my machine and is not supported anywhere else at the moment.

###Why?
This is just my view on what a programming language should look & feel like and
what it should be able to do.

###Influences
The syntax is heavily influenced by [Go](https://golang.org/).

###How?
The language offers two compilation targets:
* BPLVM Bytecode
* [C89](https://en.wikipedia.org/wiki/ANSI_C)

BPLVM Bytecode can be executed with the BPL Virtual Machine, which is also part
of this project. The BPLVM enables extensive compile-time code execution for the
C89 target and Java-Style debugging (hot swap of code during runtime).

The idea is to support BPL anywhere, where the standard gcc toolchain is
available.

The project uses [ANTLR v4](http://www.antlr.org/) for lexing and parsing and
is written in vanilla Java 8 for bootstrapping.
