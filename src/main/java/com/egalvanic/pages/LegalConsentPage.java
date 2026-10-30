package com.egalvanic.pages;

import java.util.Optional;

/**
 * ZP-3914 (iOS 1.59) "Terms of Use V2" / "Updated Terms & Conditions screen" — the sign-in consent line and the
 * legal documents it links to (AppStrings.Legal: 'By signing in, you agree to our' · 'Terms & Conditions' · 'and' ·
 * 'Privacy Policy').
 *
 * Why not LoginPage.tapPrivacyPolicyLink(): its predicate ({@code label CONTAINS 'Privacy Policy'}) also matches the
 * WHOLE consent sentence, and element.click() then lands mid-sentence — the document never opened (TC_TOU_03
 * retest 2026-10-21). Here the link is matched EXACTLY (Link elements first) and pressed by coordinates, and the
 * document's Done/Close is looked for in the top bar only, never the keyboard's 'done' key.
 */
public class LegalConsentPage extends SnapPage {

    public static final String CONSENT_PREFIX = "By signing in, you agree to our";
    public static final String TERMS = "Terms & Conditions";
    public static final String PRIVACY = "Privacy Policy";

    /** The consent sentence (or its first fragment) is on screen. */
    public boolean isConsentShown() {
        return find(n -> n.text().startsWith(CONSENT_PREFIX)).isPresent();
    }

    /** Exact-label link: a Link element first, then Button / StaticText whose whole text is the link text. */
    private Optional<SnapNode> link(String text) {
        Optional<SnapNode> l = find(n -> "Link".equals(n.type) && n.is(text));
        if (l.isPresent()) return l;
        return find(n -> ("Button".equals(n.type) || "StaticText".equals(n.type)) && n.is(text));
    }

    public boolean isLinkShown(String text) {
        return link(text).isPresent();
    }

    /** Diagnostics for a failed link check: every node mentioning the consent, Terms or Privacy, with its type. */
    public java.util.List<String> consentNodes() {
        return findAll(n -> n.text().contains(CONSENT_PREFIX) || n.text().contains(TERMS) || n.text().contains(PRIVACY))
                .stream().map(n -> n.type + " '" + n.text() + "'").toList();
    }

    /** Press the link; true once a document is up. */
    public boolean openDocument(String linkText) {
        if (isKeyboardUp()) dismissKeyboard();
        Optional<SnapNode> l = settled(n -> ("Link".equals(n.type) || "Button".equals(n.type) || "StaticText".equals(n.type))
                && n.is(linkText));
        if (l.isEmpty()) {
            System.out.println("⚠️ LegalConsentPage: no exact '" + linkText + "' link on screen");
            return false;
        }
        tapNode(l.get());
        return waitUntil(this::isDocumentShown, 15_000);
    }

    private Optional<SnapNode> topBarClose() {
        return find(n -> "Button".equals(n.type) && n.y < 200 && (n.is("Done") || n.is("Close")));
    }

    /** A legal document is showing: a web view, or a sheet with a top-bar Done/Close and no consent line. */
    public boolean isDocumentShown() {
        boolean web = find(n -> "WebView".equals(n.type)).isPresent();
        return web || (topBarClose().isPresent() && !isConsentShown());
    }

    /** Close the document; true once the consent line is back. */
    public boolean closeDocument() {
        Optional<SnapNode> c = topBarClose();
        if (c.isPresent()) {
            tapNode(c.get());
        } else {
            swipeScreen("down");
        }
        return waitUntil(() -> !isDocumentShown() && isConsentShown(), 8_000);
    }
}
