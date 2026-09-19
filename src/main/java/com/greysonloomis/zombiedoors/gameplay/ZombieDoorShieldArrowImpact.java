package com.greysonloomis.zombiedoors.gameplay;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** A blocked arrow's exact position and incoming direction in carried-door space. */
public record ZombieDoorShieldArrowImpact(
	float x,
	float y,
	float directionX,
	float directionY,
	float directionZ
) {
	public static final int MAX_IMPACTS = 12;
	private static final float EDGE_MARGIN = 0.02F;

	public static ZombieDoorShieldArrowImpact fromWorldHit(
		Vec3 hitPosition,
		Vec3 velocity,
		Vec3 zombiePosition,
		float bodyYawDegrees
	) {
		double yaw = Math.toRadians(bodyYawDegrees);
		double rightX = -Math.cos(yaw);
		double rightZ = -Math.sin(yaw);
		double forwardX = -Math.sin(yaw);
		double forwardZ = Math.cos(yaw);
		Vec3 offset = hitPosition.subtract(zombiePosition);

		float localX = (float) (offset.x * rightX + offset.z * rightZ);
		float localDirectionX = (float) (velocity.x * rightX + velocity.z * rightZ);
		float localDirectionZ = (float) -(velocity.x * forwardX + velocity.z * forwardZ);
		float localDirectionY = (float) velocity.y;
		float length = (float) Math.sqrt(
			localDirectionX * localDirectionX
				+ localDirectionY * localDirectionY
				+ localDirectionZ * localDirectionZ
		);
		if (length < 1.0E-4F) {
			localDirectionX = 0.0F;
			localDirectionY = 0.0F;
			localDirectionZ = 1.0F;
		} else {
			localDirectionX /= length;
			localDirectionY /= length;
			localDirectionZ /= length;
		}

		return new ZombieDoorShieldArrowImpact(
			clamp(0.5F + localX, EDGE_MARGIN, 1.0F - EDGE_MARGIN),
			clamp((float) offset.y, EDGE_MARGIN, 2.0F - EDGE_MARGIN),
			localDirectionX,
			localDirectionY,
			localDirectionZ
		);
	}

	public static String append(String encoded, ZombieDoorShieldArrowImpact impact) {
		if (parse(encoded).size() >= MAX_IMPACTS) {
			return encoded;
		}
		String entry = encode(impact, null);
		return encoded == null || encoded.isBlank() ? entry : encoded + ';' + entry;
	}

	public static List<ZombieDoorShieldArrowImpact> decode(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return List.of();
		}
		List<ZombieDoorShieldArrowImpact> impacts = new ArrayList<>();
		for (ParsedImpact parsed : parse(encoded)) {
			impacts.add(parsed.impact());
		}
		return List.copyOf(impacts);
	}

	public static String legacyPlacements(int count) {
		String encoded = "";
		int limited = Math.max(0, Math.min(MAX_IMPACTS, count));
		for (int index = 0; index < limited; index++) {
			float x = 0.18F + (index % 4) * 0.21F;
			float y = 0.25F + (index / 4) * 0.68F + (index % 2) * 0.14F;
			encoded = append(encoded, new ZombieDoorShieldArrowImpact(x, y, 0.0F, 0.0F, 1.0F));
		}
		return encoded;
	}

	public static String normalize(String encoded) {
		String normalized = "";
		for (ZombieDoorShieldArrowImpact impact : decode(encoded)) {
			normalized = append(normalized, impact);
		}
		return normalized;
	}

	private static List<ParsedImpact> parse(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return List.of();
		}
		List<ParsedImpact> impacts = new ArrayList<>();
		for (String entry : encoded.split(";")) {
			if (impacts.size() >= MAX_IMPACTS) {
				break;
			}
			String[] values = entry.split(",");
			if (values.length != 5 && values.length != 6) {
				continue;
			}
			try {
				float x = Float.parseFloat(values[0]);
				float y = Float.parseFloat(values[1]);
				float directionX = Float.parseFloat(values[2]);
				float directionY = Float.parseFloat(values[3]);
				float directionZ = Float.parseFloat(values[4]);
				if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(directionX)
					|| !Float.isFinite(directionY) || !Float.isFinite(directionZ)) {
					continue;
				}
				ZombieDoorShieldArrowImpact impact = new ZombieDoorShieldArrowImpact(
					clamp(x, EDGE_MARGIN, 1.0F - EDGE_MARGIN),
					clamp(y, EDGE_MARGIN, 2.0F - EDGE_MARGIN),
					directionX, directionY, directionZ
				);
				Long embeddedAt = values.length == 6 ? Long.parseLong(values[5]) : null;
				impacts.add(new ParsedImpact(impact, embeddedAt));
			} catch (NumberFormatException ignored) {
				// Skip malformed legacy or externally edited entries.
			}
		}
		return List.copyOf(impacts);
	}

	private static String encode(ZombieDoorShieldArrowImpact impact, Long embeddedAt) {
		String entry = Float.toString(impact.x()) + ','
			+ Float.toString(impact.y()) + ','
			+ Float.toString(impact.directionX()) + ','
			+ Float.toString(impact.directionY()) + ','
			+ Float.toString(impact.directionZ());
		return embeddedAt == null ? entry : entry + ',' + embeddedAt;
	}

	private record ParsedImpact(
		ZombieDoorShieldArrowImpact impact,
		Long embeddedAtGameTime
	) {
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
