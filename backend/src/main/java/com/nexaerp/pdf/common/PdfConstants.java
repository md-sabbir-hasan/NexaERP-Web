package com.nexaerp.pdf.common;

import com.itextpdf.kernel.colors.DeviceRgb;

public final class PdfConstants {

    private PdfConstants() {}

    public static final DeviceRgb PRIMARY = new DeviceRgb(37, 99, 235);
    public static final DeviceRgb DARK = new DeviceRgb(15, 23, 42);
    public static final DeviceRgb MUTED = new DeviceRgb(100, 116, 139);
    public static final DeviceRgb LIGHT = new DeviceRgb(248, 250, 252);
    public static final DeviceRgb BORDER = new DeviceRgb(226, 232, 240);
    public static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);

    public static final float PAGE_MARGIN = 36f;
}