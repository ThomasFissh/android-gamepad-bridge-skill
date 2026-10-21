#!/usr/bin/env python3
"""Build an aligned, unsigned Android gamepad bridge APK from the included sources. Author: 李逊."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent
ANDROID = '{http://schemas.android.com/apk/res/android}'


def newest(parent, pattern):
    candidates = [p for p in parent.glob(pattern)
                  if p.is_dir() and re.fullmatch(r'[0-9.]+', p.name.removeprefix('android-'))]
    return max(candidates, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.name)), default=None)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--android-jar', type=Path)
    parser.add_argument('--build-tools', type=Path)
    parser.add_argument('--output', type=Path, default=ROOT / 'build')
    args = parser.parse_args()
    java_home = args.java_home or (Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
    sdk_text = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
    sdk = Path(sdk_text) if sdk_text else None
    tools = args.build_tools or (newest(sdk / 'build-tools', '*') if sdk else None)
    platform = newest(sdk / 'platforms', 'android-*') if sdk else None
    android_jar = args.android_jar or (platform / 'android.jar' if platform else None)
    javac = str(java_home / 'bin/javac') if java_home else shutil.which('javac')
    required = [android_jar] + ([tools / name for name in ('aapt2', 'd8', 'zipalign', 'core-lambda-stubs.jar')] if tools else [])
    if not tools or not javac or not Path(javac).is_file() or not android_jar or not all(p and p.is_file() for p in required):
        parser.error('Provide JDK 17, android.jar and build-tools using flags or JAVA_HOME / ANDROID_SDK_ROOT.')
    out = args.output.resolve()
    # Dedicated output only: never replace the source tree or the accepted APK.
    if out == ROOT or ROOT.is_relative_to(out) or out.is_relative_to(ROOT / 'src') or out.is_relative_to(ROOT / 'res'):
        parser.error('Output must be a separate build directory, outside source/resource trees.')
    out.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    if java_home:
        env['JAVA_HOME'] = str(java_home.resolve())

    def run(command):
        subprocess.run([str(a) for a in command], check=True, env=env)

    manifest = ET.parse(ROOT / 'AndroidManifest.xml').getroot()
    sdk_manifest = manifest.find('uses-sdk')
    minimum = sdk_manifest.get(ANDROID + 'minSdkVersion')
    target = sdk_manifest.get(ANDROID + 'targetSdkVersion')
    # Recreate only compiler products, rather than accumulating stale classes.
    classes, dex = out / 'classes', out / 'dex'
    for directory in (classes, dex):
        if directory.is_symlink():
            parser.error('Compiler output must not be a symlink.')
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    run([javac, '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-bootclasspath', str(android_jar) + os.pathsep + str(tools / 'core-lambda-stubs.jar'),
         '-d', classes, *sorted((ROOT / 'src').rglob('*.java'))])
    resources = out / 'resources.zip'
    run([tools / 'aapt2', 'compile', '--dir', ROOT / 'res', '-o', resources])
    unsigned = out / 'unaligned.apk'
    run([tools / 'aapt2', 'link', '-I', android_jar, '--manifest', ROOT / 'AndroidManifest.xml',
         '--min-sdk-version', minimum, '--target-sdk-version', target,
         '-o', unsigned, resources])
    run([tools / 'd8', '--lib', android_jar, '--min-api', minimum, '--output', dex,
         *sorted(classes.rglob('*.class'))])
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as apk:
        for path in sorted(dex.glob('*.dex')):
            apk.write(path, path.name)
    aligned = out / 'Android-Gamepad-Bridge-unsigned.apk'
    run([tools / 'zipalign', '-f', '4', unsigned, aligned])
    run([tools / 'zipalign', '-c', '4', aligned])
    result = {'apk': aligned.name, 'bytes': aligned.stat().st_size,
              'sha256': hashlib.sha256(aligned.read_bytes()).hexdigest(),
              'package': manifest.get('package'), 'version': manifest.get(ANDROID + 'versionName'),
              'min_sdk': int(minimum), 'target_sdk': int(target), 'signed': False}
    (out / 'unsigned-manifest.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
