#!/bin/bash
exec top -pid $1 -l 0 -s 1 >> $2
