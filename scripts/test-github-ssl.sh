#!/usr/bin/env bash
set -euo pipefail

# Quick diagnostics for HTTPS connectivity to github.com:443
# This script does not modify system settings.

echo "[1/3] Checking basic TCP connectivity to github.com:443"
if command -v nc >/dev/null 2>&1; then
  if nc -zvw5 github.com 443; then
    echo "TCP connection OK"
  else
    echo "TCP connection FAILED. Check firewall/proxy/VPN."; exit 2
  fi
else
  echo "nc not available; skipping raw TCP check"
fi

echo
echo "[2/3] Checking HTTPS response headers with curl"
if command -v curl >/dev/null 2>&1; then
  set +e
  curl -I --connect-timeout 10 https://github.com 2>&1 | sed 's/^/  /'
  curl_rc=$?
  set -e
  if [[ $curl_rc -ne 0 ]]; then
    echo "curl returned non-zero ($curl_rc). This may indicate an SSL problem on your system."
  fi
else
  echo "curl not available; skipping"
fi

echo
echo "[3/3] Inspecting TLS certificate chain with openssl"
if command -v openssl >/dev/null 2>&1; then
  set +e
  echo | openssl s_client -connect github.com:443 -servername github.com 2>/dev/null \
    | openssl x509 -noout -issuer -subject -dates 2>/dev/null | sed 's/^/  /'
  openssl_rc=$?
  set -e
  if [[ $openssl_rc -ne 0 ]]; then
    echo "Could not retrieve certificate details; possible TLS library issue."
  fi
else
  echo "openssl not available; skipping"
fi

echo
echo "Diagnostics complete. If HTTPS still fails, consider switching to SSH:"
echo "  ./scripts/switch-remote-to-ssh.sh"
