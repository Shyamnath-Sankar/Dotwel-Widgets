#!/usr/bin/env python3
"""Offline compiler for the MIT AumGupta Interstellar Endurance presets.

Reads the original Kustom preset JSON, resolves Kustom's per-property toggle
semantics (0=raw value, 10=formula, 100=global) and emits:
  * app/src/main/assets/kwgt/interstellar_*.sanitized.json
  * app/src/main/res/drawable/interstellar_*_{black,translucent,transparent}.xml
  * docs/interstellar-*-{black,translucent,transparent}.png

The compiler only emits original/source-author geometric shapes. It never
samples an upstream bitmap. Shapes using CLEAR/MULTIPLY blend modes are counted
and reported; CLEAR subtracts alpha while MULTIPLY remains approximate.
"""
from __future__ import annotations
import json, math, re, os, sys
from dataclasses import dataclass, field
from typing import Any

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.environ.get("INTERSTELLAR_SRC", "/tmp/remaining-source")
ASSETS = os.path.join(ROOT, "app/src/main/assets/kwgt")
DRAWABLE = os.path.join(ROOT, "app/src/main/res/drawable")
DRAWABLE_XXX = os.path.join(ROOT, "app/src/main/res/drawable-xxxhdpi")
DOCS = os.path.join(ROOT, "docs")
SIDE = 277.0
VARIANTS = {
    "endurance": ("Interstellar_Endurance.json", "interstellar_endurance"),
    "analog": ("Interstellar_Endurance_Analog.json", "interstellar_endurance_analog"),
    "analog2": ("Interstellar_Endurance_Analog_2.json", "interstellar_endurance_analog_2"),
}
STYLES = ("black", "translucent", "transparent")

@dataclass
class Prim:
    kind: str                 # rect|oval|tri|rtri|line
    x: float; y: float; w: float; h: float
    r: float = 0.0
    fill: str | None = None
    stroke: str | None = None
    stroke_w: float = 0.0
    matrix: tuple = ((1,0,0),(0,1,0))
    tag: str = ""
    children: list = field(default_factory=list)

@dataclass
class Report:
    unsupported: list[str] = field(default_factory=list)
    counts: dict[str,int] = field(default_factory=dict)
    def add(self, k: str, n: int = 1):
        self.counts[k] = self.counts.get(k, 0) + n

def mat_mul(a,b):
    return tuple(tuple(sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)) for i in range(3))
def translate(tx,ty): return ((1,0,tx),(0,1,ty),(0,0,1))
def rotate(deg,cx,cy):
    r=math.radians(deg); c=math.cos(r); s=math.sin(r)
    return ((c,-s,cx-c*cx+s*cy),(s,c,cy-s*cx-c*cy),(0,0,1))
def apply(m,x,y):
    return (m[0][0]*x+m[0][1]*y+m[0][2], m[1][0]*x+m[1][1]*y+m[1][2])

def repair_color(v):
    if not isinstance(v,str): return None
    v=v.strip()
    if v.startswith("##"): v=v[1:]
    if re.fullmatch(r"#[0-9A-Fa-f]{6}",v): return "#FF"+v[1:].upper()
    if re.fullmatch(r"#[0-9A-Fa-f]{8}",v): return v.upper()
    return None

def eval_expr(expr, side=SIDE):
    if expr is None: return None
    if isinstance(expr,(int,float)): return float(expr)
    raw=str(expr).strip()
    if not raw: return None
    neg=raw.startswith("-")
    if neg: raw=raw[1:].strip()
    body=raw.strip("$").strip()
    m=re.fullmatch(r"gv\(side\)/\(\s*277\s*/\s*([0-9.]+)\s*\)",body)
    if m:
        d=float(m.group(1)); val=side*d/277.0
    else:
        m=re.fullmatch(r"gv\(side\)/\s*([0-9.]+)",body)
        if m:
            d=float(m.group(1)); val=side/d if d else None
        else:
            m=re.fullmatch(r"gv\(side\)\*\s*([0-9.]+)",body)
            if m: val=side*float(m.group(1))
            else:
                try: val=float(body)
                except ValueError: return None
    if val is None: return None
    return -val if neg else val

