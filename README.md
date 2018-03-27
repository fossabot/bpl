#BPL Programming Language

[![Build Status](https://travis-ci.org/baeda/bpl.svg?branch=master)](https://travis-ci.org/baeda/bpl)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fbaeda%2Fbpl.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fbaeda%2Fbpl?ref=badge_shield)

A hackish attempt to bring forth a simple, yet non-restrictive programming
language.

##Warning
This project is in very early development. The current master branch runs fine
on my machine and is not supported anywhere else at the moment.

####For the curious
Check out the code snippets in the [test resources](../../tree/master/src/test/resources/compiler)!

###Why?
This is just my view on what a programming language should look & feel like and
what it should be able to do.

###Influences
The syntax is heavily influenced by [Go](https://golang.org/).

###How?
The language offers two compilation targets:
* BPLVM Bytecode
* [C99](https://en.wikipedia.org/wiki/C99)

BPLVM Bytecode can be executed with the BPL Virtual Machine, which is also part
of this project. The BPLVM enables extensive compile-time code execution for the
C99 target and Java-Style debugging (hot swap of code during runtime).

The idea is to support BPL anywhere, where the standard gcc toolchain is
available.

The project uses [ANTLR v4](http://www.antlr.org/) for lexing and parsing and
is written in vanilla Java 8 for bootstrapping.


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fbaeda%2Fbpl.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fbaeda%2Fbpl?ref=badge_large)