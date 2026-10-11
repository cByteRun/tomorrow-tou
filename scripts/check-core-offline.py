#!/usr/bin/env python3
"""Partial diagnostic only: explicitly excludes POI reports and PostgreSQL integration tests."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
target = Path(tempfile.mkdtemp(prefix='tou-core-check-'))
shutil.copy(root/'pom.xml', target/'pom.xml')
(target/'frontend').mkdir()
shutil.copy(root/'frontend/pom.xml', target/'frontend/pom.xml')
shutil.copytree(root/'backend/src', target/'backend/src')
for rel in ['main/java/edu/tou/reports/ReportController.java', 'main/java/edu/tou/reports/ReportService.java', 'test/java/edu/tou/reports/ReportServiceTest.java', 'test/java/edu/tou/shared/PostgresApiIT.java']:
    (target/'backend/src'/rel).unlink()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
ET.register_namespace('', ns['m'])
pom = ET.parse(root/'backend/pom.xml')
deps = pom.getroot().find('m:dependencies', ns)
for dep in list(deps):
    if dep.find('m:groupId', ns).text in ['org.apache.poi', 'org.testcontainers']:
        deps.remove(dep)
pom.write(target/'backend/pom.xml', encoding='UTF-8', xml_declaration=True)
print('PARTIAL CHECK: excludes reports/POI and PostgreSQL integration tests.', flush=True)
print('Temporary build:', target, flush=True)
raise SystemExit(subprocess.call([str(root/'mvnw'), '-o', '-f', str(target/'pom.xml'), '-pl', 'backend', 'test']))