def resolve(node, prop, root):
    toggles=node.get("internal_toggles") or {}
    formulas=node.get("internal_formulas") or {}
    globals_=node.get("internal_globals") or {}
    t=toggles.get(prop,0)
    if t==10 and prop in formulas:
        v=formulas.get(prop)
        # clock mode formulas are symbolic, not numeric
        if isinstance(v,str) and v.strip().upper() in CLOCK_MODES: return v.strip().upper()
        return eval_expr(v)
    if t==100 and prop in globals_:
        gname=globals_.get(prop)
        if prop in ("paint_color","color_fgcolor","color_bgcolor"):
            return gname
        g=(root.get("globals_list") or {}).get(gname)
        if g:
            if g.get("toggles")==10 and g.get("global_formula"):
                fv=eval_global_formula(g.get("global_formula"))
                if fv is not None: return fv
            if "value" in g: return g.get("value")
        return gname
    raw=node.get(prop)
    if isinstance(raw,(int,float)): return float(raw)
    if isinstance(raw,str) and raw.strip().upper() in CLOCK_MODES: return raw.strip().upper()
    return raw

CLOCK_MODES={"CLOCK_SECOND","CLOCK_MINUTE","CLOCK_HOUR","CLOCK_HOUR_SMOOTH","DEG90","DEG180","DEG270","FLIP_X","FLIP_Y","MANUAL","NONE"}

def eval_global_formula(f):
    if not isinstance(f,str): return None
    # source colour formulas are dark/light choices; use the dark-mode branch value
    cols=re.findall(r"#[0-9A-Fa-f]{6,8}",f.replace("##","#"))
    if cols: return repair_color(cols[-1])
    return None

PALETTE={
 "black":{"bg":"#FF1B1B1D","fg":"#FFE1E1E1","fgl":"#FFC6C6C6","minute":"#FFFFFFFF","hour":"#FFD71921","seconds":"#FFD71921"},
 "translucent":{"bg":"#A6FFFFFF","fg":"#FF1B1B1D","fgl":"#FF363636","minute":"#FF1B1B1D","hour":"#FFD71921","seconds":"#FFD71921"},
 "transparent":{"bg":"#00000000","fg":"#FFE1E1E1","fgl":"#FFC6C6C6","minute":"#FFFFFFFF","hour":"#FFD71921","seconds":"#FFD71921"},
}

def color_value(node, prop, root, style, role=None):
    v=resolve(node,prop,root)
    if isinstance(v,str):
        c=repair_color(v)
        if c: return c
        if v in ("bg","fg","fgl","minute","hour","seconds"):
            return PALETTE[style][v]
        if v in (root.get("globals_list") or {}):
            g=root["globals_list"][v]
            c=repair_color(g.get("value")) or eval_global_formula(g.get("global_formula"))
            if c: return c
    if role in ("bg","fg","fgl","minute","hour","seconds"):
        return PALETTE[style][role]
    return PALETTE[style]["fg"]

def apply_style_color(c, role, style):
    if role=="bg": return {"black":"#FF1B1B1D","translucent":"#A6FFFFFF","transparent":"#00000000"}[style]
    return c

def rotated_bbox(w,h,mode,offset=None):
    m=(mode or "").upper()
    if m in ("DEG90","DEG270"): return (h,w)
    if m in ("DEG180","FLIP_X","FLIP_Y","NONE",""): return (w,h)
    ang=0.0
    if m=="MANUAL": ang=float(offset or 0)
    if ang:
        r=math.radians(ang); c=abs(math.cos(r)); sn=abs(math.sin(r))
        return (w*c+h*sn, w*sn+h*c)
    return (w,h)

def pad_add(node,root,w,h):
    l=resolve(node,"position_padding_left",root) or 0
    t=resolve(node,"position_padding_top",root) or 0
    r=resolve(node,"position_padding_right",root) or 0
    b=resolve(node,"position_padding_bottom",root) or 0
    return (w+max(0,l)+max(0,r), h+max(0,t)+max(0,b))

