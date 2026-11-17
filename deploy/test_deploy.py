#!/usr/bin/env python3
"""Exercise deployment failures in a temporary VM stand-in, with no network or real Docker."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('deploy.sh')

class DeploymentTests(unittest.TestCase):
    def setup_base(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        self.home = self.root / 'home'
        self.home.mkdir()
        self.log = self.root / 'calls'
        self.env = dict(os.environ, PATH=f'{self.bin}:{os.environ["PATH"]}',
                        HOME=str(self.home), AGROTRENDS_HOME=str(self.home / 'agrotrends'),
                        AGROTRENDS_WEB_ROOT=str(self.root / 'web'), CALL_LOG=str(self.log))
        self.stub('sleep', 'exit 0')
        self.stub('sudo', 'shift; exec "$@"')

    def stub(self, name, body):
        path = self.bin / name
        path.write_text('#!/bin/bash\n' + body + '\n')
        path.chmod(0o755)

    def run_script(self, argument):
        return subprocess.run(['bash', str(self.script), argument], env=self.env,
                              capture_output=True, text=True)

    def test_rejects_invalid_release_before_mutation(self):
        result = self.run_script('../invalid')
        self.assertEqual(result.returncode, 2)
        self.assertFalse(self.log.exists())

    def setUp(self):
        self.setup_base()
        self.release = self.root / 'release'
        self.release.mkdir()
        self.script = self.release / 'deploy.sh'
        shutil.copyfile(SCRIPT, self.script)
        shutil.copyfile(SCRIPT.with_name('compose.image.yml'), self.release / 'compose.image.yml')
        envfile = self.home / 'agrotrends/backend/.env'
        envfile.parent.mkdir(parents=True)
        envfile.touch()  # Empty stand-in; never access real credentials.
        self.image = 'ghcr.io/example/backend@sha256:' + 'a' * 64
        self.stub('docker', r'''printf '%s\n' "$* APP_IMAGE=${APP_IMAGE:-}" >> "$CALL_LOG"
if [[ $1 == pull && ${PULL_FAIL:-0} == 1 ]]; then exit 1; fi
if [[ $1 == ps ]]; then echo old-container; fi
if [[ $1 == inspect && $* == *State.Running* ]]; then echo true
elif [[ $1 == inspect ]]; then echo sha256:old; fi
if [[ $1 == compose && $* == *'ps -q app'* ]]; then echo app-container; fi
if [[ $1 == compose && $* == *'up -d'* ]]; then printf '%s' "$APP_IMAGE" > "$HOME/active-image"; fi
''')
        self.stub('curl', r'''if [[ ${HEALTH_FAIL:-0} == 1 ]] && ! grep -q sha256:old "$HOME/active-image"; then echo '{"status":"DOWN"}'; else echo '{"status":"UP"}'; fi''')

    def test_healthy_deployment_records_release(self):
        result = self.run_script(self.image)
        self.assertEqual(result.returncode, 0, result.stderr)
        current = self.home / 'agrotrends/backend-deploy/current'
        self.assertEqual(current.resolve(), self.release)
        self.assertEqual((self.release / 'image').read_text().strip(), self.image)
        self.assertIn('APP_IMAGE=' + self.image, self.log.read_text())

    def test_pull_failure_leaves_current_container_untouched(self):
        self.env['PULL_FAIL'] = '1'
        result = self.run_script(self.image)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('up -d', self.log.read_text())

    def test_unhealthy_new_image_restores_previous_image(self):
        self.env['HEALTH_FAIL'] = '1'
        result = self.run_script(self.image)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Previous image restored', result.stderr)
        self.assertEqual((self.home / 'active-image').read_text(), 'sha256:old')
        self.assertFalse((self.home / 'agrotrends/backend-deploy/current').exists())

if __name__ == '__main__':
    unittest.main()
