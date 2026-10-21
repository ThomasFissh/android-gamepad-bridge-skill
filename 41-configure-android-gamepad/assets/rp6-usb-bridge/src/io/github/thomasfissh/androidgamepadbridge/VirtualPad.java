package io.github.thomasfissh.androidgamepadbridge;
import org.json.*;
public class VirtualPad {
 static JSONObject navigation()throws Exception {
  return new JSONObject().put("id",72).put("command","register").put("name","Gamepad Bridge Menu").put("vid",0x1209).put("pid",0x5053).put("bus","usb")
   .put("configuration",new JSONArray().put(new JSONObject().put("type",100).put("data",new JSONArray(new int[]{1}))).put(new JSONObject().put("type",101).put("data",new JSONArray(new int[]{158}))));
 }

  static JSONObject registration()throws Exception {
    JSONObject r=new JSONObject().put("id",71).put("command","register").put("name","Gamepad Bridge Controller").put("vid",0x045e).put("pid",0x028e).put("bus","usb");
    JSONArray cfg=new JSONArray();
    cfg.put(new JSONObject().put("type",100).put("data",new JSONArray(new int[]{1,3,21})));
    cfg.put(new JSONObject().put("type",101).put("data",new JSONArray(new int[]{304,305,307,308,310,311,314,315,316,317,318})));
    int[] axes={0,1,2,3,4,5,16,17};
    cfg.put(new JSONObject().put("type",103).put("data",new JSONArray(axes)));
    cfg.put(new JSONObject().put("type",107).put("data",new JSONArray(new int[]{80})));
    r.put("ff_effects_max",16);
    JSONArray abs=new JSONArray();
    for(int c:axes){int lo=c==16||c==17?-1:c==2||c==5?0:-32768, hi=c==16||c==17?1:c==2||c==5?255:32767;
      abs.put(new JSONObject().put("code",c).put("info",new JSONObject().put("value",0).put("minimum",lo).put("maximum",hi).put("fuzz",0).put("flat",0).put("resolution",0)));}
    return r.put("configuration",cfg).put("abs_info",abs);
  }
}