def measure(node, parent_w, parent_h, root):
    t=node.get("internal_type")
    if t=="ShapeModule":
        w=resolve(node,"shape_width",root); h=resolve(node,"shape_height",root)
        if w is None and h is not None: w=h
        if h is None and w is not None: h=w
        w=w or parent_w; h=h or parent_h
        w,h=rotated_bbox(w,h,resolve(node,"shape_rotate_mode",root) or node.get("shape_rotate_mode"),eval_expr(node.get("shape_rotate_offset")))
        return pad_add(node,root,w,h)
    if t=="ProgressModule":
        s=resolve(node,"style_size",root) or min(parent_w,parent_h)
        return pad_add(node,root,s,s)
    if t=="OverlapLayerModule":
        visible=[]
        skip_next=False
        for child in node.get("viewgroup_items") or []:
            if skip_next:
                skip_next=False
                continue
            visible.append(child)
            if child.get("fx_mask")=="CLIP_ALL": break
            if child.get("fx_mask")=="CLIP_NEXT": skip_next=True
        vals=[measure(c,parent_w,parent_h,root) for c in visible]
        w=max([v[0] for v in vals],default=0)
        h=max([v[1] for v in vals],default=0)
        return pad_add(node,root,w,h)
    if t=="StackLayerModule":
        vals=[measure(c,parent_w,parent_h,root) for c in node.get("viewgroup_items") or []]
        margin=resolve(node,"config_margin",root) or 0
        vertical=(node.get("config_stacking") or "VERTICAL_CENTER")=="VERTICAL_CENTER"
        if vertical:
            w=max([v[0] for v in vals],default=0)
            h=sum(v[1] for v in vals)+margin*max(0,len(vals)-1)
        else:
            w=sum(v[0] for v in vals)+margin*max(0,len(vals)-1)
            h=max([v[1] for v in vals],default=0)
        return pad_add(node,root,w,h)
    return (parent_w,parent_h)

def anchor_pos(anchor, parent, w, h):
    x,y,_,_=parent
    pw,ph=parent[2],parent[3]
    a=(anchor or "CENTER").upper()
    left={"TOPLEFT":x,"BOTTOMLEFT":x,"CENTERLEFT":x,"TOP":x+(pw-w)/2,"BOTTOM":x+(pw-w)/2,"CENTER":x+(pw-w)/2}.get(a,x+(pw-w)/2)
    top={"TOPLEFT":y,"TOPRIGHT":y,"TOP":y,"CENTERLEFT":y+(ph-h)/2,"CENTERRIGHT":y+(ph-h)/2,"CENTER":y+(ph-h)/2,"BOTTOM":y+ph-h,"BOTTOMLEFT":y+ph-h,"BOTTOMRIGHT":y+ph-h}.get(a,y+(ph-h)/2)
    if a=="TOPRIGHT": left=x+pw-w
    if a=="BOTTOMRIGHT": left=x+pw-w
    if a=="CENTERRIGHT": left=x+pw-w
    return left,top

def inset(parent, node, root):
    x,y,w,h=parent
    l=resolve(node,"position_padding_left",root) or 0
    t=resolve(node,"position_padding_top",root) or 0
    r=resolve(node,"position_padding_right",root) or 0
    b=resolve(node,"position_padding_bottom",root) or 0
    return (x+l, y+t, max(0.01,w-l-r), max(0.01,h-t-b))

def rotation_matrix(mode, offset, bounds, include_clock=True, time=(10,10,30)):
    x,y,w,h=bounds; cx=x+w/2; cy=y+h/2
    mode=(mode or "").upper()
    if mode in ("CLOCK_SECOND","CLOCK_MINUTE","CLOCK_HOUR","CLOCK_HOUR_SMOOTH"):
        if not include_clock: return None
        hh,mm,ss=time
        deg={"CLOCK_SECOND":ss*6,"CLOCK_MINUTE":mm*6,"CLOCK_HOUR":hh*30,"CLOCK_HOUR_SMOOTH":(hh+mm/60)*30}[mode]
        deg += (offset or 0)
        return rotate(deg,cx,cy)
    if mode=="DEG90": return rotate(90,cx,cy)
    if mode=="DEG180": return rotate(180,cx,cy)
    if mode=="DEG270": return rotate(270,cx,cy)
    if mode=="FLIP_X": return ((1,0,0),(0,-1,2*cy),(0,0,1))
    if mode=="FLIP_Y": return ((-1,0,2*cx),(0,1,0),(0,0,1))
    if mode=="MANUAL": return rotate(offset or 0,cx,cy)
    return None

