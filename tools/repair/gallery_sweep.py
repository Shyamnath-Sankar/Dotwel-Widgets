#!/usr/bin/env python3
"""Real gallery UI/Add and selected launcher pins. No source mutations or owner data."""
import subprocess as sp,pathlib,time,re,json,os,sys,xml.etree.ElementTree as ET
P='com.dotwelwidgets.dotwellabs'; OUT=pathlib.Path('runtime-evidence/gallery');OUT.mkdir(parents=True,exist_ok=True)
BUDGET=float(os.environ.get('GALLERY_SWEEP_BUDGET_SECONDS','1050'))
ENTRY_RESERVE=float(os.environ.get('GALLERY_SWEEP_ENTRY_RESERVE_SECONDS','45'))
START=time.monotonic()
entries=json.loads(pathlib.Path('runtime-evidence/ready/catalog.json').read_text());results=[]
# catalog.json is grouped by category; this is the sheet index for each entry.
positions={};seen={}
for ix,e in enumerate(entries):
 positions[ix]=seen.get(e['category'],0);seen[e['category']]=positions[ix]+1

def remaining():return BUDGET-(time.monotonic()-START)
def adb(*args,check=True):
 r=sp.run(['adb',*args],capture_output=True,timeout=30)
 if check and r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
 return r.stdout
def tree():
 raw=adb('shell','am','instrument','-w','org.dotwel.snapshot/.Snapshot').decode(errors='replace');a=raw.find('<hierarchy');b=raw.find('</hierarchy>')
 if a<0 or b<0:raise RuntimeError('fresh accessibility tree missing')
 return ET.fromstring(raw[a:b+12])
def find(root,text,exact=True):
 return next((n for n in root.iter('node') if any((text.casefold()==n.get(k,'').casefold() if exact else text.casefold() in n.get(k,'').casefold()) for k in ('text','content-desc'))),None)
def click(node,top=False):
 if node is None:raise RuntimeError('UI target missing')
 b=list(map(int,re.findall(r'\d+',node.get('bounds',''))));x=(b[0]+b[2])//2
 y=(b[1]+b[3])//2 if not top else b[1]+min(48,max(8,(b[3]-b[1])//4))
 adb('shell','input','tap',str(x),str(y))
