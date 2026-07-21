package com.egalvanic.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Gold catalog of the Work Order "Work Type" dropdown — live-verified against
 * the backend on 2026-07-21 (docs/worktype-gold-spec-2026-07-21.md).
 *
 * Source of truth: GET /api/procedures-v2/services returns EXACTLY the 13
 * service-backed entries below; the create-WO dropdown shows them plus a
 * UI-only "General" option that persists work_type_id = null.
 *
 * Service ids are deterministic UUIDv5 values (stable across environments and
 * syncs — version nibble '5'), so they are safe to pin here. procedure_count
 * is a point-in-time observation: form/task generation is asynchronous AND
 * class-conditional per site, so tests may assert id/key/name/category but
 * must NEVER hard-assert procedure/form/task counts or % progress.
 */
public enum WorkTypeCatalog {

    // name                          display name                        key                              service id                                cat                    deEnergized
    ARC_FLASH_DATA_COLLECTION("Arc Flash Data Collection",      "arc-flash-study",                "d625cfa0-5447-52c5-858e-9ecd5c84d0fb", Category.AF,        false),
    ARC_FLASH_LABEL_PLACEMENT("Arc Flash Label Placement",      "arc-flash-label-placement",      "9de69871-ad71-56f4-8f04-515b5738770b", Category.CHECKLIST, false),
    CLEANING(                 "Cleaning",                        "cleaning",                       "180c4243-25df-581c-895a-9e883f38948f", Category.PM_FORMS,  true),
    CLEAN_TIGHTEN_TORQUE(     "Clean, Tighten, Torque",          "clean-tighten-torque",           "8e578df1-2b96-5733-8f0e-c00fef0a92b8", Category.PM_FORMS,  true),
    CONDITION_ASSESSMENT(     "Condition Assessment",            "condition-assessment",           "173c2ca2-8e86-5c95-9b1f-0724ddaccd8b", Category.COM,       false),
    DE_ENERGIZED_VISUAL(      "De-Energized Visual Inspection",  "de-energized-visual-inspection", "01ad81ff-63fe-507e-beb0-305d7f67dad9", Category.PM_FORMS,  true),
    DGA_FLUID_SAMPLE(         "DGA / Fluid Sample Analysis",     "dga-fluid-sample-analysis",      "5dff8199-3579-56c6-b81d-dc4e9b4dcd3d", Category.PM_FORMS,  false),
    INFRARED_THERMOGRAPHY(    "Infrared Thermography",           "infrared-thermography",          "3b732d14-461c-54a7-8e30-70391bd34dd6", Category.IR,        false),
    INSULATION_RESISTANCE(    "Insulation Resistance Testing",   "insulation-resistance-testing",  "d9c9efef-914f-5656-b510-e156bd07ba63", Category.PM_FORMS,  true),
    /** Display name "NETA Testing" but backend key is de-energized-testing — key ≠ name slug. */
    NETA_TESTING(             "NETA Testing",                    "de-energized-testing",           "0d914f81-a750-5833-8c46-5c71064f676e", Category.PM_FORMS,  true),
    PANEL_SCHEDULE_UPDATES(   "Panel Schedule Updates",          "panel-schedule-updates",         "3f92b954-8d88-5045-83a8-ee7d9ace504d", Category.SCHEDULE,  false),
    SHUTDOWN_COMPOSITE(       "Shutdown (Composite)",            "composite-shutdown-emp",         "f9fb8d4a-2ccd-5bdb-baac-278dc4dc6cfb", Category.PM_FORMS,  true),
    UPS_MAINTENANCE(          "UPS Maintenance",                 "ups-maintenance",                "8c5cf34c-ed04-5c5e-9bff-973410762b13", Category.PM_FORMS,  false),
    /** UI-only 14th dropdown option: no backing service, work_type_id = null. */
    GENERAL(                  "General",                          null,                             null,                                   Category.NONE,      false);

    /**
     * Workflow category (`type` field of the service). Drives which tabs /
     * columns the WO detail exposes (web contract in the gold-spec doc §5;
     * iOS surface probed the same day).
     */
    public enum Category {
        /** Arc-flash data collection: SLD + Equipment Designations, Arc Flash column. */
        AF,
        /** Checklist types: Tasks tab + Tasks column. */
        CHECKLIST,
        /** Condition assessment: Condition Assessment tab, Tasks + C.O.M. columns. */
        COM,
        /** Infrared thermography: IR Photos tab + column (the classic session view). */
        IR,
        /** Panel schedules: Panel Schedules tab, Schedule column. */
        SCHEDULE,
        /** Procedure-driven PM forms: Forms tab + column, % progress once generated. */
        PM_FORMS,
        /** "General" / legacy null work type: superset view (Tasks + Forms + IR Photos). */
        NONE
    }

    private final String displayName;
    private final String key;
    private final String serviceId;
    private final Category category;
    private final boolean deEnergized;

    WorkTypeCatalog(String displayName, String key, String serviceId,
                    Category category, boolean deEnergized) {
        this.displayName = displayName;
        this.key = key;
        this.serviceId = serviceId;
        this.category = category;
        this.deEnergized = deEnergized;
    }

    public String displayName() { return displayName; }
    /** Backend service key (slug); null for GENERAL. */
    public String key() { return key; }
    /** Deterministic UUIDv5 service id; null for GENERAL. */
    public String serviceId() { return serviceId; }
    public Category category() { return category; }
    public boolean deEnergized() { return deEnergized; }
    public boolean isServiceBacked() { return serviceId != null; }

    /** Ordinal used in the fixture family name, e.g. 8 -> "QA-WT08 …". */
    public int fixtureNumber() {
        return this == GENERAL ? 0 : ordinal() + 1;
    }

    /**
     * Name of this type's durable live fixture WO on Android Qa Site1
     * (created 2026-07-21; see gold-spec §3). Punctuation ("/", ",", "()")
     * is intentionally stripped so NS-predicate matching stays trivial.
     */
    public String fixtureName() {
        String cleaned = displayName
                .replace(" / ", " ").replace("/", " ")
                .replace(",", "").replace("(", "").replace(")", "")
                .replaceAll("\\s+", " ").trim();
        return String.format("QA-WT%02d %s", fixtureNumber(), cleaned);
    }

    /** The 13 service-backed types (everything except GENERAL). */
    public static List<WorkTypeCatalog> serviceBacked() {
        List<WorkTypeCatalog> out = new ArrayList<>(Arrays.asList(values()));
        out.remove(GENERAL);
        return Collections.unmodifiableList(out);
    }

    /** Types whose category is {@code c}. */
    public static List<WorkTypeCatalog> ofCategory(Category c) {
        List<WorkTypeCatalog> out = new ArrayList<>();
        for (WorkTypeCatalog wt : values()) if (wt.category == c) out.add(wt);
        return out;
    }

    /** Catalog entry whose backend key is {@code key}, or null. */
    public static WorkTypeCatalog byKey(String key) {
        if (key == null) return null;
        for (WorkTypeCatalog wt : values()) if (key.equals(wt.key)) return wt;
        return null;
    }

    /** Catalog entry whose display name is {@code name} (exact), or null. */
    public static WorkTypeCatalog byDisplayName(String name) {
        if (name == null) return null;
        for (WorkTypeCatalog wt : values()) if (wt.displayName.equals(name)) return wt;
        return null;
    }

    /** Number of service-backed types expected from the backend catalog. */
    public static final int SERVICE_COUNT = 13;
    /** Expected de-energized service-backed type count (gold spec). */
    public static final int DE_ENERGIZED_COUNT = 6;
}
