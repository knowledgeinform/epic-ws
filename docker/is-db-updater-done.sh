#!/bin/bash

while ping -c 1 $1; do
  echo "Waiting on epic-db-updater to finish"
  sleep 5
done

echo "epic-db-updater finished"
