package com.stratagemhero.mobile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Pure-JVM rules tests; no Android types are referenced. */
public final class LogicTest {
    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        catalogIntegrity();
        catalogIconMappings();
        voiceCueCoverage();
        sessionVoiceCue();
        audioGainAndRoundCompletePicker();
        classicRules();
        classicGameOver();
        effectSingleRules();
        effectDictationRules();
        effectOneHeartRules();
        effectMultiRules();
        effectGreatWallRules();
        effectTerminalRules();
        freeCallRules();

        System.out.println();
        System.out.println("Logic checks: " + checks + ", failures: " + failures);
        if (failures != 0) System.exit(1);
    }

    private static void catalogIntegrity() {
        check("catalog has 119 stratagems", StratagemCatalog.ALL.size() == 119);
        Set<String> ids = new HashSet<>();
        boolean codesOk = true;
        for (Stratagem s : StratagemCatalog.ALL) {
            if (!ids.add(s.id)) codesOk = false;
            if (s.length() < 3 || s.length() > 9) codesOk = false;
            for (Direction d : s.sequence) if (d == null) codesOk = false;
        }
        check("catalog ids unique and codes valid", codesOk);
        check("five public effect modes", EffectMode.values().length == 5);
        check("effect mode keys", EffectMode.fromKey("dictation") == EffectMode.DICTATION
                && EffectMode.fromKey("bogus") == EffectMode.NONE);
        check("classic round time", near(ClassicSession.getRoundTime(1), 15f)
                && near(ClassicSession.getRoundTime(2), 15f / (float) Math.pow(2, 0.3)));
        check("effect round time", near(EffectSession.getSingleTimerMax(1), 8f)
                && near(EffectSession.getSingleTimerMax(2), 8f / (float) Math.pow(2, 0.15)));
    }

    private static void catalogIconMappings() {
        String[][] expected = {
                {"eagle_fly_by", "st_eg_eagle_airstrike"},
                {"eagle_gas_airstrike", "st_eg_eagle_gas_strike"},
                {"pac_meltagun", "st_pac_meltagun"},
                {"pac_apex", "st_pac_apex"},
                {"pac_nova", "st_pac_nova"},
                {"eg_maelstrom", "st_eg_maelstrom"},
                {"ge_portable_comms_relay", "st_ge_cargo_container"},
                {"ge_prospecting_drill", "st_ge_prospecting_drill"},
                {"ge_seize_industrial_complex", "st_ge_super_earth_flag"},
                {"ge_sssd_delivery", "st_ge_upload_data"},
                {"ge_aquifer_drill", "st_ge_drill"},
                {"ge_e711_extraction_drill", "st_ge_drill"},
                {"ge_tactical_video_camera", "st_ge_tactical_video_camera"},
                {"oc_orbital_confetti", "st_br_orbital_precision_strike"}
        };
        for (String[] row : expected) {
            Stratagem s = StratagemCatalog.find(row[0]);
            check("icon mapping " + row[0],
                    s != null && row[1].equals(s.assetName));
        }
    }

    private static void voiceCueCoverage() {
        check("null stratagem has no voice cue", VoiceCue.forStratagem(null) == VoiceCue.NONE);
        boolean allMapped = true;
        for (Stratagem s : StratagemCatalog.ALL) {
            if (VoiceCue.forStratagem(s) == VoiceCue.NONE) allMapped = false;
        }
        check("all 119 stratagems have a voice cue", allMapped);

        check("mission voice cues", cue("resupply") == VoiceCue.RESUPPLY
                && cue("reinforce") == VoiceCue.REINFORCE
                && cue("sos_beacon") == VoiceCue.SOS
                && cue("eg_eagle_rearm") == VoiceCue.REARM
                && cue("ge_hellbomb") == VoiceCue.HELLBOMB
                && cue("ot_hellbomb_portable") == VoiceCue.HELLBOMB);
        check("emplacement voice cues", cue("hmg_emplacement") == VoiceCue.EMPLACEMENT
                && cue("br_grenadier_battlement") == VoiceCue.EMPLACEMENT
                && cue("ot_anti_tank_emplacement") == VoiceCue.EMPLACEMENT);
        check("mine voice cues", cue("eb_anti_personnel_minefield") == VoiceCue.MINE
                && cue("eb_anti_tank_mines") == VoiceCue.MINE
                && cue("eb_incendiary_mines") == VoiceCue.MINE
                && cue("eb_gas_mine") == VoiceCue.MINE);
        check("walker voice cues", cue("rw_patriot_exosuit") == VoiceCue.WALKER
                && cue("rw_emancipator_exosuit") == VoiceCue.WALKER
                && cue("ot_breakthrough_exosuit") == VoiceCue.WALKER
                && cue("ot_lumberer_exosuit") == VoiceCue.WALKER);
        check("sentry and relay voice cues", cue("tesla_tower") == VoiceCue.SENTRY
                && cue("shield_relay") == VoiceCue.FORTIFICATION);
        check("bastion vehicle uses support voice", cue("eg_bastion_mk_xvi") == VoiceCue.SUPPORT);
        check("orbital specialty cues", cue("orbital_laser") == VoiceCue.ORBITAL_LASER
                && cue("br_orbital_gas_strike") == VoiceCue.ORBITAL_GAS
                && cue("br_orbital_ems_strike") == VoiceCue.ORBITAL_EMS
                && cue("br_orbital_smoke_strike") == VoiceCue.ORBITAL_SMOKE);
        check("barrage voice cues", cue("orbital_gatling") == VoiceCue.BARRAGE
                && cue("orbital_120") == VoiceCue.BARRAGE
                && cue("orbital_380") == VoiceCue.BARRAGE
                && cue("orbital_walking") == VoiceCue.BARRAGE
                && cue("orbital_napalm") == VoiceCue.BARRAGE);
        check("other orbital voice cues", cue("orbital_airburst") == VoiceCue.ORBITAL
                && cue("orbital_railcannon") == VoiceCue.ORBITAL
                && cue("br_orbital_precision_strike") == VoiceCue.ORBITAL
                && cue("oc_orbital_confetti") == VoiceCue.ORBITAL);
        check("eagle voice cues", cue("eagle_strafing") == VoiceCue.EAGLE
                && cue("eagle_gas_airstrike") == VoiceCue.EAGLE);
        check("category fallback voice cues", cue("machine_gun") == VoiceCue.SUPPORT
                && cue("supply_pack") == VoiceCue.EQUIPMENT
                && cue("mg_sentry") == VoiceCue.SENTRY);
        check("generic mission voice cues", cue("ge_seaf_artillery") == VoiceCue.OBJECTIVE
                && cue("ge_upload_data") == VoiceCue.OBJECTIVE
                && cue("ge_super_earth_flag") == VoiceCue.OBJECTIVE
                && cue("ge_illumination_flare") == VoiceCue.OBJECTIVE);
    }

    private static void sessionVoiceCue() {
        ClassicSession classic = new ClassicSession(StratagemCatalog.ALL, new Random(71));
        classic.start();
        check("classic starts without a completion cue", classic.getLastCompleted() == null);
        Stratagem first = classic.getCurrent();
        finishClassicTarget(classic);
        check("classic records the completed stratagem", classic.getLastCompleted() == first);
        classic.input(wrongDirection(classic.getCurrent().sequence[0]));
        check("classic wrong input clears the completion cue", classic.getLastCompleted() == null);

        ClassicSession round = new ClassicSession(StratagemCatalog.ALL, new Random(73));
        round.start();
        Stratagem last = null;
        int guard = 0;
        while (round.getState() == ClassicSession.PLAYING && guard++ < 100) {
            last = round.getCurrent();
            finishClassicTarget(round);
        }
        check("classic keeps the final target at round score",
                round.getState() == ClassicSession.ROUND_SCORE && round.getLastCompleted() == last);
        round.update(3.5f);
        round.update(1.25f);
        check("classic next round clears the completion cue",
                round.getState() == ClassicSession.PLAYING && round.getLastCompleted() == null);

        EffectSession single = new EffectSession(StratagemCatalog.ALL, new Random(79),
                EffectMode.NONE, false);
        single.start();
        Stratagem singleTarget = single.getCurrent();
        finishSingleTarget(single);
        check("single records the completed stratagem", single.getLastCompleted() == singleTarget);

        EffectSession effectRound = new EffectSession(StratagemCatalog.ALL, new Random(83),
                EffectMode.NONE, false);
        effectRound.start();
        completeSingleRound(effectRound);
        check("single keeps the final target at intermission",
                effectRound.getLastCompleted() == effectRound.getCurrent());
        effectRound.update(4.3f);
        check("single next round clears the completion cue",
                effectRound.getState() == EffectSession.PLAYING
                        && effectRound.getLastCompleted() == null);

        List<Stratagem> tiny = new ArrayList<>();
        Stratagem line = new Stratagem("test_line", "Test", "测试", StratagemCatalog.MISSION, "UUU", "");
        tiny.add(line);
        EffectSession multi = new EffectSession(tiny, new Random(89), EffectMode.MULTILINES, false);
        multi.start();
        for (int i = 0; i < 3; i++) multi.input(Direction.UP);
        check("multi records the completed lane target",
                multi.getCompletedThisRound() == 4 && multi.getLastCompleted() == line);

        EffectSession terminal = new EffectSession(tiny, new Random(97), EffectMode.TERMINAL, false);
        terminal.start();
        finishTerminalLine(terminal);
        check("terminal completion has no stratagem cue", terminal.getLastCompleted() == null);

        EffectSession wall = new EffectSession(StratagemCatalog.ALL, new Random(101),
                EffectMode.GREATWALL, false);
        wall.start();
        wall.input(wall.getWall().get(0));
        check("wall input has no stratagem cue", wall.getLastCompleted() == null);
    }

    private static VoiceCue cue(String id) {
        return VoiceCue.forStratagem(StratagemCatalog.find(id));
    }

    private static void audioGainAndRoundCompletePicker() {
        check("audio gain clamps percent", AudioGain.clampPercent(-10) == 0
                && AudioGain.clampPercent(100) == 100
                && AudioGain.clampPercent(250) == AudioGain.MAX_PERCENT);
        check("audio gain 100 percent keeps the original mix",
                near(AudioGain.bgmVolume(100), 0.24f)
                        && near(AudioGain.voiceVolume(100), 0.45f));
        check("audio gain 0 and 200 percent endpoints",
                near(AudioGain.bgmVolume(0), 0f)
                        && near(AudioGain.voiceVolume(0), 0f)
                        && near(AudioGain.bgmVolume(200), 0.48f)
                        && near(AudioGain.voiceVolume(200), 0.9f));

        RoundCompletePicker picker = new RoundCompletePicker();
        check("round complete start can use either variant", picker.next(0) == 0);
        check("round complete never repeats the previous variant",
                picker.next(0) == 1 && picker.next(0) == 0
                        && picker.next(1) == 1 && picker.next(1) == 0);
        RoundCompletePicker normalized = new RoundCompletePicker();
        check("round complete normalizes candidates",
                normalized.next(7) == 1 && normalized.next(7) == 0);
    }

    private static void classicRules() {
        ClassicSession s = new ClassicSession(StratagemCatalog.ALL, new Random(7));
        s.start();
        check("classic starts in round 1", s.getRound() == 1 && s.getState() == ClassicSession.PLAYING);
        check("classic round 1 timer", near(s.getTimeMax(), 15f) && near(s.getTimeLeft(), 15f));
        check("classic round 1 target count", s.getRoundTargets() == 3);
        check("classic target installed", s.getCurrent() != null);

        float beforeWrong = s.getTimeLeft();
        Direction wrong = wrongDirection(s.getCurrent().sequence[0]);
        check("classic wrong result", s.input(wrong) == ClassicSession.Result.WRONG);
        check("classic wrong costs no time", near(s.getTimeLeft(), beforeWrong));
        check("classic wrong clears progress", s.getEnteredCount() == 0);
        check("classic wrong loses perfect", s.wasWrongThisRound() && !s.isPerfectRound());
        check("classic wrong locks input", s.getInputLock() > 0f);
        check("classic input blocked during lock",
                s.input(s.getCurrent().sequence[0]) == ClassicSession.Result.IGNORED);
        s.update(0.41f);

        Stratagem first = s.getCurrent();
        int firstLen = first.length();
        finishClassicTarget(s);
        check("classic completion scores arrows", s.getScore() == firstLen * 5);
        check("classic completion advances", s.getCompletedThisRound() == 1);

        float timerBeforeFinish = s.getTimeLeft();
        completeClassicRound(s);
        check("classic round ends in score state", s.getState() == ClassicSession.ROUND_SCORE);
        check("classic round bonus counts arrows", s.getRoundBonus() > 0
                && s.getRoundBonus() % 5 == 0);
        check("classic perfect lost after wrong", s.getPerfectBonus() == 0);
        check("classic score includes round bonus", s.getScore()
                == 2 * s.getRoundBonus() + s.getTimeBonus() + s.getPerfectBonus());
        check("classic last round score", s.getLastRoundScore()
                == s.getRoundBonus() + s.getTimeBonus() + s.getPerfectBonus());
        check("classic best round score", s.getBestRoundScore() == s.getLastRoundScore());
        check("classic round score timer", near(s.getStateTimer(), 3.5f));

        s.update(3.5f);
        check("classic results then ready", s.getState() == ClassicSession.READY);
        s.update(1.25f);
        check("classic next round starts", s.getState() == ClassicSession.PLAYING && s.getRound() == 2);
        check("classic round 2 target count", s.getRoundTargets() == 4);
        check("classic round 2 timer", near(s.getTimeMax(), 15f / (float) Math.pow(2, 0.3)));
        check("classic round 2 timer full", near(s.getTimeLeft(), s.getTimeMax()));

        ClassicSession perfect = new ClassicSession(StratagemCatalog.ALL, new Random(11));
        perfect.start();
        completeClassicRound(perfect);
        check("classic perfect bonus", perfect.getPerfectBonus() == 100
                && perfect.isPerfectRound() && !perfect.wasWrongThisRound());
        check("classic timer not negative after finish", perfect.getTimeLeft() > 0f
                && timerBeforeFinish > 0f);
    }

    private static void classicGameOver() {
        ClassicSession s = new ClassicSession(StratagemCatalog.ALL, new Random(3));
        s.start();
        s.update(15.1f);
        check("classic timer expiry ends run", s.isOver() && near(s.getTimeLeft(), 0f));
        check("classic ignores input after over",
                s.input(s.getCurrent() == null ? Direction.UP : s.getCurrent().sequence[0])
                        == ClassicSession.Result.IGNORED);
    }

    private static void effectSingleRules() {
        List<Stratagem> catalog = StratagemCatalog.ALL;
        EffectSession s = new EffectSession(catalog, new Random(17), EffectMode.NONE, false);
        s.start();
        check("single starts round 1", s.getState() == EffectSession.PLAYING && s.getRound() == 1);
        check("single timer max", near(s.getTimeMax(), 8f) && near(s.getTimeLeft(), 8f));
        check("single round 1 count", s.getRoundTargets() == 3);

        Stratagem first = s.getCurrent();
        int length = first.length();
        finishSingleTarget(s);
        check("single scores line length", s.getScore() == length
                && s.getArrowCompleted() == length && s.getArrowsThisRound() == length);
        check("single completion revives timer", near(s.getTimeLeft(), 8f));

        completeSingleRound(s);
        check("single round reaches intermission", s.isIntermission());
        check("single time bonus", s.getLastTimeBonus() == (int) (s.getTimeLeft() / s.getTimeMax() * 10f));
        check("single perfect bonus", s.getLastPerfectBonus() == s.getArrowsThisRound());
        check("single round score", s.getLastRoundScore()
                == s.getLastTimeBonus() + s.getLastPerfectBonus());
        check("single score accumulated", s.getScore()
                == s.getArrowCompleted() + s.getLastTimeBonus() + s.getLastPerfectBonus());
        check("single intermission length", near(s.getIntermissionLeft(), 4.3f));

        s.update(4.3f);
        check("single next round", s.getState() == EffectSession.PLAYING && s.getRound() == 2);
        check("single round 2 timer", near(s.getTimeMax(), 8f / (float) Math.pow(2, 0.15)));
        check("single round 2 count", s.getRoundTargets() == 3);

        EffectSession shortTimer = new EffectSession(catalog, new Random(19), EffectMode.NONE, false);
        shortTimer.start();
        shortTimer.update(5f);
        Stratagem target = shortTimer.getCurrent();
        int len = target.length();
        float before = shortTimer.getTimeLeft();
        finishSingleTarget(shortTimer);
        float expected = Math.min(8f, before + 4f);
        check("single revive is 4/sqrt(round)", near(shortTimer.getTimeLeft(), expected));
        check("single short timer bonus math", len > 0);
    }

    private static void effectDictationRules() {
        EffectSession s = new EffectSession(StratagemCatalog.ALL, new Random(23),
                EffectMode.DICTATION, false);
        s.start();
        check("dictation timer x6", near(s.getTimeMax(), 48f) && near(s.getTimeLeft(), 48f));
        s.update(30f);
        check("dictation timer drains", near(s.getTimeLeft(), 18f));
        finishSingleTarget(s);
        check("dictation revive x6", near(s.getTimeLeft(), Math.min(48f, 18f + 24f)));

        s.update(0.01f);
        float beforeWrong = s.getTimeLeft();
        Direction wrong = wrongDirection(s.getCurrent().sequence[0]);
        s.input(wrong);
        check("dictation wrong costs 1.5s", near(s.getTimeLeft(), beforeWrong - 1.5f));
        float afterFirstWrong = s.getTimeLeft();
        s.input(wrongDirection(s.getCurrent().sequence[0]));
        check("dictation wrong protection", near(s.getTimeLeft(), afterFirstWrong));
        check("dictation wrong loses perfect", !s.isPerfect());
    }

    private static void effectOneHeartRules() {
        EffectSession single = new EffectSession(StratagemCatalog.ALL, new Random(29),
                EffectMode.NONE, true);
        single.start();
        single.input(wrongDirection(single.getCurrent().sequence[0]));
        check("one heart wrong ends single", single.isOver() && near(single.getTimeLeft(), 0f));

        EffectSession dictation = new EffectSession(StratagemCatalog.ALL, new Random(31),
                EffectMode.DICTATION, true);
        dictation.start();
        dictation.input(wrongDirection(dictation.getCurrent().sequence[0]));
        check("one heart dictation ends", dictation.isOver());

        EffectSession wall = new EffectSession(StratagemCatalog.ALL, new Random(37),
                EffectMode.GREATWALL, true);
        wall.start();
        wall.input(wrongDirection(wall.getWall().get(0)));
        check("one heart wall ends", wall.isOver() && near(wall.getTimeLeft(), 0f));

        EffectSession terminal = new EffectSession(StratagemCatalog.ALL, new Random(41),
                EffectMode.TERMINAL, true);
        terminal.start();
        terminal.input(wrongDirection(terminal.getTerminalLine().get(0)));
        check("one heart terminal ends", terminal.isOver());
    }

    private static void effectMultiRules() {
        List<Stratagem> tiny = new ArrayList<>();
        tiny.add(new Stratagem("test_line", "Test", "测试", StratagemCatalog.MISSION, "UUU", ""));
        EffectSession s = new EffectSession(tiny, new Random(43), EffectMode.MULTILINES, false);
        s.start();
        check("multi has four lanes", s.getLanes().length == 4);
        boolean allTargets = true;
        for (EffectSession.Lane lane : s.getLanes()) allTargets &= lane.getTarget() != null;
        check("multi installs all lanes", allTargets);
        check("multi pool count", s.getPoolRemaining() == 4 && s.getRoundTargets() == 8);
        check("multi timer", near(s.getTimeMax(), 8f) && near(s.getTimeLeft(), 8f));

        s.input(Direction.UP);
        check("multi input advances lanes", s.getLanes()[0].getEnteredCount() == 1);
        s.input(Direction.UP);
        s.input(Direction.UP);
        check("multi first wave completed", s.getCompletedThisRound() == 4
                && s.getArrowCompleted() == 12 && s.getScore() == 12);
        check("multi pool refilled lanes", s.getPoolRemaining() == 0);
        s.input(Direction.UP);
        s.input(Direction.UP);
        s.input(Direction.UP);
        check("multi round completes at intermission", s.isIntermission());
        check("multi second wave score", s.getScore() == 58 && s.getArrowCompleted() == 24);
        check("multi perfect bonus", s.getLastPerfectBonus() == 24 && s.isPerfect());
        check("multi time bonus", s.getLastTimeBonus() == 10);
        check("multi round score", s.getLastRoundScore() == 34);
        s.update(4.3f);
        check("multi next round", s.getRound() == 2 && s.getState() == EffectSession.PLAYING);
        check("multi round 2 count", s.getRoundTargets() == 8 && s.getPoolRemaining() == 4);

        EffectSession wrong = new EffectSession(tiny, new Random(47), EffectMode.MULTILINES, false);
        wrong.start();
        float before = wrong.getTimeLeft();
        check("multi wrong result", wrong.input(Direction.RIGHT) == EffectSession.Result.WRONG);
        check("multi wrong penalty once", near(wrong.getTimeLeft(), before - 0.3f));
        float after = wrong.getTimeLeft();
        wrong.input(Direction.RIGHT);
        check("multi wrong protection", near(wrong.getTimeLeft(), after));
        check("multi wrong loses perfect", !wrong.isPerfect());
    }

    private static void effectGreatWallRules() {
        EffectSession s = new EffectSession(StratagemCatalog.ALL, new Random(53),
                EffectMode.GREATWALL, false);
        s.start();
        check("wall starts full", s.getWall().size() == 8 && near(s.getTimeLeft(), 15f));
        check("wall has no round", s.getRound() == 1);
        Direction first = s.getWall().get(0);
        check("wall correct result", s.input(first) == EffectSession.Result.CORRECT);
        check("wall arrow counted", s.getArrowCompleted() == 1 && s.getScore() == 1);
        check("wall refills", s.getWall().size() == 8);
        s.update(2f);
        check("wall drains", near(s.getTimeLeft(), 13f));
        s.input(s.getWall().get(0));
        check("wall revive toward max", near(s.getTimeLeft(), 15f));

        s.update(1f);
        float before = s.getTimeLeft();
        check("wall wrong result",
                s.input(wrongDirection(s.getWall().get(0))) == EffectSession.Result.WRONG);
        check("wall wrong costs 0.75s", near(s.getTimeLeft(), before - 0.75f));
        float after = s.getTimeLeft();
        s.input(wrongDirection(s.getWall().get(0)));
        check("wall wrong protection", near(s.getTimeLeft(), after));

        EffectSession over = new EffectSession(StratagemCatalog.ALL, new Random(59),
                EffectMode.GREATWALL, false);
        over.start();
        over.update(15.01f);
        check("wall timer ends run", over.isOver());
    }

    private static void effectTerminalRules() {
        List<Stratagem> tiny = new ArrayList<>();
        tiny.add(new Stratagem("test_line", "Test", "测试", StratagemCatalog.MISSION, "UUU", ""));
        EffectSession s = new EffectSession(tiny, new Random(61), EffectMode.TERMINAL, false);
        s.start();
        check("terminal timer", near(s.getTimeMax(), 20f) && near(s.getTimeLeft(), 20f));
        check("terminal first line 5", s.getTerminalLine().size() == 5);
        check("terminal line number", s.getTerminalLineNumber() == 1 && s.getLinesCompleted() == 0);

        s.update(1f);
        Direction wrong = wrongDirection(s.getTerminalLine().get(0));
        float before = s.getTimeLeft();
        check("terminal wrong result", s.input(wrong) == EffectSession.Result.WRONG);
        check("terminal wrong costs 0.75s", near(s.getTimeLeft(), before - 0.75f));
        float after = s.getTimeLeft();
        s.input(wrong);
        check("terminal wrong protection", near(s.getTimeLeft(), after));

        s.update(1f);
        finishTerminalLine(s);
        check("terminal completes first line", s.getLinesCompleted() == 1
                && s.getArrowCompleted() == 5 && s.getScore() == 5);
        check("terminal next line 6", s.getTerminalLine().size() == 6);
        check("terminal timer revived toward 20", s.getTimeLeft() > 0f
                && s.getTimeLeft() <= 20f);

        s.update(15f);
        float beforeSecond = s.getTimeLeft();
        finishTerminalLine(s);
        check("terminal second line complete", s.getLinesCompleted() == 2
                && s.getArrowCompleted() == 11);
        float revive = Math.min(20f, beforeSecond + 11.25f);
        check("terminal second revive 11.25", near(s.getTimeLeft(), revive));

        s.update(20f);
        check("terminal timer ends run", s.isOver());
    }

    private static void freeCallRules() {
        List<Stratagem> tiny = new ArrayList<>();
        Stratagem test = new Stratagem("test_line", "Test", "测试", StratagemCatalog.MISSION, "UUU", "");
        tiny.add(test);
        GameSession s = new GameSession(tiny, new Random(67));
        s.start();
        check("free call starts untimed", s.isFreeCall() && s.getState() == GameSession.RUNNING);
        check("free arrow prefix", s.input(Direction.UP) == GameSession.Result.CORRECT
                && s.getEnteredCount() == 1);
        check("free full code", s.input(Direction.UP) == GameSession.Result.CORRECT);
        check("free completes exact", s.input(Direction.UP) == GameSession.Result.COMPLETE);
        check("free records completion", s.getCompleted() == 1 && s.getLastCompleted() == test
                && s.getScore() == 0 && s.getInputCode().isEmpty());
        check("free wrong clears", s.input(Direction.RIGHT) == GameSession.Result.WRONG
                && s.getMistakes() == 1 && s.getInputCode().isEmpty());
        check("free accuracy", s.getAccuracy() < 1f);
    }

    private static void finishClassicTarget(ClassicSession s) {
        if (s.getCurrent() == null) return;
        Stratagem target = s.getCurrent();
        int guard = 0;
        while (s.getState() == ClassicSession.PLAYING && s.getCurrent() == target
                && s.getEnteredCount() < target.length() && guard++ < 20) {
            s.input(target.sequence[s.getEnteredCount()]);
        }
    }

    private static void completeClassicRound(ClassicSession s) {
        int guard = 0;
        while (s.getState() == ClassicSession.PLAYING && guard++ < 1000) {
            finishClassicTarget(s);
        }
    }

    private static void finishSingleTarget(EffectSession s) {
        if (s.getCurrent() == null) return;
        Stratagem target = s.getCurrent();
        int guard = 0;
        while (s.getState() == EffectSession.PLAYING && s.getCurrent() == target
                && s.getEnteredCount() < target.length() && guard++ < 20) {
            s.input(target.sequence[s.getEnteredCount()]);
        }
    }

    private static void completeSingleRound(EffectSession s) {
        int guard = 0;
        while (s.getState() == EffectSession.PLAYING && guard++ < 1000) {
            finishSingleTarget(s);
        }
    }

    private static void finishTerminalLine(EffectSession s) {
        List<Direction> line = s.getTerminalLine();
        for (int i = s.getTerminalEntered(); i < line.size(); i++) {
            s.input(line.get(i));
        }
    }

    private static Direction wrongDirection(Direction correct) {
        for (Direction d : Direction.values()) if (d != correct) return d;
        return Direction.LEFT;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.001f;
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }
}
