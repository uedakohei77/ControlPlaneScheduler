#!/bin/bash

mvn -q compile exec:java -Dexec.mainClass="com.example.demo.Main" -Dexec.args="$*"
