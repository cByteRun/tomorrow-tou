#!/usr/bin/env python3
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]

class LaunchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='tou-launch-')
        self.root = Path(self.temp.name)
        (self.root / 'scripts').mkdir()
        for name in ['api-port.py', 'api-env.sh', 'check-api.sh', 'docker-up.sh', 'frontend.sh']:
            shutil.copy(ROOT / 'scripts' / name, self.root / 'scripts' / name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        self.env = dict(os.environ)
        self.env.pop('API_PORT', None)
        self.env.pop('TOU_API_URL', None)
        self.env['PATH'] = str(self.bin) + os.pathsep + self.env['PATH']
        self.command('curl', "printf '%s' \"$MOCK_HEALTH\"\nexit ${MOCK_CURL_EXIT:-0}")
        self.command('docker', 'exit ${MOCK_DOCKER_EXIT:-0}')
        self.env['MOCK_HEALTH'] = json.dumps({'application': 'tomorrow-tou', 'status': 'UP'})
    def tearDown(self):
        self.temp.cleanup()
    def command(self, name, body):
        path = self.bin / name
        path.write_text('#!/bin/sh\n' + body + '\n')
        path.chmod(0o755)
    def run_script(self, name, *args):
        return subprocess.run(['sh', 'scripts/' + name, *args], cwd=self.root, env=self.env, text=True, capture_output=True, timeout=10)
    def port(self):
        return subprocess.run(['python3', 'scripts/api-port.py'], cwd=self.root, env=self.env, text=True, capture_output=True, timeout=10)
    def test_default_avoids_8080(self):
        self.assertEqual('18080', self.port().stdout.strip())
    def test_dotenv_custom_port(self):
        (self.root/'.env').write_text('API_PORT="18888" # test\n')
        self.assertEqual('18888', self.port().stdout.strip())
    def test_environment_overrides_dotenv(self):
        (self.root/'.env').write_text('API_PORT=18888\n')
        self.env['API_PORT'] = '18889'
        self.assertEqual('18889', self.port().stdout.strip())
    def test_invalid_ports_fail(self):
        for value in ['0', '65536', 'bad', '18080;echo secret']:
            with self.subTest(value=value):
                self.env['API_PORT'] = value
                self.assertNotEqual(0, self.port().returncode)
    def test_correct_api_is_accepted(self):
        self.assertEqual(0, self.run_script('check-api.sh', 'http://localhost:18080').returncode)
    def test_other_or_old_api_is_rejected_before_maven(self):
        for body in ['{"status":"UP"}', '{"application":"another","status":"UP"}', 'not json']:
            with self.subTest(body=body):
                self.env['MOCK_HEALTH'] = body
                result = self.run_script('frontend.sh')
                self.assertNotEqual(0, result.returncode)
                self.assertIn('другой или старый API', result.stderr)
    def test_api_still_starting_is_rejected(self):
        self.env['MOCK_HEALTH'] = '{"application":"tomorrow-tou","status":"STARTING"}'
        self.assertNotEqual(0, self.run_script('check-api.sh', 'http://localhost:18080').returncode)
    def test_connection_failure_stops_frontend(self):
        self.env['MOCK_CURL_EXIT'] = '7'
        result = self.run_script('frontend.sh')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('API недоступен', result.stderr)
    def test_compose_failure_is_not_reported_as_success(self):
        self.env['MOCK_DOCKER_EXIT'] = '1'
        result = self.run_script('docker-up.sh')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('порт занят', result.stderr)
    def test_success_waits_for_identified_api(self):
        result = self.run_script('docker-up.sh')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('http://localhost:18080/api', result.stdout)

if __name__ == '__main__':
    unittest.main()
