#!/usr/bin/env python3
"""Restore upstream's generated icons from the checksum-pinned release."""
import hashlib
import io
from pathlib import Path
import urllib.request
import zipfile

URL = 'https://github.com/STollenaar/ItemSorter/releases/download/v0.6.1/ItemSorter-26.2.jar'
SHA256 = '1b1db6dc3f75575a1aacf9956fe98a510933d5c5d95e7978932862a5f579b3b1'
root = Path(__file__).resolve().parents[1] / 'src/main/resources'
with urllib.request.urlopen(URL, timeout=120) as response:
    data = response.read()
if hashlib.sha256(data).hexdigest() != SHA256:
    raise SystemExit('Upstream release checksum mismatch; no icons written')
count = 0
with zipfile.ZipFile(io.BytesIO(data)) as release:
    for name in release.namelist():
        if name.startswith('web/images/block/') and name.endswith('.png'):
            if len(Path(name).parts) != 4:
                raise SystemExit('Unexpected icon path')
            target = root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(release.read(name))
            count += 1
if count == 0:
    raise SystemExit('Release contained no icons')
print(f'Restored {count} icons from verified upstream release')
