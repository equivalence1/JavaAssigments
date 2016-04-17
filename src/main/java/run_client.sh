#!/bin/bash

rm task/client/*.class
javac task/client/*
java -classpath . task.server.TorrentClientConsole
rm task/client/*.class
