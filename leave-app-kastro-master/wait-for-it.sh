## (This stays inside Jenkinsfile's 'Prepare Docker Assets' stage)
wget https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh -O wait-for-it.sh

# Ensure correct shebang for Alpine bash
sed -i '1s|^.*$|#!/usr/bin/env bash|' wait-for-it.sh

# Remove Windows CRLF (both ways)
dos2unix wait-for-it.sh || sed -i 's/\r$//' wait-for-it.sh

chmod +x wait-for-it.sh
