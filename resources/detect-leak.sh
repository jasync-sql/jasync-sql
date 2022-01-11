#!/usr/bin/env bash

if grep LEAK: $1 | grep ERROR | grep -v testcontainers
then
  echo "LEAK FOUND!!!"
  grep LEAK: $1 | grep ERROR | grep -v testcontainers
  exit 1
else
  echo "no leak found"
fi
