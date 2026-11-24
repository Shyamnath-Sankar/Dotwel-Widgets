import json, sys, unittest, os
sys.path.insert(0, os.path.dirname(__file__))
import compile_endurance as c

class CompilerSemanticsTest(unittest.TestCase):
    def root(self, name="Interstellar_Endurance.json"):
        with open(os.path.join(c.SRC,name)) as source:
            d=json.load(source)
        r=d["preset_root"]
        return r

    def test_toggle_raw_formula_global(self):
        r=self.root()
        mods=r["viewgroup_items"][0]["viewgroup_items"][0]["viewgroup_items"][1]["viewgroup_items"]
        m12=mods[0]
        # raw DEG180 must win because config_rotate_mode toggle is 0
        self.assertEqual(c.resolve(m12,"config_rotate_mode",r),"DEG180")
        # inactive CLOCK_SECOND formula on the inner overlap must not win
        inner=m12["viewgroup_items"][0]
        self.assertNotEqual(c.resolve(inner,"config_rotate_mode",r),"CLOCK_SECOND")
        # paint_color toggle 100 is a named global, resolved through style palette
        shape=m12["viewgroup_items"][0]["viewgroup_items"][0]["viewgroup_items"][0]["viewgroup_items"][0]
        self.assertEqual(c.resolve(shape,"paint_color",r),"fg")
        self.assertEqual(c.PALETTE["black"]["fg"],c.color_value(shape,"paint_color",r,"black"))

    def test_rotated_shape_measure_uses_transformed_bbox(self):
        node={"internal_type":"ShapeModule","shape_width":35.0,"shape_height":100.0,"shape_rotate_mode":"DEG270","internal_toggles":{}}
        w,h=c.measure(node,277,277,self.root())
        self.assertEqual((100.0,35.0),(w,h))

    def test_progress_defaults_to_circle_and_clear_is_mask(self):
        node={"internal_type":"ProgressModule","progress_count":12,"style_size":238.0,"style_width":22.0,
              "style_height":8.0,"style_shape":None,"internal_toggles":{},"internal_formulas":{}}
        c.REPORT_PRIMS=[]; rep=c.Report()
        c.flatten(node,(0,0,277,277),self.root(),"black",rep,include_clock=False)
        self.assertTrue(c.REPORT_PRIMS)
        self.assertTrue(all(p.kind=="oval" for p in c.REPORT_PRIMS))
        self.assertTrue(all(abs(p.w-p.h)<0.001 for p in c.REPORT_PRIMS))
        clear={"internal_type":"ShapeModule","shape_width":2.0,"shape_height":2.0,"shape_type":"RECT",
               "paint_style":"STROKE","paint_mode":"CLEAR","paint_stroke":0.5,"internal_toggles":{}}
        c.REPORT_PRIMS=[]; c.flatten(clear,(0,0,277,277),self.root(),"black",rep,include_clock=False)
        self.assertEqual("clear",c.REPORT_PRIMS[0].tag)
        self.assertEqual("stroke",c.REPORT_PRIMS[0].stroke)

    def test_clip_mask_is_invisible_and_only_paints_inside(self):
        from PIL import Image
        mask=c.Prim("oval",2,2,6,6,fill="#FFFFFFFF",tag="clip_all",matrix=((1,0,0),(0,1,0),(0,0,1)))
        fill=c.Prim("rect",0,0,10,10,fill="#FFFF0000",matrix=((1,0,0),(0,1,0),(0,0,1)))
        im=Image.new("RGBA",(10,10),(0,0,0,0));c.draw_ops([mask,fill],im,1)
        self.assertEqual((255,0,0,255),im.getpixel((5,5)))
        self.assertEqual(0,im.getpixel((0,0))[3])
        empty=Image.new("RGBA",(10,10),(0,0,0,0));c.draw_ops([mask],empty,1)
        self.assertIsNone(empty.getbbox())

    def test_ranger_mask_measure_is_not_the_hidden_triangle_bounds(self):
        r=self.root();rangers=r["viewgroup_items"][0]["viewgroup_items"][0]["viewgroup_items"][-2]
        self.assertEqual((58.0,13.0),c.measure(rangers["viewgroup_items"][0],277,277,r))

    def test_nested_offsets_are_inactive_and_padding_positions_children(self):
        from dataclasses import asdict
        r=self.root()
        node={"internal_type":"OverlapLayerModule","position_offset_x":99,"position_padding_left":10,
              "viewgroup_items":[{"internal_type":"ShapeModule","shape_width":5,"shape_height":5}]}
        c.REPORT_PRIMS=[];c.flatten(node,(0,0,50,50),r,"black",c.Report(),nested=True)
        before=asdict(c.REPORT_PRIMS[0])
        node["position_offset_x"]=0
        c.REPORT_PRIMS=[];c.flatten(node,(0,0,50,50),r,"black",c.Report(),nested=True)
        self.assertEqual(before,asdict(c.REPORT_PRIMS[0]))

if __name__=="__main__":
    unittest.main()
