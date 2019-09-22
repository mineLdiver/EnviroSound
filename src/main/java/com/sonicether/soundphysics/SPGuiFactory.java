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

import java.util.Set;

import cpw.mods.fml.client.IModGuiFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class SPGuiFactory implements IModGuiFactory {

	@Override
	public RuntimeOptionGuiHandler getHandlerFor(
			final RuntimeOptionCategoryElement element
			) {
		return null;
	}

	@Override
	public void initialize(final Minecraft minecraftInstance) {
	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return SPGuiConfig.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}
}