def capture(name,root=None):
 (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
 try:(OUT/(name+'.xml')).write_bytes(ET.tostring(root if root is not None else tree()))
 except Exception:pass
def wait(predicate,timeout=6.0):
 end=time.time()+max(0.0,min(timeout,remaining()));last=None
 while True:
  try:
   last=tree();value=predicate(last)
   if value is not None:return last,value
  except Exception:last=None
  if time.time()>=end:return last,None
  time.sleep(.15)
CATEGORY_RE=re.compile(r'^Open .+, \d+ ready$',re.I)
def gallery_marker(root):
 return find(root,'CATEGORIES') is not None or any(CATEGORY_RE.match((n.get('content-desc') or '').strip()) for n in root.iter('node'))
def screen():
 try:
  m=re.search(r'(\d+)x(\d+)',adb('shell','wm','size').decode(errors='replace'))
  if m:return int(m.group(1)),int(m.group(2))
 except Exception:pass
 return 1080,2340
SIZE=screen()
def swipe_content(direction):
 w,h=SIZE;x=w//2
 y1,y2=((int(h*.62),int(h*.30)) if direction=='forward' else (int(h*.30),int(h*.62)))
 adb('shell','input','swipe',str(x),str(y1),str(x),str(y2),'220')
def open_gallery():
 for _ in range(3):
  adb('shell','input','keyevent','3')
  adb('shell','am','force-stop',P);adb('shell','am','start','-W','-n',P+'/.MainActivity')
  root,_=wait(lambda r:r if gallery_marker(r) else None,timeout=8)
  if root is not None and gallery_marker(root):return root
 return None
def in_view(node):
 lo=120;hi=SIZE[1]-int(SIZE[1]*.16)
 b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
 return bool(b) and b[1]>=lo and b[1]<=hi
def bring_into_view(label,root):
 for _ in range(8):
  node=find(root,'Open '+label+',',False)
  if node is None:return None
  if in_view(node):return node
  b=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
  swipe_content('forward' if b[1]>SIZE[1]-int(SIZE[1]*.16) else 'back');time.sleep(.15)
  try:root=tree()
  except Exception:pass
 return find(root,'Open '+label+',',False)
def seek_category(label):
 root,_=wait(lambda r:r if gallery_marker(r) else None,timeout=8)
 if root is None:raise RuntimeError('Gallery did not load')
 for direction in ('forward','back'):
  for _ in range(14):
   node=find(root,'Open '+label+',',False)
   if node is not None:
    node=bring_into_view(label,root)
    if node is not None:return node
   swipe_content(direction);time.sleep(.15)
   try:root=tree()
   except Exception:pass
 capture('category-'+re.sub(r'\W+','-',label).strip('-').lower()+'-failure',root)
 raise RuntimeError('Category unavailable: '+label)
def sheet_marker(root):
 if find(root,'Close category') is None:return None
 return next((n for n in root.iter('node') if (n.get('content-desc') or '').startswith('Add ')),None)
def confirmation(root):
 # Ignore the still-dismissing gallery sheet, whose button also says "Add widget".
 if find(root,'Close category') is not None:return None
 for text in ('Add to home screen','Add automatically','Add widget','Add'):
  node=find(root,text)
  if node is not None:return node
 return None
def access_fallback(root):
 for node in root.iter('node'):
  text=(node.get('text') or '').casefold()
  if text.startswith('add ') and any(word in text for word in ('plain','live','fallback')):
   return node
 return None
def wait_pin(timeout=10.0):
 end=time.time()+max(0.0,min(timeout,remaining()));plain_clicks=0;last=None
 while True:
  try:last=tree()
  except Exception:last=None
  if last is not None:
   confirm=confirmation(last)
   if confirm is not None:return last,confirm
   save=find(last,'Save and add')
   if save is not None:
    click(save);time.sleep(.4);continue
   plain=access_fallback(last)
   if plain is not None and plain_clicks<2:
    click(plain);plain_clicks+=1;time.sleep(.4);continue
  if time.time()>=end:return last,None
  time.sleep(.15)
try:
 for ix,e in enumerate(entries):
  name=f'{ix+1:02d}-{e["id"]}';result=dict(e);results.append(result)
  if remaining()<=ENTRY_RESERVE:
   result['gallery_add']='not attempted - budget exhausted'
   for rest in entries[ix+1:]:
    skipped=dict(rest);skipped['gallery_add']='not attempted - budget exhausted';results.append(skipped)
   print(f'{name}: not attempted - budget exhausted',flush=True)
   break
  try:
   if open_gallery() is None:raise RuntimeError('Gallery did not load')
   click(seek_category(e['category']),top=True)
   root,sheet=wait(lambda r:r if sheet_marker(r) is not None else None,timeout=8)
   if sheet is None:
    click(seek_category(e['category']),top=True)
    root,sheet=wait(lambda r:r if sheet_marker(r) is not None else None,timeout=8)
   if sheet is None:raise RuntimeError('Category sheet did not open: '+e['category'])
   next_button=find(root,'Next');position=positions[ix]
   if position and next_button is not None:
    # The sheet controls are fixed; fast-forward without a tree round-trip per page.
    b=list(map(int,re.findall(r'\d+',next_button.get('bounds',''))));x=(b[0]+b[2])//2;y=(b[1]+b[3])//2
    adb('shell',';'.join([f'input tap {x} {y}']*position))
   root,button=wait(lambda r:find(r,'Add '+e['name']),timeout=6)
   if button is None:
    for page in range(30):
     try:root=tree()
     except Exception:continue
     button=find(root,'Add '+e['name'])
     if button is not None:break
     nxt=find(root,'Next')
     if nxt is None:break
     click(nxt)
    if button is None:raise RuntimeError('Selected gallery identity not reached')
   capture(name+'-gallery',root);click(button)
   root,confirm=wait_pin()
   if confirm is None:raise RuntimeError('No native pin confirmation; explanation captured')
   capture(name+'-pin-confirmation',root);result['gallery_add']='native confirmation reached'
   if e['id'] in ['kwgt_interstellar_endurance','kwgt_nothing_digital2','media_mini','wx_circle','photo_circle']:
    click(confirm)
    wait(lambda r:True if confirmation(r) is None else None,timeout=4)
    adb('shell','input','keyevent','3');time.sleep(.4);capture(name+'-launcher');result['launcher']='pinned; actual pixels and dumpsys require review'
    if e['id'] in ['kwgt_interstellar_endurance','kwgt_nothing_digital2']:
     adb('shell','am','kill',P);capture(name+'-launcher-killed-before');time.sleep(3);capture(name+'-launcher-killed-after')
   else:adb('shell','input','keyevent','4')
  except Exception as error:
   result['error']=str(error);capture(name+'-failure')
  finally:
   (OUT/'RESULT.json').write_text(json.dumps(results,indent=2))
   print(f'{name}: {result.get("gallery_add") or result.get("error")}',flush=True)
finally:
 (OUT/'RESULT.json').write_text(json.dumps(results,indent=2))
 try:(OUT/'appwidget-dump.txt').write_bytes(adb('shell','dumpsys','appwidget'))
 except Exception:pass
 print(json.dumps(results,indent=2))
sys.exit(0)
