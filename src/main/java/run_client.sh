#!/bin/bash

rm task/client/*.class
javac task/client/*
java -classpath . task.client.TorrentClient
rm task/client/*.class
