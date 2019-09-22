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

import cpw.mods.fml.client.config.GuiConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class SPGuiConfig extends GuiConfig {

	public SPGuiConfig(final GuiScreen parent) {
		super(
				parent, Config.instance.getConfigElements(), SoundPhysics.modid, false, false, "Sound Physics Configuration"
				);
	}

	@Override
	protected void actionPerformed(final GuiButton button) {
		super.actionPerformed(button);
	}

	@Override
	public void drawScreen(
			final int mouseX, final int mouseY, final float partialTicks
			) {
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void initGui() {
		super.initGui();
	}
}
