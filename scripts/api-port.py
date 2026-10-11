#!/usr/bin/env python3
"""Resolve the shared host API port, with environment taking precedence over .env."""
import os
from pathlib import Path
import shlex
import sys

value = os.environ.get('API_PORT')
if value is None:
    env_file = Path(__file__).resolve().parents[1] / '.env'
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            key, separator, raw = line.strip().removeprefix('export ').partition('=')
            if separator and key.strip() == 'API_PORT':
                tokens = shlex.split(raw, comments=True)
                value = tokens[0] if len(tokens) == 1 else ''
value = '18080' if value is None else value
if not value.isascii() or not value.isdigit() or not 1 <= int(value) <= 65535:
    sys.exit('API_PORT должен быть числом от 1 до 65535.')
print(int(value))
