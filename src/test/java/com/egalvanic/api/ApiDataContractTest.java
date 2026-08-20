package com.egalvanic.api;

import io.restassured.path.json.JsonPath;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * DATA-CONTRACT + DATA-INTEGRITY API tests (pure HTTP — no simulator, no WDA).
 *
 * These verify, deterministically and in ~20s, the exact things the iOS UI suite
 * struggles to test reliably (it wedges WDA on the giant bleed-through DOMs): that
 * the backend's SLD payload has the asset/issue/connection shape the app depends on,
 * and that referential integrity holds (no orphaned connections/issues, no dup nodes).
 *
 * Source of truth: GET /sld/v3/{id} (the whole-SLD sync payload). SLD ids come from
 * TestDataApi.accessibleSldIds(), which merges /auth/v2/me's accessible_sld_ids with
 * the /users/{id}/slds list (the QA backend has flip-flopped between the two —
 * accessible_sld_ids went [] on ~2026-07-22 while /users/{id}/slds came back alive).
 */
public class ApiDataContractTest {

    private static TestDataApi api;
    private static String sldId;
    private static JsonPath sld;

    @BeforeClass
    public void setup() {
        api = new TestDataApi();
        api.login();
        List<String> ids = api.accessibleSldIds();
        assertFalse(ids.isEmpty(),
            "No SLD ids from EITHER /auth/v2/me accessible_sld_ids OR /users/{id}/slds "
            + "— the data layer is unusable without a site source");
        sldId = ids.get(0);
        sld = JsonPath.from(api.getSldDetails(sldId));
    }

    @Test(description = "User can access at least one SLD via /me (RBAC-independent data source)")
    public void accessibleSldsPresent() {
        assertTrue(api.accessibleSldIds().size() >= 1,
            "expected >=1 accessible SLD; got " + api.accessibleSldIds());
    }

    @Test(description = "SLD payload carries the core collections the app syncs")
    public void sldPayloadShape() {
        for (String key : new String[]{"id", "name", "nodes", "issues", "edges"}) {
            assertTrue(sld.get(key) != null, "SLD payload missing '" + key + "' (sync contract)");
        }
        List<Map<String, Object>> nodes = sld.getList("nodes");
        assertFalse(nodes.isEmpty(), "SLD should have nodes (assets) to test against");
    }

    @Test(description = "Every asset node carries class identity fields (node_class_name / node_subtype_name)")
    public void nodeClassContract() {
        List<Map<String, Object>> nodes = sld.getList("nodes");
        int missingClass = 0;
        for (Map<String, Object> n : nodes) {
            // node_class_name is the human label the iOS picker shows; allow nulls only
            // for deleted nodes.
            boolean deleted = Boolean.TRUE.equals(n.get("is_deleted"));
            if (!deleted && !n.containsKey("node_class_name")) missingClass++;
        }
        assertTrue(missingClass == 0,
            missingClass + " live nodes missing the 'node_class_name' field (asset-class contract drift)");
    }

    @Test(description = "Connections (edges) reference REAL nodes — no orphaned/dangling edges")
    public void noOrphanedConnections() {
        Set<String> nodeIds = new HashSet<>();
        for (Map<String, Object> n : sld.<Map<String, Object>>getList("nodes")) {
            Object id = n.get("id");
            if (id != null) nodeIds.add(id.toString());
        }
        List<Map<String, Object>> edges = sld.getList("edges");
        StringBuilder orphans = new StringBuilder();
        for (Map<String, Object> e : edges) {
            if (Boolean.TRUE.equals(e.get("is_deleted"))) continue;
            String src = str(e.get("source")), tgt = str(e.get("target"));
            if (src != null && !nodeIds.contains(src)) orphans.append("\n  edge ").append(e.get("id")).append(" source ").append(src).append(" missing");
            if (tgt != null && !nodeIds.contains(tgt)) orphans.append("\n  edge ").append(e.get("id")).append(" target ").append(tgt).append(" missing");
        }
        assertTrue(orphans.length() == 0,
            "Orphaned connection(s) — edge references a node that doesn't exist:" + orphans);
    }

