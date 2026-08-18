package com.docket.domain.model

/**
 * Four crop corners, normalized to [0,1] against the *original, unrotated* image's width/height
 * so they stay valid regardless of what size we happen to decode the image at for preview vs.
 * export. Order is top-left, top-right, bottom-right, bottom-left — matters for
 * [com.docket.domain.repository.ImageProcessor], which maps this quad onto a rectangle.
 */
data class NormalizedPoint(val x: Float, val y: Float)

data class Quad(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint
) {
    companion object {
        /** Full-frame, uncropped — ML Kit's own auto-crop already deskewed the source page. */
        val FullFrame = Quad(
            topLeft = NormalizedPoint(0f, 0f),
            topRight = NormalizedPoint(1f, 0f),
            bottomRight = NormalizedPoint(1f, 1f),
            bottomLeft = NormalizedPoint(0f, 1f)
        )
    }
}
