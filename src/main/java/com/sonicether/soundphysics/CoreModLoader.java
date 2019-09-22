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
 * Copyright (c) 2017, 2018 AlkCorp.
 * Contributors: https://github.com/alkcorp/Sound-Physics/graphs/contributors
 *******************************************************************************/
package com.sonicether.soundphysics;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;

@MCVersion(value = SoundPhysics.mcVersion)
public class CoreModLoader implements IFMLLoadingPlugin {

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[]{
				CoreModInjector.class.getName()
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(final Map<String, Object> data) {
	}

}
