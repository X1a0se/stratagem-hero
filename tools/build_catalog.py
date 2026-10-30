# -*- coding: utf-8 -*-
import json, re, os
ROOT = r"D:/Work of DSH/stratagem hero"
mobile = json.load(open(ROOT + "/data/mobile_stratagems.json", encoding="utf-8"))
hdw = json.load(open(ROOT + "/data/hdw_catalog.json", encoding="utf-8"))
hdw_by_code = {}
for s in hdw:
    hdw_by_code.setdefault(s["code"], s)
extras = json.load(open(ROOT + "/data/wiki_extra.json", encoding="utf-8"))
wiki_names = json.load(open(ROOT + "/data/wiki_names.json", encoding="utf-8"))

CAT = {
 # general
 "ge_resupply":"MISSION","ge_reinforce":"MISSION","ge_sos_beacon":"MISSION","ge_hellbomb":"MISSION",
 "ge_seaf_artillery":"MISSION","ge_super_earth_flag":"MISSION","ge_seismic_probe":"MISSION",
 "ge_tectonic_drill":"MISSION","ge_illumination_flare":"MISSION","ge_upload_data":"MISSION",
 "ge_hive_breaker_drill":"MISSION","ge_dark_fluid_vessel":"MISSION","ge_call_in_super_destroyer":"MISSION",
 "ge_cargo_container":"MISSION",
 # eagle
 "eg_eagle_rearm":"MISSION","eg_fast_recon_vehicle":"SUPPORT","eg_bastion_mk_xvi":"DEFENSIVE",
 # pac
 "pac_sta_x3_wasp_launcher":"SUPPORT",
 # bridge
 "br_orbital_precision_strike":"OFFENSIVE","br_orbital_gas_strike":"OFFENSIVE","br_orbital_ems_strike":"OFFENSIVE",
 "br_orbital_smoke_strike":"OFFENSIVE","br_hmg_emplacement":"DEFENSIVE","br_shield_generator_relay":"DEFENSIVE",
 "br_tesla_tower":"DEFENSIVE","br_grenadier_battlement":"DEFENSIVE",
 # engineering
 "eb_anti_personnel_minefield":"DEFENSIVE","eb_anti_tank_mines":"DEFENSIVE","eb_supply_pack":"BACKPACK",
 "eb_grenade_launcher":"SUPPORT","eb_laser_cannon":"SUPPORT","eb_incendiary_mines":"DEFENSIVE",
 "eb_guard_dog_rover":"BACKPACK","eb_ballistic_shield_backpack":"BACKPACK","eb_arc_thrower":"SUPPORT",
 "eb_quasar_cannon":"SUPPORT","eb_shield_generator_pack":"BACKPACK","eb_gas_mine":"DEFENSIVE",
 # robotics
 "rw_machine_gun_sentry":"SENTRY","rw_gatling_sentry":"SENTRY","rw_mortar_sentry":"SENTRY",
 "rw_guard_dog":"BACKPACK","rw_autocannon_sentry":"SENTRY","rw_rocket_sentry":"SENTRY",
 "rw_ems_mortar_sentry":"SENTRY","rw_patriot_exosuit":"DEFENSIVE","rw_emancipator_exosuit":"DEFENSIVE",
 # others
 "ot_sterilizer":"SUPPORT","ot_guard_dog_breath":"BACKPACK","ot_directional_shield":"BACKPACK",
 "ot_flame_sentry":"SENTRY","ot_anti_tank_emplacement":"DEFENSIVE","ot_hellbomb_portable":"BACKPACK",
 "ot_hover_pack":"BACKPACK","ot_one_true_flag":"MISSION","ot_gl_52_de_escalator":"SUPPORT",
 "ot_guard_dog_k_9":"BACKPACK","ot_warp_pack":"BACKPACK","ot_laser_sentry":"SENTRY",
 "ot_epoch":"SUPPORT","ot_expendable_napalm":"SUPPORT","ot_solo_silo":"SUPPORT",
 "ot_speargun":"SUPPORT","ot_defoliation_tool":"SUPPORT","ot_guard_dog_hot_dog":"BACKPACK",
 "ot_maxigun":"SUPPORT","ot_c4_pack":"BACKPACK","ot_gl_28":"SUPPORT","ot_eat_411":"SUPPORT",
 "ot_cqc_20":"SUPPORT","ot_cremator":"SUPPORT","ot_gas_mortar_sentry":"SENTRY",
 "ot_supply_frv":"SUPPORT","ot_incinerator_frv":"SUPPORT","ot_breakthrough_exosuit":"DEFENSIVE",
 "ot_lumberer_exosuit":"DEFENSIVE","ot_bullet_storm":"SUPPORT",
}

