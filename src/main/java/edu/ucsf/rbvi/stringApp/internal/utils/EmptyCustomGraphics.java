package edu.ucsf.rbvi.stringApp.internal.utils;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.Image;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;

/**
 * Null object for Custom Graphics. This is used to reset custom graphics on node views.
 */
public class EmptyCustomGraphics implements CyCustomGraphics<CustomGraphicLayer> {
	
	// Human readable name of this null object.
	private static final String NAME = "[ Remove Graphics ]";

	private Long id = null;
	private int width = 0;
	private int height = 0;
	private float fitRatio = 0.0f;
	private String displayName = "Empty";

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public void setDisplayName(String dn) {displayName = dn;}

	@Override
	public float getFitRatio() {
		return fitRatio;
	}

	@Override
	public void setFitRatio(float fr) { this.fitRatio = fr; }

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {this.height = height;};

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {this.width = width;};

	@Override
	public Long getIdentifier() { return id; }

	@Override
	public void setIdentifier(Long id) { this.id = id; }

	@Override
	public List<CustomGraphicLayer> getLayers(CyNetworkView view, View<? extends CyIdentifiable> grView) {
		return new ArrayList<>();
	}

	@Override
	public String toString() {
		return "Empty";
	}

	@Override
	public Image getRenderedImage() {
		return null;
	}

	@Override
	public String toSerializableString() {
		return "Empty";
	}
}
