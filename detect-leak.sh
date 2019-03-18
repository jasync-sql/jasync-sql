#!/usr/bin/env bash

if grep LEAK: $1 | grep ERROR
then
  echo "leak found!!!"
  exit 1
else
  echo "no leak found"
fi
