package com.stratagemhero.mobile;

/** The five public Effect Mode variants from StratagemHero_Effect. */
public enum EffectMode {
    NONE("none", "无特殊效果", "No Effect",
            "没有特殊效果的模式：单行战备，时间上限 8 秒并逐回合收紧。"),
    DICTATION("dictation", "默写模式", "Dictation",
            "不会显示箭头，用于练习记忆和盲打；时间上限与完成回复 ×6，错误惩罚 ×5。"),
    GREATWALL("greatwall", "长城模式", "Great Wall",
            "连续的、无规律的无尽箭头序列，用于练习即时反应能力；同时显示 8 个箭头。"),
    MULTILINES("multilines", "多行模式", "Multi-Lines",
            "四行战备同时出现，完成屏幕上的所有战备以通关一回合。"),
    TERMINAL("terminal", "终端模式", "Terminal",
            "模拟终端上的随机长序列，用于练习输入终端的技术；每行会越来越长。");

    public final String key;
    public final String zh;
    public final String en;
    public final String description;

    EffectMode(String key, String zh, String en, String description) {
        this.key = key;
        this.zh = zh;
        this.en = en;
        this.description = description;
    }

    public String label(boolean english) {
        return english ? en : zh;
    }

    public String prefKey() {
        return "effect_" + key;
    }

    public static EffectMode fromKey(String key) {
        if (key != null) {
            for (EffectMode m : values()) {
                if (m.key.equals(key)) return m;
            }
        }
        return NONE;
    }
}
