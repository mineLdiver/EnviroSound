/*******************************************************************************
 *                     GNU GENERAL PUBLIC LICENSE
 *                        Version 3, 29 June 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 *
 *  https://github.com/sonicether/Sound-Physics/blob/master/LICENSE
 *
 * Copyright (c) 2017, 2019 AlkCorp.
 * Contributors: https://github.com/alkcorp/Sound-Physics/graphs/contributors
 *******************************************************************************/
package com.sonicether.soundphysics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;
import org.lwjgl.openal.EFX10;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;

@Mod(modid = SoundPhysics.modid, acceptedMinecraftVersions = SoundPhysics.mcVersion, version = SoundPhysics.version, guiFactory = "com.sonicether.soundphysics.SPGuiFactory", dependencies = "before:computronics") // Deps
public class SoundPhysics {

	public static class ProcThread extends Thread {
		@Override
		public synchronized void run() {
			while (SoundPhysics.thread_alive) {
				while (!Config.dynamicEnvironementEvalutaion) {
					try {
						Thread.sleep(1000);
					}
					catch (final Exception e) {
						SoundPhysics.logError(String.valueOf(e));
					}
				}
				synchronized (SoundPhysics.source_list) {
					// log("Updating env " +
					// String.valueOf(source_list.size()));
					final ListIterator<Source> iter = SoundPhysics.source_list.listIterator();
					while (iter.hasNext()) {
						final Source source = iter.next();
						// log("Updating sound '" + source.name + "' SourceID:"
						// + String.valueOf(source.sourceID));
						// boolean pl = sndHandler.isSoundPlaying(source.sound);
						// FloatBuffer pos = BufferUtils.createFloatBuffer(3);
						// AL10.alGetSource(source.sourceID,AL10.AL_POSITION,pos);
						// To try ^
						final int state = AL10.alGetSourcei(
								source.sourceID, AL10.AL_SOURCE_STATE
								);
						// int byteoff = AL10.alGetSourcei(source.sourceID,
						// AL11.AL_BYTE_OFFSET);
						// boolean finished = source.size == byteoff;
						if (state == AL10.AL_PLAYING) {
							final FloatBuffer pos = BufferUtils.createFloatBuffer(
									3
									);
							AL10.alGetSource(
									source.sourceID, AL10.AL_POSITION, pos
									);
							source.posX = pos.get(0);
							source.posY = pos.get(1);
							source.posZ = pos.get(2);
							SoundPhysics.evaluateEnvironment(
									source.sourceID, source.posX, source.posY, source.posZ, source.category, source.name
									);
						}
						else /*if (state == AL10.AL_STOPPED)*/ {
							iter.remove();
						}
					}
				}
				try {
					Thread.sleep(
							1000 / Config.dynamicEnvironementEvalutaionFrequency
							);
				}
				catch (final Exception e) {
					SoundPhysics.logError(String.valueOf(e));
				}
			}
		}
	}
	public static class Source {
		public int bufferID;
		public SoundCategory category;
		public int frequency;
		public String name;
		public float posX;
		public float posY;
		public float posZ;
		public int size;
		public int sourceID;

		public Source(
				final int sid, final float px, final float py, final float pz, final SoundCategory cat, final String n
				) {
			this.sourceID = sid;
			this.posX = px;
			this.posY = py;
			this.posZ = pz;
			this.category = cat;
			this.name = n;
			this.bufferID = AL10.alGetSourcei(sid, AL10.AL_BUFFER);
			this.size = AL10.alGetBufferi(this.bufferID, AL10.AL_SIZE);
			this.frequency = AL10.alGetBufferi(
					this.bufferID, AL10.AL_FREQUENCY
					);
		}
	}
	// THESE VARIABLES ARE CONSTANTLY ACCESSED AND USED BY ASM INJECTED CODE! DO
	// NOT REMOVE!
	public static int attenuationModel = SoundSystemConfig.ATTENUATION_ROLLOFF;

	private static int auxFXSlot0;
	private static int auxFXSlot1;
	private static int auxFXSlot2;
	private static int auxFXSlot3;
	private static final Pattern blockPattern = Pattern.compile(".*block.*");
	private static final Pattern clickPattern = Pattern.compile(
			".*random.click.*"
			);

	private static int directFilter0;

	public static float globalReverbMultiplier = 0.7f * Config.globalReverbGain;

	public static float globalRolloffFactor = Config.rolloffFactor;
	public static float globalVolumeMultiplier = 4.0f;
	private static SoundCategory lastSoundCategory;
	private static String lastSoundName;
	private static final String logPrefix = "[SOUND PHYSICS]";
	private static Minecraft mc;
	public static final String mcVersion = "1.7.10";
	public static final String modid = "soundphysics";
	private static final Pattern noteBlockPattern = Pattern.compile(
			".*note/.*"
			);
	private static ProcThread proc_thread;
	private static final Pattern rainPattern = Pattern.compile(".*rain.*");
	private static int reverb0;
	private static int reverb1;
	private static int reverb2;

	private static int reverb3;

	private static int sendFilter0;
	private static int sendFilter1;

	private static int sendFilter2;
	private static int sendFilter3;
	public static double soundDistanceAllowance = Config.soundDistanceAllowance;

	private static volatile List<Source> source_list;
	private static final Pattern stepPattern = Pattern.compile(".*step.*");
	private static volatile boolean thread_alive;
	private static final Pattern uiPattern = Pattern.compile(".*\\/ui\\/.*");
	public static final String version = "1.0.6";

