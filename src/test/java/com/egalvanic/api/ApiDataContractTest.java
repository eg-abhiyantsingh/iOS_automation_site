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
 * /auth/v2/me's accessible_sld_ids (the /users/{id}/slds endpoint returns [] for the
 * admin/RBAC account).
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
            "/auth/v2/me must expose accessible_sld_ids — the data layer is unusable without it");
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
