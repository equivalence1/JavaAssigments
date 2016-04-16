#!/bin/bash

rm task/*.class
javac task/*
java -classpath . task.SimpleFTPServer 8080
