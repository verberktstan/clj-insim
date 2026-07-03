#! /bin/bash

set -euo pipefail

java -jar [[::JAR-TARGET::]] "$@"
