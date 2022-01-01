#!/usr/bin/env sh

AC_STATUS="$(cat /sys/class/power_supply/AC/online)"

if [ 1 = "${AC_STATUS}" ]; then
  ansible-playbook ZWS.yml
else
  echo "Evitando rodar script pela falta de energia"
fi
