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

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

public class Config {

	public static float airAbsorption;
	public static boolean autoSteroDownmix;

	// misc
	public static boolean autoSteroDownmixLogging;
	private static final String categoryCompatibility = "Compatibility";
	private static final String categoryGeneral = "General";
	private static final String categoryMaterialProperties = "Material properties";
	private static final String categoryMisc = "Misc";
	private static final String categoryPerformance = "Performance";
	public static float clothReflectivity;
	// compatibility
	public static boolean computronicsPatching;
	public static boolean debugInfoShow;
	public static boolean dynamicEnvironementEvalutaion;

	public static int dynamicEnvironementEvalutaionFrequency;
	public static int environmentEvaluationRays;
	public static int environmentEvaluationRaysBounces;
	public static float glassReflectivity;
	public static float globalBlockAbsorption;
	public static float globalBlockReflectance;

	public static float globalReverbBrightness;
	public static float globalReverbGain;
	public static float groundReflectivity;
	public static boolean injectorLogging;
	public static final Config instance;
	public static float metalReflectivity;
	public static boolean noteBlockEnable;
	public static float plantReflectivity;
	// general
	public static float rolloffFactor;

	public static float sandReflectivity;
	public static boolean simplerSharedAirspaceSimulation;

	// performance
	public static boolean skipRainOcclusionTracing;
	public static float snowAirAbsorptionFactor;
	public static float snowReflectivity;

	public static float soundDistanceAllowance;
	// block properties
	public static float stoneReflectivity;
	public static float underwaterFilter;
	public static float woodReflectivity;
	public static float maxDistance;
	static {
		instance = new Config();
	}

	private Configuration forgeConfig;

	private Config() {
	}

	@SuppressWarnings({
			"rawtypes"
	})
	public List<IConfigElement> getConfigElements() {
		final ArrayList<IConfigElement> list = new ArrayList<>();

		list.add(
				new ConfigElement(
						this.forgeConfig.getCategory(Config.categoryGeneral)
						)
				);
		list.add(
				new ConfigElement(
						this.forgeConfig.getCategory(Config.categoryPerformance)
						)
				);
		list.add(
				new ConfigElement(
						this.forgeConfig.getCategory(
								Config.categoryMaterialProperties
								)
						)
				);
		list.add(
				new ConfigElement(
						this.forgeConfig.getCategory(
								Config.categoryCompatibility
								)
						)
				);
		list.add(
				new ConfigElement(
						this.forgeConfig.getCategory(Config.categoryMisc)
						)
				);

		return list;
	}

