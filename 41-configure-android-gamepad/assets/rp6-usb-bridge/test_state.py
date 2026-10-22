#!/usr/bin/env python3
"""Run the display-state tests with a caller-provided JDK; no device or network access."""
import argparse, os, shutil, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--java-home',type=Path);a=p.parse_args()
jdk=a.java_home or (Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
javac=str(jdk/'bin/javac') if jdk else shutil.which('javac');java=str(jdk/'bin/java') if jdk else shutil.which('java')
if not javac or not java:p.error('Provide JDK using --java-home or JAVA_HOME')
with tempfile.TemporaryDirectory(prefix='gamepad-state-') as output:
 subprocess.run([javac,'-encoding','UTF-8','-d',output,str(ROOT/'src/io/github/thomasfissh/androidgamepadbridge/PadState.java'),str(ROOT/'tests/PadStateTest.java')],check=True)
 subprocess.run([java,'-cp',output,'io.github.thomasfissh.androidgamepadbridge.PadStateTest'],check=True)
