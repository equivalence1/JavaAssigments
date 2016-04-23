#!/bin/bash

rm task/server/*.class
javac task/server/*
java -classpath . task.server.TorrentTrackerConsole
rm task/server/*.class
