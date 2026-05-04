package com.tnhzr.ihs.disease;

public final class TransmissionSettings {
    public final double radius;
    public final double chanceRadius;
    public final double chanceDirect;

    public TransmissionSettings(double radius, double chanceRadius, double chanceDirect) {
        this.radius = radius;
        this.chanceRadius = chanceRadius;
        this.chanceDirect = chanceDirect;
    }
}