def flatten(node, bounds, root, style, report, base=None, include_clock=True, time=(10,10,30), role=None, nested=True):
    base=base or ((1,0,0),(0,1,0),(0,0,1))
    t=node.get("internal_type")
    if resolve(node,"config_visible",root) in ("NEVER","REMOVE"):
        report.add("hidden_ancestor"); return
    if t=="ShapeModule":
        w=resolve(node,"shape_width",root); h=resolve(node,"shape_height",root)
        if w is None and h is not None: w=h
        if h is None and w is not None: h=w
        w=w or bounds[2]; h=h or bounds[3]
        content=inset(bounds,node,root)
        mode=resolve(node,"shape_rotate_mode",root) or node.get("shape_rotate_mode")
        offset=resolve(node,"shape_rotate_offset",root)
        rw,rh=rotated_bbox(w,h,mode,offset)
        bx,by=anchor_pos(node.get("position_anchor"),content,rw,rh)
        ax,ay=bx+(rw-w)/2,by+(rh-h)/2
        m=base
        rm=rotation_matrix(resolve(node,"shape_rotate_mode",root) or node.get("shape_rotate_mode"), offset, (ax,ay,w,h), include_clock, time)
        if rm: m=mat_mul(base,rm)
        fillrole=resolve(node,"paint_color",root)
        fill=color_value(node,"paint_color",root,style,fillrole)
        stylepaint=(node.get("paint_style") or "FILL").upper()
        mode=(node.get("paint_mode") or "NORMAL").upper()
        stroke_w=resolve(node,"paint_stroke",root) or 0
        if mode=="CLEAR":
            kind={"CIRCLE":"oval","OVAL":"oval","TRIANGLE":"tri","RTRIANGLE":"rtri"}.get((node.get("shape_type") or "RECT").upper(),"rect")
            REPORT_PRIMS.append(Prim(kind,ax,ay,w,h,resolve(node,"shape_corners",root) or 0,
                                     fill=None,
                                     stroke="stroke" if stylepaint=="STROKE" else None,
                                     stroke_w=(stroke_w if stroke_w and stroke_w>0 else 1.0),
                                     matrix=m,tag="clear"))
            report.add("clear_shape"); return
        if mode=="MULTIPLY":
            report.add("multiply_shape")
            report.unsupported.append("MULTIPLY approximated as source color; needs pixel validation")
        kind={"CIRCLE":"oval","OVAL":"oval","TRIANGLE":"tri","RTRIANGLE":"rtri"}.get((node.get("shape_type") or "RECT").upper(),"rect")
        report.add("shape")
        report.add("shape_"+kind)
        report.add("color_"+str(fillrole))
        p=Prim(kind,ax,ay,w,h,resolve(node,"shape_corners",root) or 0,
               fill=None if stylepaint=="STROKE" else fill,
               stroke=fill if stylepaint=="STROKE" else None, stroke_w=stroke_w, matrix=m)
        mask=node.get("fx_mask")
        if mask in ("CLIP_ALL","CLIP_NEXT"):
            p.tag=mask.lower(); report.add(mask.lower())
        elif mask not in (None,"NONE"):
            report.unsupported.append("fx_mask "+str(mask))
        REPORT_PRIMS.append(p)
        return
    if t=="ProgressModule":
        count=int(resolve(node,"progress_count",root) or node.get("progress_count") or 1)
        count=max(1,min(120,count))
        size=resolve(node,"style_size",root) or min(bounds[2],bounds[3])
        tw=resolve(node,"style_width",root) or size*0.12
        th=resolve(node,"style_height",root) or size*0.04
        off=eval_expr(node.get("progress_rotate_offset"))
        fg=color_value(node,"color_fgcolor",root,style,resolve(node,"color_fgcolor",root))
        bg=color_value(node,"color_bgcolor",root,style,resolve(node,"color_bgcolor",root))
        cx=bounds[0]+bounds[2]/2; cy=bounds[1]+bounds[3]/2; rad=size/2
        shape_code=(node.get("style_shape") or "CIRCLE").upper()
        color_mode=(node.get("color_mode") or node.get("paint_mode") or "").upper()
        clear=(color_mode=="CLEAR")
        for i in range(count):
            ang=(off or 0)+i*(360.0/count)
            m=mat_mul(base,rotate(ang,cx,cy))
            if shape_code in ("CIRCLE","OVAL"):
                kind="oval"; tw=resolve(node,"style_width",root) or size*0.12; th=tw
            else:
                kind="rect"
            if clear:
                REPORT_PRIMS.append(Prim(kind,cx-tw/2,cy-rad-th/2,tw,th,min(tw,th)/2,
                                         fill=None,stroke="stroke",stroke_w=1.0,matrix=m,tag="clear"))
            else:
                REPORT_PRIMS.append(Prim(kind,cx-tw/2,cy-rad-th/2,tw,th,min(tw,th)/2,fill=fg,matrix=m))
            report.add("progress_tick")
        report.add("progress")
        return
    if t=="OverlapLayerModule":
        off=resolve(node,"config_rotate_offset",root)
        rm=rotation_matrix(resolve(node,"config_rotate_mode",root) or node.get("config_rotate_mode"),
                           off if isinstance(off,(int,float)) else None,
                           bounds, include_clock, time)
        m=mat_mul(base,rm) if rm else base
        mw,mh=measure(node,bounds[2],bounds[3],root)
        left=bounds[0]+(bounds[2]-mw)/2 + (0 if nested else (resolve(node,"position_offset_x",root) or 0))
        top=bounds[1]+(bounds[3]-mh)/2 + (0 if nested else (resolve(node,"position_offset_y",root) or 0))
        local=(left,top,mw,mh)
        content=inset(local,node,root)
        saved=REPORT_PRIMS[:]
        del REPORT_PRIMS[:]
        for c in node.get("viewgroup_items") or []:
            flatten(c,content,root,style,report,m,include_clock,time,role)
        group_prims=REPORT_PRIMS[:]
        del REPORT_PRIMS[:]
        REPORT_PRIMS.extend(saved)
        REPORT_PRIMS.append(Prim("group",0,0,SIDE,SIDE,children=group_prims))
        report.add("overlap")
        return
    if t=="StackLayerModule":
        children=node.get("viewgroup_items") or []
        sizes=[measure(c,bounds[2],bounds[3],root) for c in children]
        margin=resolve(node,"config_margin",root) or 0
        vertical=(node.get("config_stacking") or "VERTICAL_CENTER")=="VERTICAL_CENTER"
        total_w=max([s[0] for s in sizes],default=0) if vertical else sum(s[0] for s in sizes)+margin*max(0,len(sizes)-1)
        total_h=sum(s[1] for s in sizes)+margin*max(0,len(sizes)-1) if vertical else max([s[1] for s in sizes],default=0)
        content=inset(bounds,node,root)
        left,top=anchor_pos(node.get("position_anchor"),content,total_w,total_h)
        left += 0 if nested else (resolve(node,"position_offset_x",root) or 0)
        top += 0 if nested else (resolve(node,"position_offset_y",root) or 0)
        cursor=top if vertical else left
        for i,c in enumerate(children):
            cw,ch=sizes[i]
            if vertical:
                cl=left+((total_w-cw)/2 if (node.get("config_stacking") or "VERTICAL_CENTER")=="VERTICAL_CENTER" else 0)
                cb=(cl,cursor,cw,ch)
                cursor+=ch+margin
            else:
                ct=top
                if (node.get("config_stacking") or "")=="HORIZONTAL_BOTTOM": ct=top+total_h-ch
                elif (node.get("config_stacking") or "")=="HORIZONTAL_CENTER": ct=top+(total_h-ch)/2
                cb=(cursor,ct,cw,ch)
                cursor+=cw+margin
            flatten(c,cb,root,style,report,base,include_clock,time,role)
        report.add("stack")
        return
    report.unsupported.append("unknown node "+str(t))

