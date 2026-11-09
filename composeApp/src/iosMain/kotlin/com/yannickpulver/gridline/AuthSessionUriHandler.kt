package com.yannickpulver.gridline

import androidx.compose.ui.platform.UriHandler
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject

/**
 * Opens links (the Instagram login) in Apple's login sheet: it stays out of the Instagram app and
 * closes itself when the login ends at gridline://login. The token is picked up by polling.
 */
class AuthSessionUriHandler : UriHandler {
    private var session: ASWebAuthenticationSession? = null

    private val anchor = object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
        override fun presentationAnchorForWebAuthenticationSession(
            session: ASWebAuthenticationSession,
        ): ASPresentationAnchor = UIApplication.sharedApplication.keyWindow ?: UIWindow()
    }

    override fun openUri(uri: String) {
        val url = NSURL.URLWithString(uri) ?: return
        session?.cancel()
        session = ASWebAuthenticationSession(url, "gridline") { _, _ -> session = null }.apply {
            presentationContextProvider = anchor
            start()
        }
    }
}
