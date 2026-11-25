from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
root=Path(__file__).resolve().parent
im=ImageOps.fit(Image.open(root/'composed.png').convert('RGB'),(1024,500),method=Image.Resampling.LANCZOS)
im.save(root.parent/'out/feature-graphic-five-phones-1024x500.png')
review=Image.new('RGB',(1088,850),'#102c24')
review.paste(im,(32,32))
review.paste(im.resize((512,250),Image.Resampling.LANCZOS),(32,566))
review.paste(im.resize((320,156),Image.Resampling.LANCZOS),(600,566))
review.save(root.parent/'previews/feature-five-phones-review.png')
