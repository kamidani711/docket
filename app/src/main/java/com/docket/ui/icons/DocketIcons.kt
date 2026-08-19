package com.docket.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Docket's own outline icon set — ported directly from the exact SVG path data embedded in
 * `design_handoff_docket_ui/Docket App.dc.html`'s `icon()` method (24x24 viewBox, 1.8 stroke,
 * round cap/join), rather than approximated with a different icon library. The handoff's
 * "Iconsax/Feather-style linear" set doesn't exist as a dependency; this is pixel-accurate to
 * the reference instead of a stand-in.
 *
 * Colored via the same convention as `Icons.Filled.*` everywhere else in the app: pass `tint`
 * at the `Icon(imageVector = ..., tint = ...)` call site. [home], [folder], [star] and [user]
 * additionally take a `filled` flag (solid fill for the active bottom-tab state) — the only
 * four icons the handoff ever shows in a filled variant.
 */
object DocketIcons {

    // ---- Primitive-to-path helpers: circle/rounded-rect expressed as SVG path data so every
    // shape (not just the hand-authored curves) flows through the same PathParser pipeline. ----
    private fun circlePath(cx: Float, cy: Float, r: Float): String =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun rectPath(x: Float, y: Float, w: Float, h: Float, r: Float = 0f): String = if (r <= 0f) {
        "M$x,$y H${x + w} V${y + h} H$x Z"
    } else {
        "M${x + r},$y H${x + w - r} A$r,$r 0 0 1 ${x + w},${y + r} V${y + h - r} " +
            "A$r,$r 0 0 1 ${x + w - r},${y + h} H${x + r} A$r,$r 0 0 1 $x,${y + h - r} " +
            "V${y + r} A$r,$r 0 0 1 ${x + r},$y Z"
    }