# Chinese labels for entries not in HDW, curated from mobile + wiki style.
ZH = {
 "hellbomb":"地狱火炸弹","seaf_artillery":"超级地球大炮","super_earth_flag":"超级地球旗帜","seismic_probe":"地震探测器",
 "tectonic_drill":"地壳钻机","illumination_flare":"轨道照明弹","upload_data":"上传数据","hive_breaker_drill":"破巢钻机",
 "dark_fluid_vessel":"暗流体容器","call_in_super_destroyer":"呼叫超级驱逐舰","cargo_container":"货柜",
 "eagle_rearm":"飞鹰重新装填","fast_recon_vehicle":"快速侦察载具","bastion_mk_xvi":"堡垒 MK XVI",
 "sta_x3_wasp_launcher":"StA-X3 黄蜂发射器",
 "orbital_precision_strike":"轨道精准打击","orbital_gas_strike":"轨道毒气打击","orbital_ems_strike":"轨道 EMS 打击",
 "orbital_smoke_strike":"轨道烟雾打击","hmg_emplacement":"重机枪阵地","shield_generator_relay":"护盾发生器中继",
 "tesla_tower":"特斯拉塔","grenadier_battlement":"掷弹兵堡垒",
 "anti_personnel_minefield":"反步兵雷区","anti_tank_mines":"反坦克地雷","supply_pack":"补给背包",
 "grenade_launcher":"榴弹发射器","laser_cannon":"激光炮","incendiary_mines":"燃烧地雷",
 "guard_dog_rover":"护卫犬 Rover","ballistic_shield_backpack":"弹道护盾背包","arc_thrower":"电弧发射器",
 "quasar_cannon":"类星体炮","shield_generator_pack":"护盾发生器背包","gas_mine":"毒气地雷",
 "machine_gun_sentry":"机枪哨戒塔","gatling_sentry":"加特林哨戒塔","mortar_sentry":"迫击炮哨戒塔",
 "guard_dog":"护卫犬","autocannon_sentry":"机炮哨戒塔","rocket_sentry":"火箭哨戒塔",
 "ems_mortar_sentry":"EMS 迫击炮哨戒塔","patriot_exosuit":"爱国者外骨骼","emancipator_exosuit":"解放者外骨骼",
 "sterilizer":"灭菌器","guard_dog_breath":"护卫犬 吐息","directional_shield":"定向护盾","flame_sentry":"火焰哨戒塔",
 "anti_tank_emplacement":"反坦克阵地","hellbomb_portable":"便携式地狱火炸弹","hover_pack":"悬浮背包",
 "one_true_flag":"唯一真旗","gl_52_de_escalator":"GL-52 减压器","guard_dog_k_9":"护卫犬 K-9",
 "warp_pack":"跃迁背包","laser_sentry":"激光哨戒塔","epoch":"纪元","expendable_napalm":"一次性凝固汽油弹",
 "solo_silo":"单兵发射井","speargun":"矛枪","defoliation_tool":"除叶工具","guard_dog_hot_dog":"护卫犬 热狗",
 "maxigun":"Maxi 机枪","c4_pack":"C4 背包","gl_28":"GL-28 弹链榴弹发射器","eat_411":"EAT-411 夷平者",
 "cqc_20":"CQC-20 破门锤","cremator":"焚化器","gas_mortar_sentry":"毒气迫击炮哨戒塔",
 "supply_frv":"M-103 补给 FRV","incinerator_frv":"M-104 焚化 FRV","breakthrough_exosuit":"突破者外骨骼",
 "lumberer_exosuit":"伐木者外骨骼","bullet_storm":"弹幕风暴",
}

items = []
used_ids = set()
used_names = set()
for group, entries in mobile.items():
    for e in entries:
        icon = e["id/icon"]
        slug = icon[3:] if icon.startswith("st_") else icon
        code = "".join(x[0].upper() for x in e["gesture"])
        h = hdw_by_code.get(code)
        if h:
            sid = h["id"]; name = h["name"]; zh = h["nameZh"]; cat = h["category"]
        else:
            sid = slug
            name = e["en_name"].strip()
            zh = ZH.get(slug, e["zh_name"].strip())
            cat = CAT.get(slug)
            if not cat:
                raise SystemExit("no category for " + slug)
        if sid in used_ids:
            sid = sid + "_" + group[:2]
        if sid in used_ids: raise SystemExit("dup id " + sid)
        # Keep names unique in the UI.
        base_name = name
        n = 2
        while name in used_names:
            name = base_name + " " + str(n); n += 1
        used_ids.add(sid); used_names.add(name)
        items.append({"id":sid,"name":name,"nameZh":zh,"category":cat,"code":code,"icon":icon,"group":group})

