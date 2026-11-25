"""Keep original captures intact; add requested matching top inset, then composite."""
import json, os, subprocess, sys
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parent
for slide in json.loads((root/'slides.json').read_text()):
    src=root/slide['screen']
    if not src.exists():
        print('PENDING CAPTURE:',slide['id'],flush=True)
        continue
    im=Image.open(src).convert('RGB')
    inset=40 # 13.3 dp at the Samsung capture's 3x density.
    bg=im.getpixel((im.width//2,0))
    padded=Image.new('RGB',(im.width,im.height+inset),bg)
    padded.paste(im,(0,inset))
    screen=root/'screens'/f"{slide['id']}.png"
    padded.save(screen)
    target=root/'assets'/f"{slide['id']}.png"
    subprocess.run([sys.executable,str(root/'composite_screen.py'),str(target),str(root/'assets'/slide['plate']),str(screen)],env={**os.environ,'NO_HOLE':'1'},check=True)