REPORT_PRIMS=[]

def layer_nodes(root):
    return root.get("viewgroup_items") or []

def build_layer(variant, layer, style, root):
    global REPORT_PRIMS
    REPORT_PRIMS=[]
    rep=Report()
    bounds=(0,0,SIDE,SIDE)
    tops=layer_nodes(root)
    if layer=="seconds":
        flatten(tops[0],bounds,root,style,rep,include_clock=False,time=(10,10,30))
    elif layer=="hour":
        flatten(tops[1],bounds,root,style,rep,include_clock=False)
    elif layer=="minute":
        flatten(tops[2],bounds,root,style,rep,include_clock=False)
    elif layer=="dial":
        # variant3 static spacecraft: include root DEG180, not a clock mode
        flatten(tops[0],bounds,root,style,rep,include_clock=True)
    return REPORT_PRIMS, rep

def hex_to_rgb(c):
    c=repair_color(c) or "#FFFFFFFF"
    a=int(c[1:3],16); r=int(c[3:5],16); g=int(c[5:7],16); b=int(c[7:9],16)
    return (r,g,b,a)

def svg_path(p: Prim):
    m=p.matrix
    if p.kind=="oval":
        cx=p.x+p.w/2; cy=p.y+p.h/2; rx=p.w/2; ry=p.h/2
        pts=[apply(m,cx-rx,cy),(apply(m,cx,cy-ry)),apply(m,cx+rx,cy),apply(m,cx,cy+ry)]
        a,b,c,d=pts
        return (f"M{a[0]:.3f},{a[1]:.3f} A{rx:.3f},{ry:.3f} 0 1,1 {c[0]:.3f},{c[1]:.3f} "
                f"A{rx:.3f},{ry:.3f} 0 1,1 {a[0]:.3f},{a[1]:.3f} Z")
    if p.kind in ("tri","rtri"):
        if p.kind=="tri": local=[(p.x+p.w/2,p.y),(p.x+p.w,p.y+p.h),(p.x,p.y+p.h)]
        else: local=[(p.x,p.y),(p.x+p.w,p.y),(p.x+p.w,p.y+p.h)]
        pts=[apply(m,x,y) for x,y in local]
        return "M"+" L".join(f"{x:.3f},{y:.3f}" for x,y in pts)+" Z"
    # rounded rect approximated with arcs
    x,y,w,h,r=p.x,p.y,p.w,p.h,max(0,min(p.r,p.w/2,p.h/2))
    if r<=0.01:
        pts=[apply(m,x,y),apply(m,x+w,y),apply(m,x+w,y+h),apply(m,x,y+h)]
        return "M"+" L".join(f"{px:.3f},{py:.3f}" for px,py in pts)+" Z"
    def P(px,py): return apply(m,px,py)
    a=P(x+r,y); b=P(x+w-r,y); c=P(x+w,y+r); d=P(x+w,y+h-r)
    e=P(x+w-r,y+h); f=P(x+r,y+h); g=P(x,y+h-r); hh=P(x,y+r)
    return (f"M{a[0]:.3f},{a[1]:.3f} L{b[0]:.3f},{b[1]:.3f} "
            f"A{r:.3f},{r:.3f} 0 0,1 {c[0]:.3f},{c[1]:.3f} L{d[0]:.3f},{d[1]:.3f} "
            f"A{r:.3f},{r:.3f} 0 0,1 {e[0]:.3f},{e[1]:.3f} L{f[0]:.3f},{f[1]:.3f} "
            f"A{r:.3f},{r:.3f} 0 0,1 {g[0]:.3f},{g[1]:.3f} L{hh[0]:.3f},{hh[1]:.3f} "
            f"A{r:.3f},{r:.3f} 0 0,1 {a[0]:.3f},{a[1]:.3f} Z")