# Wiki-aligned names and call-in codes for the base catalog entries.
override = {e["id"]: e for e in wiki_names}
seen_ids = set()
for it in items:
    o = override.get(it["id"])
    if o is None:
        continue
    it["name"] = o["name"]
    it["nameZh"] = o["nameZh"]
    it["code"] = o["code"]
    seen_ids.add(it["id"])
missing = sorted(set(override) - seen_ids)
if missing:
    raise SystemExit("wiki_names has unknown ids: " + ", ".join(missing))
uncovered = sorted(i["id"] for i in items if i["id"] not in override)
if uncovered:
    raise SystemExit("wiki_names missing ids: " + ", ".join(uncovered))

# Wiki-only stratagems shipped by the game data source but absent from the base catalog.
used_ids = {i["id"] for i in items}
used_names = {i["name"] for i in items}
for e in extras:
    if e["id"] in used_ids:
        raise SystemExit("dup extra id " + e["id"])
    if e["name"] in used_names:
        raise SystemExit("dup extra name " + e["name"])
    used_ids.add(e["id"]); used_names.add(e["name"])
    items.append({"id":e["id"],"name":e["name"],"nameZh":e["nameZh"],
                  "category":e["category"],"code":e["code"],"icon":e["icon"],"group":e["group"]})

def jstr(s):
    return '"' + s.replace("\\","\\\\").replace('"','\\"') + '"'

lines = []
lines.append("package com.stratagemhero.mobile;")
lines.append("")
lines.append("import java.util.ArrayList;")
lines.append("import java.util.Collections;")
lines.append("import java.util.List;")
lines.append("")
lines.append("/** Generated from HDWearStrategy catalog.json + HD2-Stratagem-Hero-Mobile stratagems.json;")
lines.append(" * names and call-in codes aligned to the Helldivers Wiki. */")
lines.append("public final class StratagemCatalog {")
lines.append("    public static final String MISSION = \"MISSION\";")
lines.append("    public static final String SUPPORT = \"SUPPORT\";")
lines.append("    public static final String OFFENSIVE = \"OFFENSIVE\";")
lines.append("    public static final String BACKPACK = \"BACKPACK\";")
lines.append("    public static final String SENTRY = \"SENTRY\";")
lines.append("    public static final String DEFENSIVE = \"DEFENSIVE\";")
lines.append("")
lines.append("    public static final List<Stratagem> ALL;")
lines.append("    static {")
lines.append("        List<Stratagem> all = new ArrayList<>();")
for i in items:
    lines.append("        all.add(new Stratagem(%s, %s, %s, %s, %s, %s));" % (jstr(i["id"]), jstr(i["name"]), jstr(i["nameZh"]), jstr(i["category"]), jstr(i["code"]), jstr(i["icon"])))
lines.append("        ALL = Collections.unmodifiableList(all);")
lines.append("    }")
lines.append("")
lines.append("    private StratagemCatalog() {}")
lines.append("")
lines.append("    public static Stratagem find(String id) {")
lines.append("        for (Stratagem s : ALL) if (s.id.equals(id)) return s;")
lines.append("        return null;")
lines.append("    }")
lines.append("}")
from collections import Counter
expected = sum(len(v) for v in mobile.values()) + len(extras)
if len(items) != expected:
    raise SystemExit("expected %d entries, got %d" % (expected, len(items)))
for field in ("id", "name", "nameZh"):
    vals = [i[field] for i in items]
    dups = [v for v,n in Counter(vals).items() if n > 1]
    if dups:
        raise SystemExit("dup %s: %s" % (field, dups))
bad_codes = [i["id"] for i in items if not re.fullmatch(r"[DURL]{3,9}", i["code"])]
if bad_codes:
    raise SystemExit("invalid codes: " + ", ".join(bad_codes))
dup_codes = {c:n for c,n in Counter(i["code"] for i in items).items() if n > 1}
if dup_codes != {"DDDDDUU": 2}:
    raise SystemExit("unexpected duplicate codes: %r" % dup_codes)
missing_icons = [i["icon"] for i in items if not os.path.exists(ROOT + "/app/res/drawable/" + i["icon"] + ".xml")]
if missing_icons:
    raise SystemExit("missing icons: " + ", ".join(missing_icons))

open(ROOT + "/app/src/com/stratagemhero/mobile/StratagemCatalog.java", "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
json.dump(items, open(ROOT + "/data/catalog.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print("items", len(items), "categories", Counter(i["category"] for i in items))
print("maxlen", max(len(i["code"]) for i in items), "lengths", Counter(len(i["code"]) for i in items))
print("dup codes", dup_codes, "at", [i["id"] for i in items if i["code"] == "DDDDDUU"])
print("extras", len(extras), [i["id"] for i in extras])
