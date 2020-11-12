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
package net.alkalus.envirosound;

import java.io.IOException;
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

import net.minecraft.block.BlockBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityBase;
import net.minecraft.tileentity.TileEntityBase;
import net.minecraft.util.maths.MathHelper;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.maths.Vec3f;
import net.minecraft.block.BlockSounds;
import paulscode.sound.SoundBuffer;
import paulscode.sound.SoundSystemConfig;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import net.modificationstation.stationloader.api.common.mod.StationMod;

@Environment(EnvType.CLIENT)
public class EnviroSound implements StationMod {
    // THESE VARIABLES ARE CONSTANTLY ACCESSED AND USED BY ASM INJECTED CODE! DO
    // NOT REMOVE!
    public static int attenuationModel = SoundSystemConfig.ATTENUATION_ROLLOFF;

    private static int auxFXSlot0;
    private static int auxFXSlot1;
    private static int auxFXSlot2;
    private static int auxFXSlot3;

    private static int directFilter0;

    public static float globalReverbMultiplier = 0.65f * Config.globalReverbGain;

    public static float globalRolloffFactor = Config.rolloffFactor;
    public static float globalVolumeMultiplier = 4.20f;
    private static String lastSoundName;
    private static Minecraft mc;
    public static final String mcVersion = "1.7.10";
    public static final String modid = "EnviroSound";
    private static final String logPrefix = "["+EnviroSound.modid+"]";
    private static ProcThread proc_thread;

    private static final Pattern blockPattern = Pattern.compile(".*block.*");
    private static final Pattern clickPattern = Pattern.compile(".*random.click.*");
    private static final Pattern noteBlockPattern = Pattern.compile(".*note/.*");
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
    public static final String version = getVersion();
    
    public static String getVersion() {
        return "1.0.0";
    }

    public static class ProcThread extends Thread {
        @Override
        public synchronized void run() {
            while (EnviroSound.thread_alive) {
                while (!Config.dynamicEnvironementEvalutaion) {
                    try {
                        Thread.sleep(1250); // Give it a little more time to rest, lighter on the cpu, probably not noticeable. TODO Confirm this.
                    }
                    catch (final Exception e) {
                        EnviroSound.logError(String.valueOf(e));
                    }
                }
                synchronized (EnviroSound.source_list) {
                    // log("Updating env " +
                    // String.valueOf(source_list.size()));
                    final ListIterator<Source> iter = EnviroSound.source_list.listIterator();
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
                            source.x = pos.get(0);
                            source.y = pos.get(1);
                            source.z = pos.get(2);
                            EnviroSound.evaluateEnvironment(
                                    source.sourceID, source.x, source.y, source.z, source.name
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
                    EnviroSound.logError(String.valueOf(e));
                }
            }
        }
    }
    
    public static class Source {
        public int bufferID;
        public int frequency;
        public String name;
        public float x;
        public float y;
        public float z;
        public int size;
        public int sourceID;

        public Source(
                final int sid, final float px, final float py, final float pz, final String n
                ) {
            this.sourceID = sid;
            this.x = px;
            this.y = py;
            this.z = pz;
            this.name = n;
            this.bufferID = AL10.alGetSourcei(sid, AL10.AL_BUFFER);
            this.size = AL10.alGetBufferi(this.bufferID, AL10.AL_SIZE);
            this.frequency = AL10.alGetBufferi(
                    this.bufferID, AL10.AL_FREQUENCY
                    );
        }
    }

