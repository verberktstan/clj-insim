#! /bin/bash

set -euo pipefail

java -jar [[::JAR-TARGET::]] 127.0.0.1 29999 "$@"
