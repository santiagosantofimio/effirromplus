package dev.santiagosantofimio.effirromplus.villager;

import java.util.function.DoubleSupplier;

public final class PriceDistribution {
	private static final double CONCENTRATED = 1000.0;

	private final int min;
	private final int max;
	private final double exponent;

	public PriceDistribution(int min, int max, int average) {
		this.min = min;
		this.max = Math.max(min, max);
		int span = this.max - min;
		int offset = Math.clamp(average, min, this.max) - min;
		this.exponent = span == 0 ? 1.0 : offset == 0 ? CONCENTRATED : Math.max(0.0, (double) span / offset - 1.0);
	}

	public double exponent() {
		return exponent;
	}

	public int sample(DoubleSupplier uniform) {
		double x = Math.clamp(uniform.getAsDouble(), 0.0, 1.0);
		return min + (int) Math.round((max - min) * Math.pow(x, exponent));
	}
}