	public void init(final FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConfigChanged(
			final ConfigChangedEvent.OnConfigChangedEvent eventArgs
			) {
		if (eventArgs.modID.equals(EnviroSound.modid)) {
			syncConfig();
		}
	}

	public void preInit(final FMLPreInitializationEvent event) {
		this.forgeConfig = new Configuration(
				event.getSuggestedConfigurationFile()
				);
		syncConfig();
	}

	private void syncConfig() {
		// General
		Config.rolloffFactor = this.forgeConfig.getFloat(
				"Attenuation Factor", Config.categoryGeneral, 1.0f, 0.2f, 1.0f, "Affects how quiet a sound gets based on distance. Lower values mean distant sounds are louder. 1.0 is the physically correct value."
				);
		Config.globalReverbGain = this.forgeConfig.getFloat(
				"Global Reverb Gain", Config.categoryGeneral, 1.0f, 0.1f, 2.0f, "The global volume of simulated reverberations."
				);
		Config.globalReverbBrightness = this.forgeConfig.getFloat(
				"Global Reverb Brightness", Config.categoryGeneral, 1.0f, 0.1f, 2.0f, "The brightness of reverberation. Higher values result in more high frequencies in reverberation. Lower values give a more muffled sound to the reverb."
				);
		Config.globalBlockAbsorption = this.forgeConfig.getFloat(
				"Global Block Absorption", Config.categoryGeneral, 1.0f, 0.1f, 4.0f, "The global amount of sound that will be absorbed when traveling through blocks."
				);
		Config.globalBlockReflectance = this.forgeConfig.getFloat(
				"Global Block Reflectance", Config.categoryGeneral, 1.0f, 0.1f, 4.0f, "The global amount of sound reflectance energy of all blocks. Lower values result in more conservative reverb simulation with shorter reverb tails. Higher values result in more generous reverb simulation with higher reverb tails."
				);
		Config.soundDistanceAllowance = this.forgeConfig.getFloat(
				"Sound Distance Allowance", Config.categoryGeneral, 4.0f, 1.0f, 6.0f, "Minecraft won't allow sounds to play past a certain distance. This parameter is a multiplier for how far away a sound source is allowed to be in order for it to actually play. Values too high can cause polyphony issues."
				);
		Config.airAbsorption = this.forgeConfig.getFloat(
				"Air Absorption", Config.categoryGeneral, 1.0f, 0.0f, 5.0f, "A value controlling the amount that air absorbs high frequencies with distance. A value of 1.0 is physically correct for air with normal humidity and temperature. Higher values mean air will absorb more high frequencies with distance. 0 disables this effect."
				);
		Config.snowAirAbsorptionFactor = this.forgeConfig.getFloat(
				"Max Snow Air Absorption Factor", Config.categoryGeneral, 5.0f, 0.0f, 10.0f, "The maximum air absorption factor when it's snowing. The real absorption factor will depend on the snow's intensity. Set to 1 or lower to disable"
				);
		Config.underwaterFilter = this.forgeConfig.getFloat(
				"Underwater Filter", Config.categoryGeneral, 0.8f, 0.0f, 1.0f, "How much sound is filtered when the player is underwater. 0.0 means no filter. 1.0 means fully filtered."
				);
		Config.noteBlockEnable = this.forgeConfig.getBoolean(
				"Affect Note Blocks", Config.categoryGeneral, true, "If true, note blocks will be processed."
				);
		Config.maxDistance = this.forgeConfig.getFloat
				("Max ray distance", categoryGeneral, 256.0f, 1.0f, 8192.0f, "How far the rays should be traced.");

		// performance
		Config.skipRainOcclusionTracing = this.forgeConfig.getBoolean(
				"Skip Rain Occlusion Tracing", Config.categoryPerformance, true, "If true, rain sound sources won't trace for sound occlusion. This can help performance during rain."
				);
		Config.environmentEvaluationRays = this.forgeConfig.getInt(
				"Environment Evaluation Rays", Config.categoryPerformance, 32, 8, 64, "The number of rays to trace to determine reverberation for each sound source. More rays provides more consistent tracing results but takes more time to calculate. Decrease this value if you experience lag spikes when sounds play."
				);
		Config.environmentEvaluationRaysBounces = this.forgeConfig.getInt(
				"Environment Evaluation Rays Bounces", Config.categoryPerformance, 4, 1, 64, "The number of rays bounces to determine reverberation for each sound source. More bounces provides more consistent tracing results but takes more time to calculate. Decrease this value if you experience lag spikes when sounds play."
				);
		Config.simplerSharedAirspaceSimulation = this.forgeConfig.getBoolean(
				"Simpler Shared Airspace Simulation", Config.categoryPerformance, false, "If true, enables a simpler technique for determining when the player and a sound source share airspace. Might sometimes miss recognizing shared airspace, but it's faster to calculate."
				);
		Config.dynamicEnvironementEvalutaion = this.forgeConfig.getBoolean(
				"Dynamic environment evaluation", Config.categoryPerformance, false, "WARNING it's implemented really badly so i'd recommend not always using it.If true, the environment will keep getting evaluated for every sound that is currently playing. This may affect performance"
				);
		Config.dynamicEnvironementEvalutaionFrequency = this.forgeConfig.getInt(
				"Frequency of environment evaluation", Config.categoryPerformance, 30, 1, 60, "The frequency at witch to update environment of sounds if dynamic environment evaluation is enabled"
				);

		// material properties
		Config.stoneReflectivity = this.forgeConfig.getFloat(
				"Stone Reflectivity", Config.categoryMaterialProperties, 0.95f, 0.0f, 1.0f, "Sound reflectivity for stone blocks."
				);
		Config.woodReflectivity = this.forgeConfig.getFloat(
				"Wood Reflectivity", Config.categoryMaterialProperties, 0.7f, 0.0f, 1.0f, "Sound reflectivity for wooden blocks."
				);
		Config.groundReflectivity = this.forgeConfig.getFloat(
				"Ground Reflectivity", Config.categoryMaterialProperties, 0.3f, 0.0f, 1.0f, "Sound reflectivity for ground blocks (dirt, gravel, etc)."
				);
		Config.plantReflectivity = this.forgeConfig.getFloat(
				"Foliage Reflectivity", Config.categoryMaterialProperties, 0.2f, 0.0f, 1.0f, "Sound reflectivity for foliage blocks (leaves, grass, etc.)."
				);
		Config.metalReflectivity = this.forgeConfig.getFloat(
				"Metal Reflectivity", Config.categoryMaterialProperties, 0.97f, 0.0f, 1.0f, "Sound reflectivity for metal blocks."
				);
		Config.glassReflectivity = this.forgeConfig.getFloat(
				"Glass Reflectivity", Config.categoryMaterialProperties, 0.5f, 0.0f, 1.0f, "Sound reflectivity for glass blocks."
				);
		Config.clothReflectivity = this.forgeConfig.getFloat(
				"Cloth Reflectivity", Config.categoryMaterialProperties, 0.25f, 0.0f, 1.0f, "Sound reflectivity for cloth blocks (carpet, wool, etc)."
				);
		Config.sandReflectivity = this.forgeConfig.getFloat(
				"Sand Reflectivity", Config.categoryMaterialProperties, 0.2f, 0.0f, 1.0f, "Sound reflectivity for sand blocks."
				);
		Config.snowReflectivity = this.forgeConfig.getFloat(
				"Snow Reflectivity", Config.categoryMaterialProperties, 0.2f, 0.0f, 1.0f, "Sound reflectivity for snow blocks."
				);

		// compatibility
		Config.computronicsPatching = this.forgeConfig.getBoolean(
				"Patch Computronics", Config.categoryCompatibility, true, "MAY REQUIRE RESTART.If true, patches the computronics sound sources so it works with sound physics."
				);
		Config.autoSteroDownmix = this.forgeConfig.getBoolean(
				"Auto stereo downmix", Config.categoryCompatibility, true, "MAY REQUIRE RESTART.If true, Automatically downmix stereo sounds that are loaded to mono"
				);

		// misc
		Config.autoSteroDownmixLogging = this.forgeConfig.getBoolean(
				"Stereo downmix Logging", Config.categoryMisc, false, "If true, Prints sound name and format of the sounds that get converted"
				);
		Config.debugInfoShow = this.forgeConfig.getBoolean(
				"Dynamic env. info in F3", Config.categoryMisc, false, "If true, Shows sources currently playing in the F3 debug info"
				);
		Config.injectorLogging = this.forgeConfig.getBoolean(
				"Injector Logging", Config.categoryMisc, false, "If true, Logs debug info about the injector"
				);

		EnviroSound.applyConfigChanges();
		if (this.forgeConfig.hasChanged()) {
			this.forgeConfig.save();
		}
	}

}