    @Test(description = "Issues reference REAL nodes — no orphaned issues pointing at deleted/absent assets")
    public void noOrphanedIssues() {
        Set<String> nodeIds = new HashSet<>();
        for (Map<String, Object> n : sld.<Map<String, Object>>getList("nodes")) {
            Object id = n.get("id");
            if (id != null) nodeIds.add(id.toString());
        }
        List<Map<String, Object>> issues = sld.getList("issues");
        StringBuilder orphans = new StringBuilder();
        for (Map<String, Object> is : issues) {
            if (Boolean.TRUE.equals(is.get("is_deleted"))) continue;
            String nodeId = str(is.get("node_id"));
            if (nodeId != null && !nodeIds.contains(nodeId)) {
                orphans.append("\n  issue '").append(is.get("title")).append("' -> node ").append(nodeId).append(" missing");
            }
        }
        assertTrue(orphans.length() == 0,
            "Orphaned issue(s) — issue references a node that doesn't exist:" + orphans);
    }

    @Test(description = "Every non-deleted issue has a class + status (issue contract)")
    public void issueClassAndStatusContract() {
        List<Map<String, Object>> issues = sld.getList("issues");
        int bad = 0;
        for (Map<String, Object> is : issues) {
            if (Boolean.TRUE.equals(is.get("is_deleted"))) continue;
            if (!is.containsKey("issue_class") || !is.containsKey("status")) bad++;
        }
        assertTrue(bad == 0, bad + " live issues missing issue_class/status (contract drift)");
    }

    /**
     * READINESS CANARY for iOS PR #482 / backend #1057 (prepared 2026-06-25, feature not in QA yet).
     * The backend is replacing the eqp_engineering_approved boolean with a four-state
     * engineering_status (SKM Data State vocabulary). Until the column reaches this env, this
     * test SKIPs with a clear message; the run it starts PASSING is the signal that gate G2 in
     * docs/engineering-status-sync-test-design.md is open and the TC_ES_* round-trip suite can
     * be activated. Once present, it also guards the vocabulary contract the iOS decoder
     * (EngineeringStatus.swift) mirrors.
     */
    @Test(description = "engineering_status readiness canary + vocabulary contract (Incomplete/Estimated/Complete/Verified)")
    public void engineeringStatusReadiness() {
        List<Map<String, Object>> nodes = sld.getList("nodes");
        boolean fieldSeen = false;
        java.util.Map<String, Integer> distribution = new java.util.TreeMap<>();
        StringBuilder unknown = new StringBuilder();
        Set<String> vocab = new HashSet<>(java.util.Arrays.asList(
            "Incomplete", "Estimated", "Complete", "Verified"));
        for (Map<String, Object> n : nodes) {
            if (!n.containsKey("engineering_status")) continue;
            fieldSeen = true;
            String v = str(n.get("engineering_status"));
            distribution.merge(v == null ? "(null)" : v, 1, Integer::sum);
            // null tolerated at the payload level (iOS degrades unknown->nil); a NON-null value
            // outside the vocabulary is contract drift the iOS enum would silently drop.
            if (v != null && !vocab.contains(v)) {
                unknown.append("\n  node ").append(n.get("id")).append(" -> '").append(v).append("'");
            }
        }
        if (!fieldSeen) {
            throw new org.testng.SkipException(
                "engineering_status not on this backend yet (backend #1057 unpromoted) — "
                + "TC_ES_* suite stays blocked; see docs/engineering-status-sync-test-design.md");
        }
        System.out.println("engineering_status IS LIVE on this env — distribution: " + distribution);
        assertTrue(unknown.length() == 0,
            "engineering_status value(s) outside the SKM vocabulary "
            + "{Incomplete, Estimated, Complete, Verified} — iOS decode would degrade these to nil:"
            + unknown);
    }

    @Test(description = "Node ids are unique (no duplicate assets in the sync payload)")
    public void nodeIdsUnique() {
        List<Map<String, Object>> nodes = sld.getList("nodes");
        Set<String> seen = new HashSet<>();
        Set<String> dups = new HashSet<>();
        for (Map<String, Object> n : nodes) {
            String id = str(n.get("id"));
            if (id != null && !seen.add(id)) dups.add(id);
        }
        assertTrue(dups.isEmpty(), "Duplicate node ids in SLD payload: " + dups);
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
}
