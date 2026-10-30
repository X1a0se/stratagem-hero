package com.stratagemhero.mobile;

/** Maps a stratagem to the Mission Control voice group used for its call-in. */
enum VoiceCue {
    NONE,
    RESUPPLY,
    REINFORCE,
    SOS,
    REARM,
    HELLBOMB,
    OBJECTIVE,
    SUPPORT,
    EQUIPMENT,
    SENTRY,
    FORTIFICATION,
    EMPLACEMENT,
    MINE,
    WALKER,
    EAGLE,
    BARRAGE,
    ORBITAL,
    ORBITAL_LASER,
    ORBITAL_GAS,
    ORBITAL_EMS,
    ORBITAL_SMOKE;

    static VoiceCue forStratagem(Stratagem s) {
        if (s == null) return NONE;

        // Explicit overrides where the catalog category or the name is not the
        // same thing as the voice family heard in game.
        switch (s.id) {
            case "resupply":
                return RESUPPLY;
            case "reinforce":
                return REINFORCE;
            case "sos_beacon":
                return SOS;
            case "eg_eagle_rearm":
                return REARM;
            case "ge_hellbomb":
            case "ot_hellbomb_portable":
                return HELLBOMB;
            case "hmg_emplacement":
            case "br_grenadier_battlement":
            case "ot_anti_tank_emplacement":
                return EMPLACEMENT;
            case "eb_anti_personnel_minefield":
            case "eb_anti_tank_mines":
            case "eb_incendiary_mines":
            case "eb_gas_mine":
                return MINE;
            case "rw_patriot_exosuit":
            case "rw_emancipator_exosuit":
            case "ot_breakthrough_exosuit":
            case "ot_lumberer_exosuit":
                return WALKER;
            case "tesla_tower":
                return SENTRY;
            case "shield_relay":
                return FORTIFICATION;
            case "eg_bastion_mk_xvi":
                // TD-220 is a vehicle, not a fortification, despite the catalog
                // grouping it under DEFENSIVE.
                return SUPPORT;
            case "orbital_laser":
                return ORBITAL_LASER;
            case "br_orbital_gas_strike":
                return ORBITAL_GAS;
            case "br_orbital_ems_strike":
                return ORBITAL_EMS;
            case "br_orbital_smoke_strike":
                return ORBITAL_SMOKE;
            case "orbital_gatling":
            case "orbital_120":
            case "orbital_380":
            case "orbital_walking":
            case "orbital_napalm":
                return BARRAGE;
            case "orbital_airburst":
            case "orbital_railcannon":
            case "br_orbital_precision_strike":
            case "oc_orbital_confetti":
                return ORBITAL;
            default:
                break;
        }

        if (s.hasCategory(StratagemCatalog.SUPPORT)) return SUPPORT;
        if (s.hasCategory(StratagemCatalog.BACKPACK)) return EQUIPMENT;
        if (s.hasCategory(StratagemCatalog.SENTRY)) return SENTRY;
        if (s.hasCategory(StratagemCatalog.DEFENSIVE)) return FORTIFICATION;
        if (s.hasCategory(StratagemCatalog.OFFENSIVE)) {
            return s.id.startsWith("eagle_") ? EAGLE : ORBITAL;
        }
        // Ordinary mission stratagems (SEAF Artillery, Upload Data, flags,
        // drills...) have no dedicated Mission Control call-in in the source
        // packs; use the generic objective line so every completion speaks.
        return OBJECTIVE;
    }
}