    public static void applyConfigChanges() {
        EnviroSound.globalRolloffFactor = Config.rolloffFactor;
        EnviroSound.globalReverbMultiplier = 0.7f * Config.globalReverbGain;
        EnviroSound.soundDistanceAllowance = Config.soundDistanceAllowance;

        if (EnviroSound.auxFXSlot0 != 0) {
            // Set the global reverb parameters and apply them to the effect and
            // effectslot
            EnviroSound.setReverbParams(
                    ReverbParams.getReverb0(), EnviroSound.auxFXSlot0, EnviroSound.reverb0
                    );
            EnviroSound.setReverbParams(
                    ReverbParams.getReverb1(), EnviroSound.auxFXSlot1, EnviroSound.reverb1
                    );
            EnviroSound.setReverbParams(
                    ReverbParams.getReverb2(), EnviroSound.auxFXSlot2, EnviroSound.reverb2
                    );
            EnviroSound.setReverbParams(
                    ReverbParams.getReverb3(), EnviroSound.auxFXSlot3, EnviroSound.reverb3
                    );
        }
    }


    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static double calculateEntitySoundOffset(final EntityBase entity, final String name) {
        if (name != null) {
            if (stepPattern.matcher(name).matches()) {
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

        EnviroSound.logError(errorMessage + " OpenAL error " + errorName);
        return true;
    }

    public static Vec3f computronicsOffset(final Vec3f or, final TileEntityBase te) {
        return or;
    }

    /*@Mod.EventBusSubscriber
    public static class DebugDisplayEventHandler {
        @SubscribeEvent
        public static void onDebugOverlay(RenderGameOverlayEvent.Text event)
        {
            if(mc.gameSettings.showDebugInfo && Config.dynamicEnvironementEvalutaion && Config.debugInfoShow) {
                event.getLeft().add("");
                event.getLeft().add("[EnviroSound] "+String.valueOf(source_list.size())+" Sources");
                event.getLeft().add("[EnviroSound] Source list :");
                synchronized (source_list) {
                    ListIterator<Source> iter = source_list.listIterator();
                    while (iter.hasNext())  {
                        Source s = iter.next();
                        Vec3d tmp = new Vec3d(s.x,s.y,s.z);
                        event.getLeft().add(String.valueOf(s.sourceID)+"-"+"-"+s.name+"-"+tmp.toString());
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

    private static void evaluateEnvironment(
            final int sourceID, final float x, final float y, final float z, final String name
            ) {

        try {
            // TODO: sound category
            if ((EnviroSound.mc.player == null)
                    || (EnviroSound.mc.level == null) || (y <= 0)
                    //|| (category == SoundCategory.RECORDS)
                    //|| (category == SoundCategory.MUSIC)
                ) {
                // y <= 0 as a condition has to be there: Ingame
                // menu clicks do have a player and world present
                EnviroSound.setEnvironment(
                        sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
                        );
                return;
            }

            final boolean isRain = EnviroSound.rainPattern.matcher(name).matches();

            if (Config.skipRainOcclusionTracing && isRain) {
                EnviroSound.setEnvironment(
                        sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
                        );
                return;
            }

            float directCutoff = 1.0f;
            final float absorptionCoeff = Config.globalBlockAbsorption * 3.0f;

            final Vec3f playerPos = Vec3f.method_1293(
                    EnviroSound.mc.player.x, EnviroSound.mc.player.y
                    + EnviroSound.mc.player.getEyeHeight(), EnviroSound.mc.player.z
                    );
            final Vec3f soundPos = EnviroSound.offsetSoundByName(
                    x, y, z, playerPos, name
                    );
            final Vec3f normalToPlayer = playerPos.method_1307(soundPos).method_1296();

            float snowFactor = 0.0f;

            if (EnviroSound.mc.level.isRaining()) {
                final Vec3f middlePos = playerPos.method_1301(
                        soundPos.x, soundPos.y, soundPos.z
                        );
                middlePos.x = middlePos.x * 0.5d;
                middlePos.y = middlePos.y * 0.5d;
                middlePos.z = middlePos.z * 0.5d;
                final int snowingPlayer = EnviroSound.isSnowingAt(
                        playerPos, false
                        );
                final int snowingSound = EnviroSound.isSnowingAt(soundPos, false);
                final int snowingMiddle = EnviroSound.isSnowingAt(
                        middlePos, false
                        );
                snowFactor = (snowingPlayer * 0.25f) + (snowingMiddle * 0.5f)
                        + (snowingSound * 0.25f);
            }

            float airAbsorptionFactor = 1.0f;

            if (snowFactor > 0.0f) {
                airAbsorptionFactor = Math.max(
                        Config.snowAirAbsorptionFactor
                        * EnviroSound.mc.level.getRainGradient(1.0f) * snowFactor, airAbsorptionFactor
                        );
            }

            /*final double distance = playerPos.method_1294(soundPos);
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

            Vec3f rayOrigin = soundPos;

            float occlusionAccumulation = 0.0f;

            for (int i = 0; i < 10; i++) {
                final HitResult rayHit = EnviroSound.mc.level.method_161(
                        rayOrigin, playerPos, true
                        );

                if (rayHit == null) {
                    break;
                }

                final BlockBase blockHit = BlockBase.BY_ID[EnviroSound.mc.level.getTileId(rayHit.x, rayHit.y, rayHit.z)];

                float blockOcclusion = 1.0f;

                if (!blockHit.isFullOpaque()) {
                    // log("not a solid block!");
                    blockOcclusion *= 0.15f;
                }

                occlusionAccumulation += blockOcclusion;

                rayOrigin = Vec3f.method_1293(
                        rayHit.field_1988.x + (normalToPlayer.x
                                * 0.1), rayHit.field_1988.y
                        + (normalToPlayer.y
                                * 0.1), rayHit.field_1988.z
                        + (normalToPlayer.z
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

            if (EnviroSound.mc.player.isInFluid(Material.WATER)) {
                directCutoff *= 1.0f - Config.underwaterFilter;
            }

            if (isRain) {
                EnviroSound.setEnvironment(
                        sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2, sendCutoff3, directCutoff, directGain, airAbsorptionFactor
                        );
                return;
            }

            // Shoot rays around sound
            final float phi = 1.618033988f;
            final float gAngle = phi * (float) Math.PI * 2.0f;
            final float maxDistance = Config.maxDistance;

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

                final Vec3f rayDir = Vec3f.method_1293(
                        Math.cos(latitude) * Math.cos(longitude), Math.cos(latitude) * Math.sin(longitude), Math.sin(latitude)
                        );

                final Vec3f rayStart = Vec3f.method_1293(
                        soundPos.x, soundPos.y, soundPos.z
                        );

                final Vec3f rayEnd = Vec3f.method_1293(
                        rayStart.x
                        + (rayDir.x * maxDistance), rayStart.y
                        + (rayDir.y
                                * maxDistance), rayStart.z
                        + (rayDir.z
                                * maxDistance)
                        );

                final HitResult rayHit = EnviroSound.mc.level.method_161(
                        rayStart, rayEnd, true
                        );

                if (rayHit != null) {
                    final double rayLength = soundPos.method_1294(rayHit.field_1988);

                    // Additional bounces
                    BlockBase lastHitBlock = BlockBase.BY_ID[EnviroSound.mc.level.getTileId(rayHit.x, rayHit.y, rayHit.z)];
                    Vec3f lastHitPos = rayHit.field_1988;
                    Vec3f lastHitNormal = EnviroSound.getNormalFromFacing(
                            rayHit.field_1987
                            );
                    Vec3f lastRayDir = rayDir;

                    float totalRayDistance = (float) rayLength;

                    // Secondary ray bounces
                    for (int j = 0; j < rayBounces; j++) {
                        final Vec3f newRayDir = EnviroSound.reflect(
                                lastRayDir, lastHitNormal
                                );
                        // Vec3f newRayDir = lastHitNormal;
                        final Vec3f newRayStart = Vec3f.method_1293(
                                lastHitPos.x + (lastHitNormal.x
                                        * 0.01), lastHitPos.y
                                + (lastHitNormal.y
                                        * 0.01), lastHitPos.z
                                + (lastHitNormal.z
                                        * 0.01)
                                );
                        final Vec3f newRayEnd = Vec3f.method_1293(
                                newRayStart.x + (newRayDir.x
                                        * maxDistance), newRayStart.y
                                + (newRayDir.y
                                        * maxDistance), newRayStart.z
                                + (newRayDir.z
                                        * maxDistance)
                                );

                        final HitResult newRayHit = EnviroSound.mc.level.method_161(
                                newRayStart, newRayEnd, true
                                );

                        float energyTowardsPlayer = 0.25f;
                        final float blockReflectivity = EnviroSound.getBlockReflectivity(
                                lastHitBlock
                                );
                        energyTowardsPlayer *= (blockReflectivity * 0.75f) + 0.25f;

                        if (newRayHit == null) {
                            totalRayDistance += lastHitPos.method_1294(playerPos);
                        }
                        else {
                            final double newRayLength = lastHitPos.method_1294(
                                    newRayHit.field_1988
                                    );

                            bounceReflectivityRatio[j] += blockReflectivity;

                            totalRayDistance += newRayLength;

                            lastHitPos = newRayHit.field_1988;
                            lastHitNormal = EnviroSound.getNormalFromFacing(
                                    newRayHit.field_1987
                                    );
                            lastRayDir = newRayDir;
                            lastHitBlock = BlockBase.BY_ID[EnviroSound.mc.level.getTileId(newRayHit.x, newRayHit.y, newRayHit.z)];

                            // Cast one final ray towards the player. If it's
                            // unobstructed, then the sound source and the player
                            // share airspace.
                            if ((Config.simplerSharedAirspaceSimulation
                                    && (j == (rayBounces - 1)))
                                    || !Config.simplerSharedAirspaceSimulation) {
                                final Vec3f finalRayStart = Vec3f.method_1293(
                                        lastHitPos.x + (lastHitNormal.x
                                                * 0.01), lastHitPos.y
                                        + (lastHitNormal.y
                                                * 0.01), lastHitPos.z
                                        + (lastHitNormal.z
                                                * 0.01)
                                        );

                                final HitResult finalRayHit = EnviroSound.mc.level.method_161(
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

                        final float cross0 = 1.0f - clamp_float(
                                Math.abs(reflectionDelay - 0.0f), 0.0f, 1.0f
                                );
                        final float cross1 = 1.0f - clamp_float(
                                Math.abs(reflectionDelay - 1.0f), 0.0f, 1.0f
                                );
                        final float cross2 = 1.0f - clamp_float(
                                Math.abs(reflectionDelay - 2.0f), 0.0f, 1.0f
                                );
                        final float cross3 = clamp_float(
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

            final float sharedAirspaceWeight0 = clamp_float(
                    sharedAirspace / 20.0f, 0.0f, 1.0f
                    );
            final float sharedAirspaceWeight1 = clamp_float(
                    sharedAirspace / 15.0f, 0.0f, 1.0f
                    );
            final float sharedAirspaceWeight2 = clamp_float(
                    sharedAirspace / 10.0f, 0.0f, 1.0f
                    );
            final float sharedAirspaceWeight3 = clamp_float(
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

            sendGain0 = clamp_float(sendGain0, 0.0f, 1.0f);
            sendGain1 = clamp_float(sendGain1, 0.0f, 1.0f);
            sendGain2 = clamp_float(
                    (sendGain2 * 1.05f) - 0.05f, 0.0f, 1.0f
                    );
            sendGain3 = clamp_float(
                    (sendGain3 * 1.05f) - 0.05f, 0.0f, 1.0f
                    );

            sendGain0 *= (float) Math.pow(sendCutoff0, 0.1);
            sendGain1 *= (float) Math.pow(sendCutoff1, 0.1);
            sendGain2 *= (float) Math.pow(sendCutoff2, 0.1);
            sendGain3 *= (float) Math.pow(sendCutoff3, 0.1);

            if (EnviroSound.mc.player.method_1334()) {
                sendCutoff0 *= 0.4f;
                sendCutoff1 *= 0.4f;
                sendCutoff2 *= 0.4f;
                sendCutoff3 *= 0.4f;
            }

            EnviroSound.setEnvironment(
                    sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2, sendCutoff3, directCutoff, directGain, airAbsorptionFactor
                    );
        }
        catch (final Throwable t) {
            EnviroSound.logError("Error while evaluation environment:");
            t.printStackTrace();
            EnviroSound.setEnvironment(sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static float getBlockReflectivity(final BlockBase block) {
        final BlockSounds stepSound = block.sounds;
        final Material blockMaterial = block.material;

        float reflectivity = 0.5f;

        if ((stepSound == BlockBase.PISTON_SOUNDS)) {
            reflectivity = Config.stoneReflectivity;
        }
        else if (stepSound == BlockBase.WOOD_SOUNDS) {
            reflectivity = Config.woodReflectivity;
        }
        else if ((stepSound == BlockBase.GRAVEL_SOUNDS)
                || (stepSound == BlockBase.GRASS_SOUNDS)) {
            if ((blockMaterial == Material.PLANT)
                    || (blockMaterial == Material.ORGANIC)
                    || (blockMaterial == Material.LEAVES)) {
                reflectivity = Config.plantReflectivity;
            }
            else {
                reflectivity = Config.groundReflectivity;
            }
        }
        else if (stepSound == BlockBase.METAL_SOUNDS) {
            reflectivity = Config.metalReflectivity;
        }
        else if (stepSound == BlockBase.GLASS_SOUNDS) {
            reflectivity = Config.glassReflectivity;
        }
        else if (stepSound == BlockBase.WOOL_SOUNDS) {
            reflectivity = Config.clothReflectivity;
        }
        else if (stepSound == BlockBase.SAND_SOUNDS) {
            reflectivity = Config.sandReflectivity;
        }

        reflectivity *= Config.globalBlockReflectance;

        return reflectivity;
    }

    private static Vec3f getNormalFromFacing(final int sideHit) {
        int x = 0;
        int y = 0;
        int z = 0;
        switch(sideHit) {
            case 0:
                y--;
                break;
            case 1:
                y++;
                break;
            case 2:
                z--;
                break;
            case 3:
                z++;
                break;
            case 4:
                x--;
                break;
            case 5:
                x++;
                break;
        }
        return Vec3f.method_1293(x, y, z);
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void init() {
        EnviroSound.mc = (Minecraft)FabricLoader.getInstance().getGameInstance();
        // I could check if the sound system loaded correctly but that's long and anoying
        try {
            EnviroSound.setupEFX();
        } catch (final Throwable e) {
            e.printStackTrace();
            //EnviroSound.logError("Failed to init EFX");
            //EnviroSound.logError(e.toString());
        }
        try {
            EnviroSound.setupThread();
        } catch (final Throwable e) {
            e.printStackTrace();
            //EnviroSound.logError("Failed to init thread");
            //EnviroSound.logError(e.toString());
        }
    }

    // Copy of isRainingAt (1.12.2)
    private static int isSnowingAt(
            final Vec3f position, final boolean check_rain
            ) {
        if (check_rain && !EnviroSound.mc.level.isRaining()) {
            return 0;
        }
        else if (!EnviroSound.mc.level.isAboveGroundCached(
                (int) position.x, (int) position.y, (int) position.z
                )) {
            return 0;
        }
        else if (EnviroSound.mc.level.getHeight( // mcp 1.7.10: getPrecipitationHeight
                (int) position.x, (int) position.z
                ) > position.y) {
            return 0;
        }
        else {
            /*boolean cansnow = mc.level.canSnowAt(position, false);
            if (mc.level.getBiome(position).getEnableSnow() && cansnow) return true;
            else if (cansnow) return true;
            else return false;*/
            // canSnowAt() but the name isn't there
            return (EnviroSound.mc.level.canRainAt( // mcp 1.7.10: func_147478_e
                    (int) position.x, (int) position.y, (int) position.z
                    ) || EnviroSound.mc.level.getBiomeSource().getBiome(
                            (int) position.x, (int) position.z
                            ).canSnow()) ? 1 : 0;
        }
    }