	public static void applyConfigChanges() {
		SoundPhysics.globalRolloffFactor = Config.rolloffFactor;
		SoundPhysics.globalReverbMultiplier = 0.7f * Config.globalReverbGain;
		SoundPhysics.soundDistanceAllowance = Config.soundDistanceAllowance;

		if (SoundPhysics.auxFXSlot0 != 0) {
			// Set the global reverb parameters and apply them to the effect and
			// effectslot
			SoundPhysics.setReverbParams(
					ReverbParams.getReverb0(), SoundPhysics.auxFXSlot0, SoundPhysics.reverb0
					);
			SoundPhysics.setReverbParams(
					ReverbParams.getReverb1(), SoundPhysics.auxFXSlot1, SoundPhysics.reverb1
					);
			SoundPhysics.setReverbParams(
					ReverbParams.getReverb2(), SoundPhysics.auxFXSlot2, SoundPhysics.reverb2
					);
			SoundPhysics.setReverbParams(
					ReverbParams.getReverb3(), SoundPhysics.auxFXSlot3, SoundPhysics.reverb3
					);
		}
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static double calculateEntitySoundOffset(
			final Entity entity, final String name
			) {
		if (name != null) {
			if (SoundPhysics.stepPattern.matcher(name).matches()) {
				return 0;
			}
		}
		return entity.getEyeHeight();
	}

	protected static boolean checkErrorLog(final String errorMessage) {
		final int error = AL10.alGetError();
		if (error == AL10.AL_NO_ERROR) {
			return false;
		}

		String errorName;

		switch (error) {
			case AL10.AL_INVALID_NAME :
				errorName = "AL_INVALID_NAME";
				break;
			case AL10.AL_INVALID_ENUM :
				errorName = "AL_INVALID_ENUM";
				break;
			case AL10.AL_INVALID_VALUE :
				errorName = "AL_INVALID_VALUE";
				break;
			case AL10.AL_INVALID_OPERATION :
				errorName = "AL_INVALID_OPERATION";
				break;
			case AL10.AL_OUT_OF_MEMORY :
				errorName = "AL_OUT_OF_MEMORY";
				break;
			default :
				errorName = Integer.toString(error);
				break;
		}

		SoundPhysics.logError(errorMessage + " OpenAL error " + errorName);
		return true;
	}

	private static void evaluateEnvironment(
			final int sourceID, final float posX, final float posY, final float posZ, final SoundCategory category, final String name
			) {
		if ((SoundPhysics.mc.thePlayer == null)
				| (SoundPhysics.mc.theWorld == null) | (posY <= 0)
				| (category == SoundCategory.RECORDS)
				| (category == SoundCategory.MUSIC)) {
			// posY <= 0 as a condition has to be there: Ingame
			// menu clicks do have a player and world present
			SoundPhysics.setEnvironment(
					sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
					);
			return;
		}

		final boolean isRain = SoundPhysics.rainPattern.matcher(name).matches();

		if (Config.skipRainOcclusionTracing && isRain) {
			SoundPhysics.setEnvironment(
					sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
					);
			return;
		}

		float directCutoff = 1.0f;
		final float absorptionCoeff = Config.globalBlockAbsorption * 3.0f;

		final Vec3 playerPos = Vec3.createVectorHelper(
				SoundPhysics.mc.thePlayer.posX, SoundPhysics.mc.thePlayer.posY
				+ SoundPhysics.mc.thePlayer.getEyeHeight(), SoundPhysics.mc.thePlayer.posZ
				);
		final Vec3 soundPos = SoundPhysics.offsetSoundByName(
				posX, posY, posZ, playerPos, name, category
				);
		final Vec3 normalToPlayer = playerPos.subtract(soundPos).normalize();

		float snowFactor = 0.0f;

		if (SoundPhysics.mc.theWorld.isRaining()) {
			final Vec3 middlePos = playerPos.addVector(
					soundPos.xCoord, soundPos.yCoord, soundPos.zCoord
					);
			middlePos.xCoord = middlePos.xCoord * 0.5d;
			middlePos.yCoord = middlePos.yCoord * 0.5d;
			middlePos.zCoord = middlePos.zCoord * 0.5d;
			final int snowingPlayer = SoundPhysics.isSnowingAt(
					playerPos, false
					);
			final int snowingSound = SoundPhysics.isSnowingAt(soundPos, false);
			final int snowingMiddle = SoundPhysics.isSnowingAt(
					middlePos, false
					);
			snowFactor = (snowingPlayer * 0.25f) + (snowingMiddle * 0.5f)
					+ (snowingSound * 0.25f);
		}

		float airAbsorptionFactor = 1.0f;

		if (snowFactor > 0.0f) {
			airAbsorptionFactor = Math.max(
					Config.snowAirAbsorptionFactor
					* SoundPhysics.mc.theWorld.getRainStrength(1.0f) * snowFactor, airAbsorptionFactor
					);
		}

		/*final double distance = playerPos.distanceTo(soundPos);
		final double time = (distance/343.3)*1000;
		AL10.alSourcePause(sourceID);
		log("paused, time "+String.valueOf(time));

		new java.util.Timer().schedule(
			new java.util.TimerTask() {
				@Override
				public void run() {
					log("play, time "+String.valueOf(time));
					AL10.alSourcePlay(sourceID);
				}
			},
			(long)time
		);*/

		Vec3 rayOrigin = soundPos;

		float occlusionAccumulation = 0.0f;

		for (int i = 0; i < 10; i++) {
			final MovingObjectPosition rayHit = SoundPhysics.mc.theWorld.rayTraceBlocks(
					rayOrigin, playerPos, true
					);

			if (rayHit == null) {
				break;
			}

			final Block blockHit = SoundPhysics.mc.theWorld.getBlock(
					rayHit.blockX, rayHit.blockY, rayHit.blockZ
					);

			float blockOcclusion = 1.0f;

			if (!blockHit.isOpaqueCube()) {
				// log("not a solid block!");
				blockOcclusion *= 0.15f;
			}

			occlusionAccumulation += blockOcclusion;

			rayOrigin = Vec3.createVectorHelper(
					rayHit.hitVec.xCoord + (normalToPlayer.xCoord
							* 0.1), rayHit.hitVec.yCoord
					+ (normalToPlayer.yCoord
							* 0.1), rayHit.hitVec.zCoord
					+ (normalToPlayer.zCoord
							* 0.1)
					);
		}

		directCutoff = (float) Math.exp(
				-occlusionAccumulation * absorptionCoeff
				);
		float directGain = (float) Math.pow(directCutoff, 0.1);

		// Calculate reverb parameters for this sound
		float sendGain0 = 0.0f;
		float sendGain1 = 0.0f;
		float sendGain2 = 0.0f;
		float sendGain3 = 0.0f;

		float sendCutoff0 = 1.0f;
		float sendCutoff1 = 1.0f;
		float sendCutoff2 = 1.0f;
		float sendCutoff3 = 1.0f;

		if (SoundPhysics.mc.thePlayer.isInsideOfMaterial(Material.water)) {
			directCutoff *= 1.0f - Config.underwaterFilter;
		}

		if (isRain) {
			SoundPhysics.setEnvironment(
					sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2, sendCutoff3, directCutoff, directGain, airAbsorptionFactor
					);
			return;
		}

		// Shoot rays around sound
		final float phi = 1.618033988f;
		final float gAngle = phi * (float) Math.PI * 2.0f;
		final float maxDistance = 256.0f;

		final int numRays = Config.environmentEvaluationRays;
		final int rayBounces = Config.environmentEvaluationRaysBounces;

		final float[] bounceReflectivityRatio = new float[rayBounces];

		float sharedAirspace = 0.0f;

		final float rcpTotalRays = 1.0f / (numRays * rayBounces);
		final float rcpPrimaryRays = 1.0f / numRays;

		for (int i = 0; i < numRays; i++) {
			final float fi = i;
			final float fiN = fi / numRays;
			final float longitude = gAngle * fi;
			final float latitude = (float) Math.asin((fiN * 2.0f) - 1.0f);

			final Vec3 rayDir = Vec3.createVectorHelper(
					Math.cos(latitude) * Math.cos(longitude), Math.cos(latitude) * Math.sin(longitude), Math.sin(latitude)
					);

			final Vec3 rayStart = Vec3.createVectorHelper(
					soundPos.xCoord, soundPos.yCoord, soundPos.zCoord
					);

			final Vec3 rayEnd = Vec3.createVectorHelper(
					rayStart.xCoord
					+ (rayDir.xCoord * maxDistance), rayStart.yCoord
					+ (rayDir.yCoord
							* maxDistance), rayStart.zCoord
					+ (rayDir.zCoord
							* maxDistance)
					);

			final MovingObjectPosition rayHit = SoundPhysics.mc.theWorld.rayTraceBlocks(
					rayStart, rayEnd, true
					);

			if (rayHit != null) {
				final double rayLength = soundPos.distanceTo(rayHit.hitVec);

				// Additional bounces
				Block lastHitBlock = SoundPhysics.mc.theWorld.getBlock(
						rayHit.blockX, rayHit.blockY, rayHit.blockZ
						);
				Vec3 lastHitPos = rayHit.hitVec;
				Vec3 lastHitNormal = SoundPhysics.getNormalFromFacing(
						rayHit.sideHit
						);
				Vec3 lastRayDir = rayDir;

				float totalRayDistance = (float) rayLength;

				// Secondary ray bounces
				for (int j = 0; j < rayBounces; j++) {
					final Vec3 newRayDir = SoundPhysics.reflect(
							lastRayDir, lastHitNormal
							);
					// Vec3 newRayDir = lastHitNormal;
					final Vec3 newRayStart = Vec3.createVectorHelper(
							lastHitPos.xCoord + (lastHitNormal.xCoord
									* 0.01), lastHitPos.yCoord
							+ (lastHitNormal.yCoord
									* 0.01), lastHitPos.zCoord
							+ (lastHitNormal.zCoord
									* 0.01)
							);
					final Vec3 newRayEnd = Vec3.createVectorHelper(
							newRayStart.xCoord + (newRayDir.xCoord
									* maxDistance), newRayStart.yCoord
							+ (newRayDir.yCoord
									* maxDistance), newRayStart.zCoord
							+ (newRayDir.zCoord
									* maxDistance)
							);

					final MovingObjectPosition newRayHit = SoundPhysics.mc.theWorld.rayTraceBlocks(
							newRayStart, newRayEnd, true
							);

					float energyTowardsPlayer = 0.25f;
					final float blockReflectivity = SoundPhysics.getBlockReflectivity(
							lastHitBlock
							);
					energyTowardsPlayer *= (blockReflectivity * 0.75f) + 0.25f;

					if (newRayHit == null) {
						totalRayDistance += lastHitPos.distanceTo(playerPos);
					}
					else {
						final double newRayLength = lastHitPos.distanceTo(
								newRayHit.hitVec
								);

						bounceReflectivityRatio[j] += blockReflectivity;

						totalRayDistance += newRayLength;

						lastHitPos = newRayHit.hitVec;
						lastHitNormal = SoundPhysics.getNormalFromFacing(
								newRayHit.sideHit
								);
						lastRayDir = newRayDir;
						lastHitBlock = SoundPhysics.mc.theWorld.getBlock(
								newRayHit.blockX, newRayHit.blockY, newRayHit.blockZ
								);

						// Cast one final ray towards the player. If it's
						// unobstructed, then the sound source and the player
						// share airspace.
						if ((Config.simplerSharedAirspaceSimulation
								&& (j == (rayBounces - 1)))
								|| !Config.simplerSharedAirspaceSimulation) {
							final Vec3 finalRayStart = Vec3.createVectorHelper(
									lastHitPos.xCoord + (lastHitNormal.xCoord
											* 0.01), lastHitPos.yCoord
									+ (lastHitNormal.yCoord
											* 0.01), lastHitPos.zCoord
									+ (lastHitNormal.zCoord
											* 0.01)
									);

							final MovingObjectPosition finalRayHit = SoundPhysics.mc.theWorld.rayTraceBlocks(
									finalRayStart, playerPos, true
									);

							if (finalRayHit == null) {
								// log("Secondary ray hit the player!");
								sharedAirspace += 1.0f;
							}
						}
					}

					final float reflectionDelay = (float) Math.max(
							totalRayDistance, 0.0
							) * 0.12f * blockReflectivity;

					final float cross0 = 1.0f - MathHelper.clamp_float(
							Math.abs(reflectionDelay - 0.0f), 0.0f, 1.0f
							);
					final float cross1 = 1.0f - MathHelper.clamp_float(
							Math.abs(reflectionDelay - 1.0f), 0.0f, 1.0f
							);
					final float cross2 = 1.0f - MathHelper.clamp_float(
							Math.abs(reflectionDelay - 2.0f), 0.0f, 1.0f
							);
					final float cross3 = MathHelper.clamp_float(
							reflectionDelay - 2.0f, 0.0f, 1.0f
							);

					sendGain0 += cross0 * energyTowardsPlayer * 6.4f
							* rcpTotalRays;
					sendGain1 += cross1 * energyTowardsPlayer * 12.8f
							* rcpTotalRays;
					sendGain2 += cross2 * energyTowardsPlayer * 12.8f
							* rcpTotalRays;
					sendGain3 += cross3 * energyTowardsPlayer * 12.8f
							* rcpTotalRays;

					// Nowhere to bounce off of, stop bouncing!
					if (newRayHit == null) {
						break;
					}
				}
			}

		}

		// log("total reflectivity ratio: " + totalReflectivityRatio);

		bounceReflectivityRatio[0] = bounceReflectivityRatio[0] / numRays;
		bounceReflectivityRatio[1] = bounceReflectivityRatio[1] / numRays;
		bounceReflectivityRatio[2] = bounceReflectivityRatio[2] / numRays;
		bounceReflectivityRatio[3] = bounceReflectivityRatio[3] / numRays;

		sharedAirspace *= 64.0f;

		if (Config.simplerSharedAirspaceSimulation) {
			sharedAirspace *= rcpPrimaryRays;
		}
		else {
			sharedAirspace *= rcpTotalRays;
		}

		final float sharedAirspaceWeight0 = MathHelper.clamp_float(
				sharedAirspace / 20.0f, 0.0f, 1.0f
				);
		final float sharedAirspaceWeight1 = MathHelper.clamp_float(
				sharedAirspace / 15.0f, 0.0f, 1.0f
				);
		final float sharedAirspaceWeight2 = MathHelper.clamp_float(
				sharedAirspace / 10.0f, 0.0f, 1.0f
				);
		final float sharedAirspaceWeight3 = MathHelper.clamp_float(
				sharedAirspace / 10.0f, 0.0f, 1.0f
				);

		sendCutoff0 = ((float) Math.exp(
				-occlusionAccumulation * absorptionCoeff * 1.0f
				) * (1.0f - sharedAirspaceWeight0)) + sharedAirspaceWeight0;
		sendCutoff1 = ((float) Math.exp(
				-occlusionAccumulation * absorptionCoeff * 1.0f
				) * (1.0f - sharedAirspaceWeight1)) + sharedAirspaceWeight1;
		sendCutoff2 = ((float) Math.exp(
				-occlusionAccumulation * absorptionCoeff * 1.5f
				) * (1.0f - sharedAirspaceWeight2)) + sharedAirspaceWeight2;
		sendCutoff3 = ((float) Math.exp(
				-occlusionAccumulation * absorptionCoeff * 1.5f
				) * (1.0f - sharedAirspaceWeight3)) + sharedAirspaceWeight3;

		// attempt to preserve directionality when airspace is shared by
		// allowing some of the dry signal through but filtered
		final float averageSharedAirspace = (sharedAirspaceWeight0
				+ sharedAirspaceWeight1 + sharedAirspaceWeight2
				+ sharedAirspaceWeight3) * 0.25f;
		directCutoff = Math.max(
				(float) Math.pow(averageSharedAirspace, 0.5) * 0.2f, directCutoff
				);

		directGain = (float) Math.pow(directCutoff, 0.1);

		sendGain1 *= bounceReflectivityRatio[1];
		sendGain2 *= (float) Math.pow(bounceReflectivityRatio[2], 3.0);
		sendGain3 *= (float) Math.pow(bounceReflectivityRatio[3], 4.0);

		sendGain0 = MathHelper.clamp_float(sendGain0, 0.0f, 1.0f);
		sendGain1 = MathHelper.clamp_float(sendGain1, 0.0f, 1.0f);
		sendGain2 = MathHelper.clamp_float(
				(sendGain2 * 1.05f) - 0.05f, 0.0f, 1.0f
				);
		sendGain3 = MathHelper.clamp_float(
				(sendGain3 * 1.05f) - 0.05f, 0.0f, 1.0f
				);

		sendGain0 *= (float) Math.pow(sendCutoff0, 0.1);
		sendGain1 *= (float) Math.pow(sendCutoff1, 0.1);
		sendGain2 *= (float) Math.pow(sendCutoff2, 0.1);
		sendGain3 *= (float) Math.pow(sendCutoff3, 0.1);

		if (SoundPhysics.mc.thePlayer.isInWater()) {
			sendCutoff0 *= 0.4f;
			sendCutoff1 *= 0.4f;
			sendCutoff2 *= 0.4f;
			sendCutoff3 *= 0.4f;
		}

		SoundPhysics.setEnvironment(
				sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2, sendCutoff3, directCutoff, directGain, airAbsorptionFactor
				);
	}

	/*@Mod.EventBusSubscriber
	public static class DebugDisplayEventHandler {
		@SubscribeEvent
		public static void onDebugOverlay(RenderGameOverlayEvent.Text event)
		{
			if(mc.gameSettings.showDebugInfo && Config.dynamicEnvironementEvalutaion && Config.debugInfoShow) {
				event.getLeft().add("");
				event.getLeft().add("[SoundPhysics] "+String.valueOf(source_list.size())+" Sources");
				event.getLeft().add("[SoundPhysics] Source list :");
				synchronized (source_list) {
					ListIterator<Source> iter = source_list.listIterator();
					while (iter.hasNext())  {
						Source s = iter.next();
						Vec3d tmp = new Vec3d(s.posX,s.posY,s.posZ);
						event.getLeft().add(String.valueOf(s.sourceID)+"-"+s.category.toString()+"-"+s.name+"-"+tmp.toString());
						/*int buffq = AL10.alGetSourcei(s.sourceID, AL10.AL_BUFFERS_QUEUED);
						int buffp = AL10.alGetSourcei(s.sourceID, AL10.AL_BUFFERS_PROCESSED);
						int sampoff = AL10.alGetSourcei(s.sourceID, AL11.AL_SAMPLE_OFFSET);
						int byteoff = AL10.alGetSourcei(s.sourceID, AL11.AL_BYTE_OFFSET);
						String k = "";
						if (sampoff!=0) {
							//k = String.valueOf(sampoff)+"/"+String.valueOf((byteoff/sampoff)*size)+" ";
							k = String.valueOf((float)sampoff/(float)s.frequency)+"/"+String.valueOf((float)((byteoff/sampoff)*s.size)/(float)s.frequency)+" ";
						} else {
							k = "0/? ";
						}
						event.getLeft().add(k+String.valueOf(buffp)+"/"+String.valueOf(buffq)+" "+String.valueOf(s.bufferID));
						event.getLeft().add("----");
					}
				}
			}
		}
	}*/

	private static float getBlockReflectivity(final Block block) {
		final Block.SoundType soundType = block.stepSound;
		final Material blockMaterial = block.getMaterial();

		float reflectivity = 0.5f;

		if ((soundType == Block.soundTypeStone)
				|| (soundType == Block.soundTypePiston)) {
			reflectivity = Config.stoneReflectivity;
		}
		else if (soundType == Block.soundTypeWood) {
			reflectivity = Config.woodReflectivity;
		}
		else if ((soundType == Block.soundTypeGravel)
				|| (soundType == Block.soundTypeGrass)) {
			if ((blockMaterial == Material.plants)
					|| (blockMaterial == Material.vine)
					|| (blockMaterial == Material.grass)
					|| (blockMaterial == Material.leaves)) {
				reflectivity = Config.plantReflectivity;
			}
			else {
				reflectivity = Config.groundReflectivity;
			}
		}
		else if (soundType == Block.soundTypeMetal) {
			reflectivity = Config.metalReflectivity;
		}
		else if (soundType == Block.soundTypeGlass) {
			reflectivity = Config.glassReflectivity;
		}
		else if (soundType == Block.soundTypeCloth) {
			reflectivity = Config.clothReflectivity;
		}
		else if (soundType == Block.soundTypeSand) {
			reflectivity = Config.sandReflectivity;
		}
		else if (soundType == Block.soundTypeSnow) {
			reflectivity = Config.snowReflectivity;
		}
		else if (soundType == Block.soundTypeLadder) {
			reflectivity = Config.woodReflectivity;
		}
		else if (soundType == Block.soundTypeAnvil) {
			reflectivity = Config.metalReflectivity;
		}

		reflectivity *= Config.globalBlockReflectance;

		return reflectivity;
	}

	private static Vec3 getNormalFromFacing(final int sideHit) {
		final EnumFacing facing = EnumFacing.getFront(sideHit);// getFacingFromSide(sideHit);
		return Vec3.createVectorHelper(
				facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ()
				);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void init() {
		SoundPhysics.setupEFX();
		SoundPhysics.mc = Minecraft.getMinecraft();
		SoundPhysics.setupThread();
	}

	// Copy of isRainingAt (1.12.2)
	private static int isSnowingAt(
			final Vec3 position, final boolean check_rain
			) {
		if (check_rain && !SoundPhysics.mc.theWorld.isRaining()) {
			return 0;
		}
		else if (!SoundPhysics.mc.theWorld.canBlockSeeTheSky(
				(int) position.xCoord, (int) position.yCoord, (int) position.zCoord
				)) {
			return 0;
		}
		else if (SoundPhysics.mc.theWorld.getPrecipitationHeight(
				(int) position.xCoord, (int) position.zCoord
				) > position.yCoord) {
			return 0;
		}
		else {
			/*boolean cansnow = mc.theWorld.canSnowAt(position, false);
			if (mc.theWorld.getBiome(position).getEnableSnow() && cansnow) return true;
			else if (cansnow) return true;
			else return false;*/
			// canSnowAt() but the name isn't there
			return (SoundPhysics.mc.theWorld.func_147478_e(
					(int) position.xCoord, (int) position.yCoord, (int) position.zCoord, false
					) | SoundPhysics.mc.theWorld.getBiomeGenForCoords(
							(int) position.xCoord, (int) position.zCoord
							).getEnableSnow()) ? 1 : 0;
		}
	}

	public static void log(final String message) {
		System.out.println(
				SoundPhysics.logPrefix.concat(" : ").concat(message)
				);
	}

	public static void logError(final String errorMessage) {
		System.out.println(
				SoundPhysics.logPrefix.concat(" [ERROR] : ").concat(errorMessage)
				);
	}

	private static Vec3 offsetSoundByName(
			final double soundX, final double soundY, final double soundZ, final Vec3 playerPos, final String name, final SoundCategory category
			) {
		double offsetX = 0.0;
		double offsetY = 0.0;
		double offsetZ = 0.0;
		double offsetTowardsPlayer = 0.0;

		double tempNormX = 0;
		double tempNormY = 0;
		double tempNormZ = 0;

		if (((soundY % 1.0) < 0.001)
				|| SoundPhysics.stepPattern.matcher(name).matches()) {
			offsetY = 0.13;
		}

		if ((category == SoundCategory.BLOCKS)
				|| SoundPhysics.blockPattern.matcher(name).matches() || !SoundPhysics.mc.theWorld.isAirBlock((int) Math.floor(soundX), (int) Math.floor(soundY), (int) Math.floor(soundZ))) {
			// The ray will probably hit the block that it's emitting from
			// before
			// escaping. Offset the ray start position towards the player by the
			// diagonal half length of a cube

			tempNormX = playerPos.xCoord - soundX;
			tempNormY = playerPos.yCoord - soundY;
			tempNormZ = playerPos.zCoord - soundZ;
			final double length = Math.sqrt(
					(tempNormX * tempNormX) + (tempNormY * tempNormY)
					+ (tempNormZ * tempNormZ)
					);
			tempNormX /= length;
			tempNormY /= length;
			tempNormZ /= length;
			// 0.867 > square root of 0.5^2 * 3
			offsetTowardsPlayer = 0.867;
			offsetX += tempNormX * offsetTowardsPlayer;
			offsetY += tempNormY * offsetTowardsPlayer;
			offsetZ += tempNormZ * offsetTowardsPlayer;
		}

		return Vec3.createVectorHelper(
				soundX + offsetX, soundY + offsetY, soundZ + offsetZ
				);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static SoundBuffer onLoadSound(
			final SoundBuffer buff, final String filename
			) {
		if ((buff == null) || (buff.audioFormat.getChannels() == 1)
				|| !Config.autoSteroDownmix) {
			return buff;
		}
		if ((SoundPhysics.mc.thePlayer == null)
				| (SoundPhysics.mc.theWorld == null)
				| (SoundPhysics.lastSoundCategory == SoundCategory.RECORDS)
				| (SoundPhysics.lastSoundCategory == SoundCategory.MUSIC)
				| SoundPhysics.uiPattern.matcher(filename).matches() | SoundPhysics.clickPattern.matcher(filename).matches()) {
			if (Config.autoSteroDownmixLogging) {
				SoundPhysics.log(
						"Not converting sound '" + filename + "'("
								+ buff.audioFormat.toString() + ")"
						);
			}
			return buff;
		}
		final AudioFormat orignalformat = buff.audioFormat;
		final int bits = orignalformat.getSampleSizeInBits();
		final boolean bigendian = orignalformat.isBigEndian();
		final AudioFormat monoformat = new AudioFormat(
				orignalformat.getEncoding(), orignalformat.getSampleRate(), bits, 1, orignalformat.getFrameSize(), orignalformat.getFrameRate(), bigendian
				);
		if (Config.autoSteroDownmixLogging) {
			SoundPhysics.log(
					"Converting sound '" + filename + "'("
							+ orignalformat.toString() + ") to mono ("
							+ monoformat.toString() + ")"
					);
		}

		final ByteBuffer bb = ByteBuffer.wrap(
				buff.audioData, 0, buff.audioData.length
				);
		bb.order(bigendian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		if (bits == 8) {
			for (int i = 0; i < buff.audioData.length; i += 2) {
				bb.put(i / 2, (byte) ((bb.get(i) + bb.get(i + 1)) / 2));
			}
		}
		else if (bits == 16) {
			for (int i = 0; i < buff.audioData.length; i += 4) {
				bb.putShort(
						(i / 2), (short) ((bb.getShort(i) + bb.getShort(i + 2)) / 2)
						);
			}
		}
		buff.audioFormat = monoformat;
		buff.trimData(buff.audioData.length / 2);
		return buff;
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	// For sounds that get played normally
	public static void onPlaySound(
			final float posX, final float posY, final float posZ, final int sourceID
			) {
		SoundPhysics.onPlaySound(
				posX, posY, posZ, sourceID, SoundPhysics.lastSoundCategory, SoundPhysics.lastSoundName
				);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void onPlaySound(
			final float posX, final float posY, final float posZ, final int sourceID, SoundCategory soundCat, final String soundName
			) {
		// log(String.valueOf(posX)+" "+String.valueOf(posY)+"
		// "+String.valueOf(posZ)+" - "+String.valueOf(sourceID)+" -
		// "+soundCat.toString()+" - "+soundName);
		if (Config.noteBlockEnable && (soundCat == SoundCategory.RECORDS)
				&& SoundPhysics.noteBlockPattern.matcher(soundName).matches()) {
			soundCat = SoundCategory.BLOCKS;
		}
		SoundPhysics.evaluateEnvironment(
				sourceID, posX, posY, posZ, soundCat, soundName
				);
		if (!Config.dynamicEnvironementEvalutaion) {
			return;
		}
		if (((SoundPhysics.mc.thePlayer == null)
				| (SoundPhysics.mc.theWorld == null) | (posY <= 0)
				| (soundCat == SoundCategory.RECORDS)
				| (soundCat == SoundCategory.MUSIC))
				|| (Config.skipRainOcclusionTracing
						&& SoundPhysics.rainPattern.matcher(soundName).matches())) {
			return;
		}
		final Source tmp = new Source(
				sourceID, posX, posY, posZ, soundCat, soundName
				);
		if (SoundPhysics.source_check(tmp)) {
			return;
		}
		SoundPhysics.source_list.add(tmp);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	// For sounds that get played using OpenAL directly or just not using the
	// minecraft sound system
	public static void onPlaySoundAL(
			final float posX, final float posY, final float posZ, final int sourceID
			) {
		SoundPhysics.onPlaySound(
				posX, posY, posZ, sourceID, SoundCategory.BLOCKS, "null"
				);
	}

	private static Vec3 reflect(final Vec3 dir, final Vec3 normal) {
		final double dot2 = dir.dotProduct(normal) * 2;

		final double x = dir.xCoord - (dot2 * normal.xCoord);
		final double y = dir.yCoord - (dot2 * normal.yCoord);
		final double z = dir.zCoord - (dot2 * normal.zCoord);

		return Vec3.createVectorHelper(x, y, z);
	}

	/*private static EnumFacing getFacingFromSide(final int side) {
		switch (side) {
			case 0: return EnumFacing.DOWN;
			case 1: return EnumFacing.UP;
			case 2: return EnumFacing.EAST;
			case 3: return EnumFacing.WEST;
			case 4: return EnumFacing.NORTH;
			case 5: return EnumFacing.SOUTH;
			default: return EnumFacing.UP;
		}
	}*/

	private static void setEnvironment(
			final int sourceID, final float sendGain0, final float sendGain1, final float sendGain2, final float sendGain3, final float sendCutoff0, final float sendCutoff1, final float sendCutoff2, final float sendCutoff3, final float directCutoff, final float directGain, final float airAbsorptionFactor
			) {
		// Set reverb send filter values and set source to send to all reverb fx
		// slots
		EFX10.alFilterf(
				SoundPhysics.sendFilter0, EFX10.AL_LOWPASS_GAIN, sendGain0
				);
		EFX10.alFilterf(
				SoundPhysics.sendFilter0, EFX10.AL_LOWPASS_GAINHF, sendCutoff0
				);
		AL11.alSource3i(
				sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, SoundPhysics.auxFXSlot0, 0, SoundPhysics.sendFilter0
				);

		EFX10.alFilterf(
				SoundPhysics.sendFilter1, EFX10.AL_LOWPASS_GAIN, sendGain1
				);
		EFX10.alFilterf(
				SoundPhysics.sendFilter1, EFX10.AL_LOWPASS_GAINHF, sendCutoff1
				);
		AL11.alSource3i(
				sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, SoundPhysics.auxFXSlot1, 1, SoundPhysics.sendFilter1
				);

		EFX10.alFilterf(
				SoundPhysics.sendFilter2, EFX10.AL_LOWPASS_GAIN, sendGain2
				);
		EFX10.alFilterf(
				SoundPhysics.sendFilter2, EFX10.AL_LOWPASS_GAINHF, sendCutoff2
				);
		AL11.alSource3i(
				sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, SoundPhysics.auxFXSlot2, 2, SoundPhysics.sendFilter2
				);

		EFX10.alFilterf(
				SoundPhysics.sendFilter3, EFX10.AL_LOWPASS_GAIN, sendGain3
				);
		EFX10.alFilterf(
				SoundPhysics.sendFilter3, EFX10.AL_LOWPASS_GAINHF, sendCutoff3
				);
		AL11.alSource3i(
				sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, SoundPhysics.auxFXSlot3, 3, SoundPhysics.sendFilter3
				);

		EFX10.alFilterf(
				SoundPhysics.directFilter0, EFX10.AL_LOWPASS_GAIN, directGain
				);
		EFX10.alFilterf(
				SoundPhysics.directFilter0, EFX10.AL_LOWPASS_GAINHF, directCutoff
				);
		AL10.alSourcei(
				sourceID, EFX10.AL_DIRECT_FILTER, SoundPhysics.directFilter0
				);

		AL10.alSourcef(
				sourceID, EFX10.AL_AIR_ABSORPTION_FACTOR, MathHelper.clamp_float(
						Config.airAbsorption * airAbsorptionFactor, 0.0f, 10.0f
						)
				);
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void setLastSoundCategory(final SoundCategory sc) {
		SoundPhysics.lastSoundCategory = sc;
	}

	/**
	 * CALLED BY ASM INJECTED CODE!
	 */
	public static void setLastSoundName(final String soundName) {
		SoundPhysics.lastSoundName = soundName;
	}

	/**
	 * Applies the parameters in the enum ReverbParams to the main reverb
	 * effect.
	 */
	protected static void setReverbParams(
			final ReverbParams r, final int auxFXSlot, final int reverbSlot
			) {
		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DENSITY, r.density);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb density: " + r.density
				);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DIFFUSION, r.diffusion);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb diffusion: " + r.diffusion
				);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAIN, r.gain);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb gain: " + r.gain
				);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAINHF, r.gainHF);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb gainHF: " + r.gainHF
				);

		EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DECAY_TIME, r.decayTime);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb decayTime: " + r.decayTime
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_DECAY_HFRATIO, r.decayHFRatio
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb decayHFRatio: " + r.decayHFRatio
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_REFLECTIONS_GAIN, r.reflectionsGain
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb reflectionsGain: "
						+ r.reflectionsGain
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_GAIN, r.lateReverbGain
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb lateReverbGain: "
						+ r.lateReverbGain
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_DELAY, r.lateReverbDelay
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb lateReverbDelay: "
						+ r.lateReverbDelay
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, r.airAbsorptionGainHF
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb airAbsorptionGainHF: "
						+ r.airAbsorptionGainHF
				);

		EFX10.alEffectf(
				reverbSlot, EFX10.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, r.roomRolloffFactor
				);
		SoundPhysics.checkErrorLog(
				"Error while assigning reverb roomRolloffFactor: "
						+ r.roomRolloffFactor
				);

		// Attach updated effect object
		EFX10.alAuxiliaryEffectSloti(
				auxFXSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbSlot
				);
	}

	private static void setupEFX() {
		// Get current context and device
		final ALCcontext currentContext = ALC10.alcGetCurrentContext();
		final ALCdevice currentDevice = ALC10.alcGetContextsDevice(
				currentContext
				);

		if (ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
			SoundPhysics.log("EFX Extension recognized.");
		}
		else {
			SoundPhysics.logError(
					"EFX Extension not found on current device. Aborting."
					);
			return;
		}

		// Create auxiliary effect slots
		SoundPhysics.auxFXSlot0 = EFX10.alGenAuxiliaryEffectSlots();
		SoundPhysics.log("Aux slot " + SoundPhysics.auxFXSlot0 + " created");
		EFX10.alAuxiliaryEffectSloti(
				SoundPhysics.auxFXSlot0, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
				);

		SoundPhysics.auxFXSlot1 = EFX10.alGenAuxiliaryEffectSlots();
		SoundPhysics.log("Aux slot " + SoundPhysics.auxFXSlot1 + " created");
		EFX10.alAuxiliaryEffectSloti(
				SoundPhysics.auxFXSlot1, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
				);

		SoundPhysics.auxFXSlot2 = EFX10.alGenAuxiliaryEffectSlots();
		SoundPhysics.log("Aux slot " + SoundPhysics.auxFXSlot2 + " created");
		EFX10.alAuxiliaryEffectSloti(
				SoundPhysics.auxFXSlot2, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
				);

		SoundPhysics.auxFXSlot3 = EFX10.alGenAuxiliaryEffectSlots();
		SoundPhysics.log("Aux slot " + SoundPhysics.auxFXSlot3 + " created");
		EFX10.alAuxiliaryEffectSloti(
				SoundPhysics.auxFXSlot3, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
				);
		SoundPhysics.checkErrorLog("Failed creating auxiliary effect slots!");

		SoundPhysics.reverb0 = EFX10.alGenEffects();
		EFX10.alEffecti(
				SoundPhysics.reverb0, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
				);
		SoundPhysics.checkErrorLog("Failed creating reverb effect slot 0!");
		SoundPhysics.reverb1 = EFX10.alGenEffects();
		EFX10.alEffecti(
				SoundPhysics.reverb1, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
				);
		SoundPhysics.checkErrorLog("Failed creating reverb effect slot 1!");
		SoundPhysics.reverb2 = EFX10.alGenEffects();
		EFX10.alEffecti(
				SoundPhysics.reverb2, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
				);
		SoundPhysics.checkErrorLog("Failed creating reverb effect slot 2!");
		SoundPhysics.reverb3 = EFX10.alGenEffects();
		EFX10.alEffecti(
				SoundPhysics.reverb3, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
				);
		SoundPhysics.checkErrorLog("Failed creating reverb effect slot 3!");

		// Create filters
		SoundPhysics.directFilter0 = EFX10.alGenFilters();
		EFX10.alFilteri(
				SoundPhysics.directFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
				);

		SoundPhysics.sendFilter0 = EFX10.alGenFilters();
		EFX10.alFilteri(
				SoundPhysics.sendFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
				);

		SoundPhysics.sendFilter1 = EFX10.alGenFilters();
		EFX10.alFilteri(
				SoundPhysics.sendFilter1, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
				);

		SoundPhysics.sendFilter2 = EFX10.alGenFilters();
		EFX10.alFilteri(
				SoundPhysics.sendFilter2, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
				);

		SoundPhysics.sendFilter3 = EFX10.alGenFilters();
		EFX10.alFilteri(
				SoundPhysics.sendFilter3, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
				);
		SoundPhysics.checkErrorLog("Error creating lowpass filters!");

		SoundPhysics.applyConfigChanges();
	}

	private static synchronized void setupThread() {
		if (SoundPhysics.source_list == null) {
			SoundPhysics.source_list = Collections.synchronizedList(
					new ArrayList<Source>()
					);
		}
		else {
			SoundPhysics.source_list.clear();
		}

		/*if (proc_thread != null) {
			thread_signal_death = false;
			thread_alive = false;
			while (!thread_signal_death);
		}*/
		if (SoundPhysics.proc_thread == null) {
			SoundPhysics.proc_thread = new ProcThread();
			SoundPhysics.thread_alive = true;
			SoundPhysics.proc_thread.start();
		}
	}

	public static boolean source_check(final Source s) {
		synchronized (SoundPhysics.source_list) {
			final ListIterator<Source> iter = SoundPhysics.source_list.listIterator();
			while (iter.hasNext()) {
				final Source sn = iter.next();
				if ((sn.sourceID == s.sourceID) && (sn.bufferID == s.bufferID)
						&& (sn.posX == s.posX) && (sn.posY == s.posY)
						&& (sn.posZ == s.posZ)) {
					return true;
				}
			}
		}
		return false;
	}

	@Mod.EventHandler
	public void init(final FMLInitializationEvent event) {
		Config.instance.init(event);
	}

	@Mod.EventHandler
	public void preInit(final FMLPreInitializationEvent event) {
		Config.instance.preInit(event);
	}

}