    private fun build(name: String, segments: List<Pair<String, Boolean>>): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            segments.forEach { (d, filled) ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = if (filled) 0f else 1.8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                )
            }
        }.build()

    /** One stroke-only path, the common case for most icons below. */
    private fun outline(name: String, vararg d: String): ImageVector =
        build(name, d.map { it to false })

    // ---- Tab icons (filled variant for the active state) ----
    private val homeD = "M4 10.6 12 4.2l8 6.4V19a1.6 1.6 0 0 1-1.6 1.6h-3.1v-5.3H8.7v5.3H5.6A1.6 1.6 0 0 1 4 19z"
    private val folderD = "M3.4 7.6a2 2 0 0 1 2-2h3.3l1.5 2h8.4a2 2 0 0 1 2 2v7.2a2 2 0 0 1-2 2H5.4a2 2 0 0 1-2-2z"
    private val starD = "M12 3.8l2.6 5.2 5.8.8-4.2 4.1 1 5.7-5.2-2.8-5.2 2.8 1-5.7-4.2-4.1 5.8-.8z"
    private val userBodyD = "M5.6 20.2c.7-3.6 3.3-5.5 6.4-5.5s5.7 1.9 6.4 5.5"

    fun home(filled: Boolean = false): ImageVector = build("DocketHome-$filled", listOf(homeD to filled))
    fun folder(filled: Boolean = false): ImageVector = build("DocketFolder-$filled", listOf(folderD to filled))
    fun star(filled: Boolean = false): ImageVector = build("DocketStar-$filled", listOf(starD to filled))
    fun user(filled: Boolean = false): ImageVector = build(
        "DocketUser-$filled",
        listOf(circlePath(12f, 8.4f, 3.4f) to filled, userBodyD to false)
    )

    // ---- Everything else: outline-only, built once. ----
    val Search: ImageVector = build("DocketSearch", listOf(circlePath(11f, 11f, 6.6f) to false, "M16.6 16.6 21 21" to false))
    val Camera: ImageVector = build(
        "DocketCamera",
        listOf(
            "M4 8.6a2 2 0 0 1 2-2h1.9l1.2-1.8h5.8l1.2 1.8H19a2 2 0 0 1 2 2v8.8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" to false,
            circlePath(12f, 12.6f, 3.4f) to false
        )
    )
    val Image: ImageVector = build(
        "DocketImage",
        listOf(
            "M4 6.6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10.8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" to false,
            circlePath(12f, 10f, 1.8f) to false,
            "M4.6 17 9.4 12l3.4 3.2 2.4-2 4.2 4" to false
        )
    )
    val Doc: ImageVector = outline(
        "DocketDoc",
        "M6.2 4.4h7.4l4.2 4.2v11a1 1 0 0 1-1 1H7.2a1 1 0 0 1-1-1z",
        "M9 12.4h6M9 15.8h4"
    )
    val Shield: ImageVector = outline(
        "DocketShield",
        "M12 3.6l7 2.6v5.6c0 4-2.9 6.6-7 8.4-4.1-1.8-7-4.4-7-8.4V6.2z"
    )
    val ShieldCheck: ImageVector = outline(
        "DocketShieldCheck",
        "M12 3.6l7 2.6v5.6c0 4-2.9 6.6-7 8.4-4.1-1.8-7-4.4-7-8.4V6.2z",
        "M9.2 11.8 11.4 14l3.6-3.8"
    )
    val Gear: ImageVector = build(
        "DocketGear",
        listOf(
            "M12 3.6l1.4 2 2.4-.5.6 2.4 2.1 1.3-1.2 2.1 1.2 2.1-2.1 1.3-.6 2.4-2.4-.5L12 20.4l-1.4-2-2.4.5-.6-2.4L5.5 15l1.2-2.1L5.5 10.8l2.1-1.3.6-2.4 2.4.5z" to false,
            circlePath(12f, 12f, 2.8f) to false
        )
    )
    val Eye: ImageVector = build(
        "DocketEye",
        listOf(
            "M2.8 12S6 6.6 12 6.6 21.2 12 21.2 12 18 17.4 12 17.4 2.8 12 2.8 12z" to false,
            circlePath(12f, 12f, 2.8f) to false
        )
    )
    val Logout: ImageVector = outline(
        "DocketLogout",
        "M14.4 4.6H7.2a1.4 1.4 0 0 0-1.4 1.4v12a1.4 1.4 0 0 0 1.4 1.4h7.2",
        "M17.4 8.6 20.8 12l-3.4 3.4M20.4 12h-8"
    )
    val Info: ImageVector = build(
        "DocketInfo",
        listOf(rectPath(4.6f, 4.6f, 14.8f, 14.8f, 4f) to false, "M12 10.6v5.2M12 8.4v.1" to false)
    )
    val Sort: ImageVector = outline("DocketSort", "M7 4.6v14.8M7 19.4 4.2 16.4M17 19.4V4.6M17 4.6l2.8 3")
    val FolderPlus: ImageVector = outline("DocketFolderPlus", folderD)
    val ArrowRight: ImageVector = outline("DocketArrowRight", "M4.6 12h14M13.8 6.4 19.4 12l-5.6 5.6")
    val Back: ImageVector = outline("DocketBack", "M19.4 12H5.4M11.2 5.6 4.8 12l6.4 6.4")
    val Chevron: ImageVector = outline("DocketChevron", "M9.6 5.6 16 12l-6.4 6.4")
    val Check: ImageVector = outline("DocketCheck", "M5.6 12.4 9.8 16.6 18.4 8")
    val Crop: ImageVector = outline(
        "DocketCrop",
        "M6.4 3.6v12.6a1.4 1.4 0 0 0 1.4 1.4h12.6M17.6 20.4V7.8a1.4 1.4 0 0 0-1.4-1.4H3.6"
    )
    val Rotate: ImageVector = outline("DocketRotate", "M20 11.4a8 8 0 1 1-2.5-5.8M20 4v4.2h-4.2")
    val Retake: ImageVector = outline("DocketRetake", "M4 12.6a8 8 0 1 0 2.5-5.8M4 5v4.2h4.2")
    val Frame: ImageVector = outline(
        "DocketFrame",
        "M4 8.6V5.4a1.4 1.4 0 0 1 1.4-1.4h3.2M20 8.6V5.4A1.4 1.4 0 0 0 18.6 4h-3.2M4 15.4v3.2A1.4 1.4 0 0 0 5.4 20h3.2M20 15.4v3.2A1.4 1.4 0 0 1 18.6 20h-3.2"
    )
    val Spark: ImageVector = outline("DocketSpark", "M12 3.6l1.9 4.9 4.9 1.9-4.9 1.9L12 17.2l-1.9-4.9-4.9-1.9 4.9-1.9z")
    val Flash: ImageVector = outline("DocketFlash", "M13.4 3.4 6.6 13.2h4.2l-.8 7.4 7-9.9h-4.2z")
    val Csv: ImageVector = outline("DocketCsv", "M5.4 5.4h13.2v13.2H5.4zM5.4 10.2h13.2M10.2 10.2v8.4")
    val Receipt: ImageVector = outline("DocketReceipt", "M6.6 3.8h10.8v16.4l-2.7-1.8-2.7 1.8-2.7-1.8-2.7 1.8z")
    val IdCard: ImageVector = build(
        "DocketIdCard",
        listOf(
            rectPath(3.4f, 6.6f, 17.2f, 10.8f, 2.6f) to false,
            circlePath(8.6f, 12f, 2.2f) to false,
            "M13.6 10.4h4.2M13.6 13.6h3" to false
        )
    )
    val Pdf: ImageVector = outline("DocketPdf", "M7 3.8h6.6l4 4V20a.9.9 0 0 1-.9.9H7a.9.9 0 0 1-.9-.9V4.7A.9.9 0 0 1 7 3.8z", "M13.2 3.9V8h4.2")
    val Grid: ImageVector = build(
        "DocketGrid",
        listOf("M4.6 4.6h5.6v5.6H4.6zM13.8 4.6h5.6v5.6h-5.6zM4.6 13.8h5.6v5.6H4.6zM13.8 13.8h5.6v5.6h-5.6z" to true)
    )
    val Share: ImageVector = build(
        "DocketShare",
        listOf(
            circlePath(17.4f, 5.6f, 2.4f) to false,
            circlePath(6.6f, 12f, 2.4f) to false,
            circlePath(17.4f, 18.4f, 2.4f) to false,
            "M8.6 13.4 15.4 17M15.4 7 8.6 10.6" to false
        )
    )
    val Dots: ImageVector = build(
        "DocketDots",
        listOf(12f, 12f, 12f).zip(listOf(5.4f, 12f, 18.6f)).map { (cx, cy) -> circlePath(cx, cy, 1.6f) to true }
    )
    val DotsCircle: ImageVector = build(
        "DocketDotsCircle",
        listOf(circlePath(12f, 12f, 8.4f) to false) +
            listOf(8.2f, 12f, 15.8f).map { cx -> circlePath(cx, 12f, 1.2f) to true }
    )
}
