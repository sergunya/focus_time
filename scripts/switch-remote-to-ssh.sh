#!/usr/bin/env bash
set -euo pipefail

# Switch the current repository's origin from HTTPS to SSH for GitHub.
# Useful when HTTPS fails with LibreSSL SSL_ERROR_SYSCALL.

if ! command -v git >/dev/null 2>&1; then
  echo "Error: git is not installed or not in PATH" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Error: This is not a Git repository. Run this script from the project root." >&2
  exit 1
fi

current_url=$(git remote get-url origin 2>/dev/null || true)
if [[ -z "${current_url}" ]]; then
  echo "Error: No 'origin' remote found." >&2
  exit 1
fi

if [[ "${current_url}" =~ ^git@github.com:(.+)\.git$ ]]; then
  echo "Origin already uses SSH: ${current_url}"
  exit 0
fi

if [[ "${current_url}" =~ ^https://github.com/(.+)\.git$ ]]; then
  path_part="${BASH_REMATCH[1]}"
  new_url="git@github.com:${path_part}.git"
  echo "Switching origin from HTTPS to SSH:"
  echo "  Old: ${current_url}"
  echo "  New: ${new_url}"
  git remote set-url origin "${new_url}"
  echo "Done. Testing connection to GitHub over SSH..."
  if command -v ssh >/dev/null 2>&1; then
    ssh -T git@github.com || true
    echo "If you see a greeting or success message above, SSH is configured correctly."
  else
    echo "Note: ssh command not found; please ensure OpenSSH is installed to test the connection."
  fi
  exit 0
fi

echo "Origin remote does not look like a GitHub HTTPS URL: ${current_url}" >&2
echo "This script only converts https://github.com/<owner>/<repo>.git to SSH." >&2
exit 2
