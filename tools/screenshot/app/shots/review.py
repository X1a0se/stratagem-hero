
from PIL import Image, ImageDraw
import os
base = os.getcwd()
names = [
 ("41-short-effect-setup.png",),
 ("23-landscape-greatwall.png",),
 ("44-short-greatwall.png",),
 ("23-landscape-multilines.png",),
 ("22-landscape-classic.png",),
 ("15-over-greatwall.png",),
 ("31-landscape-over.png",),
 ("54-tall-over.png",),
]
thumbs=[]
for (n,) in names:
    p=os.path.join(base,n)
    im=Image.open(p).convert("RGB")
    w,h=im.size
    tw=760
    th=int(h*tw/w)
    im=im.resize((tw,th), Image.LANCZOS)
    canvas=Image.new("RGB",(tw,th+28),(20,20,20))
    canvas.paste(im,(0,28))
    d=ImageDraw.Draw(canvas)
    d.text((8,8),n,fill=(255,210,74))
    thumbs.append(canvas)
cols=2
rows=(len(thumbs)+cols-1)//cols
cellh=max(t.height for t in thumbs)
out=Image.new("RGB",(cols*760,rows*cellh),(8,8,8))
for i,t in enumerate(thumbs):
    out.paste(t,((i%cols)*760,(i//cols)*cellh))
out.save(os.path.join(base,"review-contact.png"))
print(out.size)
