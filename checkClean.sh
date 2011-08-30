#!/bin/bash
# This tool checks if there are any uncommitted changes in the current git branch.
# If it finds any uncommited changes it exits with an error.
# We use it to prevent pushing dirty changes to production.

clean=$(git status | grep "nothing to commit (working directory clean)")
if [ -z "$clean" ]; then
    echo There are uncommitted changes.
    exit 1
else
    echo Branch is clean.
fi