def vector_xml(prims):
    paths=[]
    for p in prims:
        fill=p.fill or "#00000000"; stroke=p.stroke or "#00000000"
        sw=f'\n        android:strokeWidth="{p.stroke_w:.3f}"' if p.stroke else ""
        paths.append(f'    <path\n        android:fillColor="{fill}"\n        android:strokeColor="{stroke}"{sw}\n        android:pathData="{svg_path(p)}" />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{int(SIDE)}dp"\n    android:height="{int(SIDE)}dp"\n'
            f'    android:viewportWidth="{int(SIDE)}"\n    android:viewportHeight="{int(SIDE)}">\n'
            + "\n".join(paths) + "\n</vector>\n")

def render_png(prims, style, path, scale=3, include_bg=True):
    try:
        from PIL import Image, ImageDraw
    except Exception as e:
        print("Pillow unavailable, skipping PNG:",e); return False
    W=int(SIDE*scale)
    img=Image.new("RGBA",(W,W),(0,0,0,0))
    bg={"black":"#FF1B1B1D","translucent":"#A6FFFFFF","transparent":"#00000000"}[style]
    if include_bg and hex_to_rgb(bg)[3]>0:
        d=ImageDraw.Draw(img,"RGBA"); d.rectangle([0,0,W,W],fill=hex_to_rgb(bg))
    draw_ops(prims,img,scale)
    img.save(path)
    return True

