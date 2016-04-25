#!/bin/bash

rm task/client/*.class
javac task/client/*
java -classpath . task.client.TorrentClientConsole $1
rm task/client/*.class
