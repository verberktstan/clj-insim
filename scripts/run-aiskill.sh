#! /bin/bash

set -euo pipefail

java -Dclj-insim.strict-validation=false -jar [[::JAR-TARGET::]] 127.0.0.1 29999 "$@"