def draw_ops(prims, img, scale):
    from PIL import Image, ImageDraw
    W=img.width
    for index,p in enumerate(prims):
        if p.tag in ("clip_all","clip_next"):
            from dataclasses import replace
            from PIL import ImageChops
            mask_img=Image.new("RGBA",(W,W),(0,0,0,0))
            draw_ops([replace(p,tag="",fill="#FFFFFFFF",stroke=None)],mask_img,scale)
            before=img.copy()
            temp=img.copy()
            tail=prims[index+1:] if p.tag=="clip_all" else prims[index+1:index+2]
            draw_ops(tail,temp,scale)
            img.paste(Image.composite(temp,before,mask_img.getchannel("A")))
            if p.tag=="clip_next": draw_ops(prims[index+2:],img,scale)
            return
        if p.kind=="group":
            temp=Image.new("RGBA",(W,W),(0,0,0,0))
            draw_ops(p.children,temp,scale)
            img.alpha_composite(temp)
            continue
        m=p.matrix
        def T(x,y):
            px,py=apply(m,x,y); return (px*scale,py*scale)
        if p.tag=="clear":
            import math as _m
            mask=Image.new("L",(W,W),0); md=ImageDraw.Draw(mask)
            if p.kind=="oval":
                pts=[]
                for i in range(96):
                    a=2*_m.pi*i/96
                    pts.append(T(p.x+p.w/2+p.w/2*_m.cos(a),p.y+p.h/2+p.h/2*_m.sin(a)))
                pts.append(pts[0])
            elif p.kind in ("tri","rtri"):
                local=[(p.x+p.w/2,p.y),(p.x+p.w,p.y+p.h),(p.x,p.y+p.h)] if p.kind=="tri" else [(p.x,p.y),(p.x+p.w,p.y),(p.x+p.w,p.y+p.h)]
                pts=[T(x,y) for x,y in local]; pts.append(pts[0])
            else:
                pts=[T(p.x,p.y),T(p.x+p.w,p.y),T(p.x+p.w,p.y+p.h),T(p.x,p.y+p.h)]; pts.append(pts[0])
            if p.stroke=="stroke":
                w=max(1,int(round((p.stroke_w or 1.0)*scale)))
                md.line(pts,fill=255,width=w,joint="curve")
            else:
                md.polygon(pts,fill=255)
            img.paste((0,0,0,0),(0,0),mask)
            continue
        dr=ImageDraw.Draw(img,"RGBA")
        fill=hex_to_rgb(p.fill) if p.fill else None
        outline=hex_to_rgb(p.stroke) if p.stroke else None
        sw=max(1,int(round(p.stroke_w*scale))) if p.stroke else 1
        if p.kind=="oval":
            import math as _m
            pts=[]
            for i in range(64):
                a=2*_m.pi*i/64
                pts.append(T(p.x+p.w/2+p.w/2*_m.cos(a),p.y+p.h/2+p.h/2*_m.sin(a)))
            dr.polygon(pts,fill=fill,outline=outline,width=sw)
        elif p.kind in ("tri","rtri"):
            local=[(p.x+p.w/2,p.y),(p.x+p.w,p.y+p.h),(p.x,p.y+p.h)] if p.kind=="tri" else [(p.x,p.y),(p.x+p.w,p.y),(p.x+p.w,p.y+p.h)]
            dr.polygon([T(x,y) for x,y in local],fill=fill,outline=outline,width=sw)
        else:
            r=max(0,min(p.r,p.w/2,p.h/2))
            if r>0:
                pts=[]
                for cx,cy,start in [(p.x+p.w-r,p.y+r,-90),(p.x+p.w-r,p.y+p.h-r,0),(p.x+r,p.y+p.h-r,90),(p.x+r,p.y+r,180)]:
                    for j in range(9):
                        a=math.radians(start+j*90/8)
                        pts.append(T(cx+r*math.cos(a),cy+r*math.sin(a)))
            else:
                pts=[T(p.x,p.y),T(p.x+p.w,p.y),T(p.x+p.w,p.y+p.h),T(p.x,p.y+p.h)]
            dr.polygon(pts,fill=fill,outline=outline,width=sw)

