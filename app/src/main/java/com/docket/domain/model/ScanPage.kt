package com.docket.domain.model

/**
 * One page of an in-progress scan session. [originalImagePath] always points at the
 * untouched, full-resolution capture — [corners]/[rotationDegrees]/[filter] are edit
 * parameters applied on top of it at render time, never baked into the original. That's what
 * lets crop/rotate/filter stay cheap and instantly undoable while editing.
 *
 * [filter] defaults to [ScanFilter.ENHANCE], not [ScanFilter.ORIGINAL] — a fresh capture should
 * already look like a clean scan (shadow removed, contrast stretched) without the user having to
 * find and tap a filter chip first. Original stays one tap away for anyone who wants the
 * untouched photo. Kept color rather than defaulting to Black & White since Enhance is the
 * reversible, information-preserving choice — a user photographing a colored receipt or an ID
 * card shouldn't lose that color as an opt-out default.
 */
data class ScanPage(
    val id: String,
    val originalImagePath: String,
    val corners: Quad = Quad.FullFrame,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.ENHANCE,
    val orderIndex: Int
)
