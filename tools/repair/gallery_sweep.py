#!/usr/bin/env python3
"""Real gallery UI/Add and selected launcher pins. No source mutations or owner data."""
import subprocess as sp,pathlib,time,re,json,xml.etree.ElementTree as ET
P='com.dotwelwidgets.dotwellabs'; OUT=pathlib.Path('runtime-evidence/gallery');OUT.mkdir(parents=True,exist_ok=True)
entries=json.loads(pathlib.Path('runtime-evidence/ready/catalog.json').read_text());results=[]
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
def click(node):
 if node is None:raise RuntimeError('UI target missing')
 bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))));adb('shell','input','tap',str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2));time.sleep(.15)
def capture(name):
 (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
 try:(OUT/(name+'.xml')).write_bytes(ET.tostring(tree()))
 except Exception:pass
def seek_category(label):
 for attempt in range(14):
  root=tree();n=find(root,'Open '+label+',',False)
  if n is not None:return n
  adb('shell','input','swipe','550','1600','550','650','180')
 raise RuntimeError('Category unavailable: '+label)
for ix,e in enumerate(entries):
 name=f'{ix+1:02d}-{e["id"]}';result=dict(e);results.append(result)
 try:
  adb('shell','am','force-stop',P);adb('shell','am','start','-W','-n',P+'/.MainActivity');time.sleep(.25)
  click(seek_category(e['category']))
  for page in range(30):
   root=tree();button=find(root,'Add '+e['name'])
   if button is not None:break
   click(find(root,'Next'))
  else:raise RuntimeError('Selected gallery identity not reached')
  capture(name+'-gallery');click(button);root=tree()
  if e['action']=='WORLD_ZONES':click(find(root,'Save and add'));root=tree()
  # Clocks without exact access retain their real plain-clock explanation.
  if not any(find(root,s) is not None for s in ['Add to home screen','Add automatically','Add widget','Add']):
   plain=next((n for n in root.iter('node') if 'plain' in n.get('text','').lower() and 'add' in n.get('text','').lower()),None)
   if plain is not None:click(plain);root=tree()
  confirmation=next((find(root,s) for s in ['Add automatically','Add widget','Add'] if find(root,s) is not None),None)
  if confirmation is None:raise RuntimeError('No native pin confirmation; explanation captured')
  capture(name+'-pin-confirmation');result['gallery_add']='native confirmation reached'
  if e['id'] in ['kwgt_interstellar_endurance','kwgt_nothing_digital2','media_mini','wx_circle','photo_circle']:
   click(confirmation);adb('shell','input','keyevent','3');time.sleep(.5);capture(name+'-launcher');result['launcher']='pinned; actual pixels and dumpsys require review'
   if e['id'] in ['kwgt_interstellar_endurance','kwgt_nothing_digital2']:
    adb('shell','am','kill',P);capture(name+'-launcher-killed-before');time.sleep(3 if e['id']=='kwgt_interstellar_endurance' else 66);capture(name+'-launcher-killed-after')
  else:adb('shell','input','keyevent','4')
 except Exception as error:
  result['error']=str(error);capture(name+'-failure')
 finally:(OUT/'RESULT.json').write_text(json.dumps(results,indent=2))
(OUT/'appwidget-dump.txt').write_bytes(adb('shell','dumpsys','appwidget'))
print(json.dumps(results,indent=2))
