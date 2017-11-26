#!/bin/bash
exec top -p $1 -b -d 1 >> $2