def sanitize(src_name, out_name):
    d=json.load(open(os.path.join(SRC,src_name)))
    info=d.get("preset_info") or {}
    root=d.get("preset_root") or {}
    root.pop("internal_events",None)
    out={"width":info.get("width",415),"height":info.get("height",542),
         "source":"AumGupta/KWGT-Widgets "+os.path.splitext(src_name)[0],
         "license":"MIT Copyright (c) 2024 Om Gupta",
         "commit":"96ca27be4cbff35efec5a86c64dd39fd065c958f",
         "items":[root]}
    with open(os.path.join(ASSETS,out_name+".sanitized.json"),"w") as f:
        json.dump(out,f,separators=(",",":"))
    return root

def main():
    os.makedirs(DRAWABLE,exist_ok=True); os.makedirs(DRAWABLE_XXX,exist_ok=True); os.makedirs(DOCS,exist_ok=True)
    summary={}
    for variant,(src_name,asset_name) in VARIANTS.items():
        root=sanitize(src_name,asset_name)
        tops=layer_nodes(root)
        layers=[]
        if variant=="endurance":
            layers=[("seconds",0)]
        elif variant=="analog":
            layers=[("seconds",0),("hour",1),("minute",2)]
        else:
            layers=[("dial",0),("hour",1),("minute",2)]
        summary[variant]={"top_nodes":len(tops),"layers":{}}
        for style in STYLES:
            for layer,idx in layers:
                prims,rep=build_layer(variant,layer,style,root)
                name=f"interstellar_{variant}_{layer}_{style}"
                # Native layers are generated raster PNGs because source CLEAR
                # paint modes require true alpha subtraction; vector XML cannot
                # express erase and would override the correct raster layer.
                # Native AnalogClock hand/dial layers are generated source geometry
                # with CLEAR subtraction, at xxxhdpi so intrinsic size is 277dp.
                render_png(prims,"transparent",os.path.join(DRAWABLE_XXX,name+".png"),scale=4,include_bg=False)
                # preview renders the full source scene with a fixed sample time
                allprims,allrep=build_scene(variant,style,root)
                render_png(allprims,style,os.path.join(DOCS,f"interstellar-{variant}-{style}.png"))
                summary[variant]["layers"][f"{layer}_{style}"]={"prims":len(prims),"unsupported":rep.unsupported,"counts":rep.counts}
        # summary of full scene unsupported
        _,allrep=build_scene(variant,"black",root)
        summary[variant]["unsupported"]=allrep.unsupported
        summary[variant]["scene_counts"]=allrep.counts
    with open(os.path.join(DOCS,"interstellar-compile-report.json"),"w") as f:
        json.dump(summary,f,indent=2)
    print(json.dumps({k:{"tops":v["top_nodes"],"unsupported":v["unsupported"]} for k,v in summary.items()},indent=2))

def build_scene(variant, style, root):
    global REPORT_PRIMS
    REPORT_PRIMS=[]; rep=Report(); bounds=(0,0,SIDE,SIDE); tops=layer_nodes(root)
    if variant=="endurance":
        flatten(tops[0],bounds,root,style,rep,include_clock=True)
    elif variant=="analog":
        flatten(tops[0],bounds,root,style,rep,include_clock=True)
        flatten(tops[1],bounds,root,style,rep,include_clock=True)
        flatten(tops[2],bounds,root,style,rep,include_clock=True)
    else:
        flatten(tops[0],bounds,root,style,rep,include_clock=True)
        flatten(tops[1],bounds,root,style,rep,include_clock=True)
        flatten(tops[2],bounds,root,style,rep,include_clock=True)
    return list(REPORT_PRIMS),rep

if __name__=="__main__":
    main()