    public static void log(final String message) {
        System.out.println(
                EnviroSound.logPrefix.concat(" : ").concat(message)
                );
    }

    public static void logError(final String errorMessage) {
        System.out.println(
                EnviroSound.logPrefix.concat(" [ERROR] : ").concat(errorMessage)
                );
    }

    private static Vec3f offsetSoundByName(
            final double soundX, final double soundY, final double soundZ, final Vec3f playerPos, final String name
            ) {
        double offsetX = 0.0;
        double offsetY = 0.0;
        double offsetZ = 0.0;
        double offsetTowardsPlayer = 0.0;

        double tempNormX = 0;
        double tempNormY = 0;
        double tempNormZ = 0;

        if (((soundY % 1.0) < 0.001)
                || EnviroSound.stepPattern.matcher(name).matches()) {
            offsetY = 0.1875;
        }

        // TODO: sound category
        if (
                //(category == SoundCategory.BLOCKS) ||
                EnviroSound.blockPattern.matcher(name).matches() ||
                ((name == "openal") && !EnviroSound.mc.level.isAir((int) Math.floor(soundX), (int) Math.floor(soundY), (int) Math.floor(soundZ)))) {
            // The ray will probably hit the block that it's emitting from
            // before
            // escaping. Offset the ray start position towards the player by the
            // diagonal half length of a cube

            tempNormX = playerPos.x - soundX;
            tempNormY = playerPos.y - soundY;
            tempNormZ = playerPos.z - soundZ;
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

        return Vec3f.method_1293(
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
        // TODO: sound category
        if ((EnviroSound.mc.player == null)
                || (EnviroSound.mc.level == null)
                //|| (EnviroSound.lastSoundCategory == SoundCategory.RECORDS)
                //|| (EnviroSound.lastSoundCategory == SoundCategory.MUSIC)
                || EnviroSound.uiPattern.matcher(filename).matches() || EnviroSound.clickPattern.matcher(filename).matches()) {
            if (Config.autoSteroDownmixLogging) {
                EnviroSound.log(
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
            EnviroSound.log(
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
            final float x, final float y, final float z, final int sourceID
            ) {
        if(EnviroSound.lastSoundName != null) {
            EnviroSound.onPlaySound(x, y, z, sourceID, EnviroSound.lastSoundName);
        }
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void onPlaySound(
            final float x, final float y, final float z, final int sourceID, final String soundName
            ) {
         log(String.valueOf(x)+" "+String.valueOf(y)+" "+String.valueOf(z)+" - "+String.valueOf(sourceID)+" - "+soundName);
        // TODO: sound category
        if (Config.noteBlockEnable
            //&& (soundCat == SoundCategory.RECORDS)
            && EnviroSound.noteBlockPattern.matcher(soundName).matches()
        ) {
            //soundCat = SoundCategory.BLOCKS;
        }
        EnviroSound.evaluateEnvironment(
                sourceID, x, y, z, soundName
                );
        /*if (!Config.dynamicEnvironementEvalutaion) {
            return;
        }
        // TODO: sound category
        if (((EnviroSound.mc.player == null)
                || (EnviroSound.mc.level == null) || (y <= 0)
                //|| (soundCat == SoundCategory.RECORDS)
                //|| (soundCat == SoundCategory.MUSIC)
                )
                || (Config.skipRainOcclusionTracing
                        && EnviroSound.rainPattern.matcher(soundName).matches())) {
            return;
        }
        final Source tmp = new Source(
                sourceID, x, y, z, soundName
                );
        EnviroSound.source_check_add(tmp);*/
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    // For sounds that get played using OpenAL directly or just not using the
    // minecraft sound system
    public static void onPlaySoundAL(
            final float x, final float y, final float z, final int sourceID
            ) {
        EnviroSound.onPlaySound(
                x, y, z, sourceID, "openal"
                );
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

    private static Vec3f reflect(final Vec3f dir, final Vec3f normal) {
        final double dot2 = dotProduct(dir, normal) * 2;

        final double x = dir.x - (dot2 * normal.x);
        final double y = dir.y - (dot2 * normal.y);
        final double z = dir.z - (dot2 * normal.z);

        return Vec3f.method_1293(x, y, z);
    }
    
    static float clamp_float(float a, float b, float c) {
        return a < b ? b : (a > c ? c : a);
    }

    public static double dotProduct(Vec3f a, Vec3f b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private static void setEnvironment(
            final int sourceID, final float sendGain0, final float sendGain1, final float sendGain2, final float sendGain3, final float sendCutoff0, final float sendCutoff1, final float sendCutoff2, final float sendCutoff3, final float directCutoff, final float directGain, final float airAbsorptionFactor
            ) {
        // Set reverb send filter values and set source to send to all reverb fx
        // slots
        EFX10.alFilterf(
                EnviroSound.sendFilter0, EFX10.AL_LOWPASS_GAIN, sendGain0
                );
        EFX10.alFilterf(
                EnviroSound.sendFilter0, EFX10.AL_LOWPASS_GAINHF, sendCutoff0
                );
        AL11.alSource3i(
                sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, EnviroSound.auxFXSlot0, 0, EnviroSound.sendFilter0
                );

        EFX10.alFilterf(
                EnviroSound.sendFilter1, EFX10.AL_LOWPASS_GAIN, sendGain1
                );
        EFX10.alFilterf(
                EnviroSound.sendFilter1, EFX10.AL_LOWPASS_GAINHF, sendCutoff1
                );
        AL11.alSource3i(
                sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, EnviroSound.auxFXSlot1, 1, EnviroSound.sendFilter1
                );

        EFX10.alFilterf(
                EnviroSound.sendFilter2, EFX10.AL_LOWPASS_GAIN, sendGain2
                );
        EFX10.alFilterf(
                EnviroSound.sendFilter2, EFX10.AL_LOWPASS_GAINHF, sendCutoff2
                );
        AL11.alSource3i(
                sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, EnviroSound.auxFXSlot2, 2, EnviroSound.sendFilter2
                );

        EFX10.alFilterf(
                EnviroSound.sendFilter3, EFX10.AL_LOWPASS_GAIN, sendGain3
                );
        EFX10.alFilterf(
                EnviroSound.sendFilter3, EFX10.AL_LOWPASS_GAINHF, sendCutoff3
                );
        AL11.alSource3i(
                sourceID, EFX10.AL_AUXILIARY_SEND_FILTER, EnviroSound.auxFXSlot3, 3, EnviroSound.sendFilter3
                );

        EFX10.alFilterf(
                EnviroSound.directFilter0, EFX10.AL_LOWPASS_GAIN, directGain
                );
        EFX10.alFilterf(
                EnviroSound.directFilter0, EFX10.AL_LOWPASS_GAINHF, directCutoff
                );
        AL10.alSourcei(
                sourceID, EFX10.AL_DIRECT_FILTER, EnviroSound.directFilter0
                );

        AL10.alSourcef(
                sourceID, EFX10.AL_AIR_ABSORPTION_FACTOR, clamp_float(
                        Config.airAbsorption * airAbsorptionFactor, 0.0f, 10.0f
                        )
                );
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void setLastSoundName(final String soundName) {
        EnviroSound.lastSoundName = soundName;
    }

    /**
     * Applies the parameters in the enum ReverbParams to the main reverb
     * effect.
     */
    protected static void setReverbParams(
            final ReverbParams r, final int auxFXSlot, final int reverbSlot
            ) {
        EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DENSITY, r.density);
        EnviroSound.checkErrorLog(
                "Error while assigning reverb density: " + r.density
                );

        EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DIFFUSION, r.diffusion);
        EnviroSound.checkErrorLog(
                "Error while assigning reverb diffusion: " + r.diffusion
                );

        EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAIN, r.gain);
        EnviroSound.checkErrorLog(
                "Error while assigning reverb gain: " + r.gain
                );

        EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_GAINHF, r.gainHF);
        EnviroSound.checkErrorLog(
                "Error while assigning reverb gainHF: " + r.gainHF
                );

        EFX10.alEffectf(reverbSlot, EFX10.AL_EAXREVERB_DECAY_TIME, r.decayTime);
        EnviroSound.checkErrorLog(
                "Error while assigning reverb decayTime: " + r.decayTime
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_DECAY_HFRATIO, r.decayHFRatio
                );
        EnviroSound.checkErrorLog(
                "Error while assigning reverb decayHFRatio: " + r.decayHFRatio
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_REFLECTIONS_GAIN, r.reflectionsGain
                );
        EnviroSound.checkErrorLog(
                "Error while assigning reverb reflectionsGain: "
                        + r.reflectionsGain
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_GAIN, r.lateReverbGain
                );
        EnviroSound.checkErrorLog(
                "Error while assigning reverb lateReverbGain: "
                        + r.lateReverbGain
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_LATE_REVERB_DELAY, r.lateReverbDelay
                );
        EnviroSound.checkErrorLog(
                "Error while assigning reverb lateReverbDelay: "
                        + r.lateReverbDelay
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, r.airAbsorptionGainHF
                );
        EnviroSound.checkErrorLog(
                "Error while assigning reverb airAbsorptionGainHF: "
                        + r.airAbsorptionGainHF
                );

        EFX10.alEffectf(
                reverbSlot, EFX10.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, r.roomRolloffFactor
                );
        EnviroSound.checkErrorLog(
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
            EnviroSound.log("EFX Extension recognized.");
        }
        else {
            EnviroSound.logError(
                    "EFX Extension not found on current device. Aborting."
                    );
            return;
        }

        // Create auxiliary effect slots
        EnviroSound.auxFXSlot0 = EFX10.alGenAuxiliaryEffectSlots();
        EnviroSound.log("Aux slot " + EnviroSound.auxFXSlot0 + " created");
        EFX10.alAuxiliaryEffectSloti(
                EnviroSound.auxFXSlot0, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
                );

        EnviroSound.auxFXSlot1 = EFX10.alGenAuxiliaryEffectSlots();
        EnviroSound.log("Aux slot " + EnviroSound.auxFXSlot1 + " created");
        EFX10.alAuxiliaryEffectSloti(
                EnviroSound.auxFXSlot1, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
                );

        EnviroSound.auxFXSlot2 = EFX10.alGenAuxiliaryEffectSlots();
        EnviroSound.log("Aux slot " + EnviroSound.auxFXSlot2 + " created");
        EFX10.alAuxiliaryEffectSloti(
                EnviroSound.auxFXSlot2, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
                );

        EnviroSound.auxFXSlot3 = EFX10.alGenAuxiliaryEffectSlots();
        EnviroSound.log("Aux slot " + EnviroSound.auxFXSlot3 + " created");
        EFX10.alAuxiliaryEffectSloti(
                EnviroSound.auxFXSlot3, EFX10.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE
                );
        EnviroSound.checkErrorLog("Failed creating auxiliary effect slots!");

        EnviroSound.reverb0 = EFX10.alGenEffects();
        EFX10.alEffecti(
                EnviroSound.reverb0, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
                );
        EnviroSound.checkErrorLog("Failed creating reverb effect slot 0!");
        EnviroSound.reverb1 = EFX10.alGenEffects();
        EFX10.alEffecti(
                EnviroSound.reverb1, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
                );
        EnviroSound.checkErrorLog("Failed creating reverb effect slot 1!");
        EnviroSound.reverb2 = EFX10.alGenEffects();
        EFX10.alEffecti(
                EnviroSound.reverb2, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
                );
        EnviroSound.checkErrorLog("Failed creating reverb effect slot 2!");
        EnviroSound.reverb3 = EFX10.alGenEffects();
        EFX10.alEffecti(
                EnviroSound.reverb3, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_EAXREVERB
                );
        EnviroSound.checkErrorLog("Failed creating reverb effect slot 3!");

        // Create filters
        EnviroSound.directFilter0 = EFX10.alGenFilters();
        EFX10.alFilteri(
                EnviroSound.directFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
                );

        EnviroSound.sendFilter0 = EFX10.alGenFilters();
        EFX10.alFilteri(
                EnviroSound.sendFilter0, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
                );

        EnviroSound.sendFilter1 = EFX10.alGenFilters();
        EFX10.alFilteri(
                EnviroSound.sendFilter1, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
                );

        EnviroSound.sendFilter2 = EFX10.alGenFilters();
        EFX10.alFilteri(
                EnviroSound.sendFilter2, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
                );

        EnviroSound.sendFilter3 = EFX10.alGenFilters();
        EFX10.alFilteri(
                EnviroSound.sendFilter3, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS
                );
        EnviroSound.checkErrorLog("Error creating lowpass filters!");

        EnviroSound.applyConfigChanges();
    }

    private static synchronized void setupThread() {
        if (EnviroSound.source_list == null) {
            EnviroSound.source_list = Collections.synchronizedList(
                    new ArrayList<Source>()
                    );
        }
        else {
            EnviroSound.source_list.clear();
        }

        /*if (proc_thread != null) {
            thread_signal_death = false;
            thread_alive = false;
            while (!thread_signal_death);
        }*/
        if (EnviroSound.proc_thread == null) {
            EnviroSound.proc_thread = new ProcThread();
            EnviroSound.thread_alive = true;
            EnviroSound.proc_thread.start();
        }
    }

    public static boolean source_check(final Source s) {
        synchronized (EnviroSound.source_list) {
            final ListIterator<Source> iter = EnviroSound.source_list.listIterator();
            while (iter.hasNext()) {
                final Source sn = iter.next();
                if ((sn.sourceID == s.sourceID) && (sn.bufferID == s.bufferID)
                        && (sn.x == s.x) && (sn.y == s.y)
                        && (sn.z == s.z)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void source_check_add(final Source s) {
        synchronized (EnviroSound.source_list) {
            final ListIterator<Source> iter = EnviroSound.source_list.listIterator();
            while (iter.hasNext()) {
                final Source sn = iter.next();
                if (sn.sourceID == s.sourceID) {
                    sn.x = s.x;
                    sn.y = s.y;
                    sn.z = s.z;
                    sn.name = s.name;
                    return;
                }
            }
            EnviroSound.source_list.add(s);
        }
    }
    
    @Override
    public void preInit() {
        ALC10.alcGetCurrentContext();
        Config.instance.preInit();    
        log("Pre-Init Complete.");
    }
}
