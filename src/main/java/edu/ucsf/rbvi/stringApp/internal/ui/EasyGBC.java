// WikiPathways App for Cytoscape
//
// Copyright 2013-2014 WikiPathways
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.GridBagConstraints;
import java.util.Map;
import java.util.HashMap;

/**
 * Uses the  builder pattern for filling fields of a {@link GridBagConstraints}.
 */
class EasyGBC extends GridBagConstraints {
	static final Map<String,Integer> anchors = new HashMap<String,Integer>();
	static {
		anchors.put("north",     NORTH);
		anchors.put("northwest", NORTHWEST);
		anchors.put("northeast", NORTHEAST);
		anchors.put("west",      WEST);
		anchors.put("south",     SOUTH);
		anchors.put("east",      EAST);
	}
	
	public EasyGBC() {
		reset();
	}

	public EasyGBC reset() {
		gridx = 0;			gridy = 0;
		gridwidth = 1;		gridheight = 1;
		weightx = 0.0;		weighty = 0.0;
		fill = GridBagConstraints.NONE;
		insets.set(0, 0, 0, 0);
		return this;
	}

	public EasyGBC noExpand() {
		weightx = 0.0;
		weighty = 0.0;
		fill = GridBagConstraints.NONE;
		return this;
	}

	public EasyGBC expandVert() {
		weightx = 0.0;
		weighty = 1.0;
		fill = GridBagConstraints.VERTICAL;
		return this;
	}

	public EasyGBC expandHoriz() {
		weightx = 1.0;
		weighty = 0.0;
		fill = GridBagConstraints.HORIZONTAL;
		return this;
	}

	public EasyGBC expandBoth() {
		weightx = 1.0;
		weighty = 1.0;
		fill = GridBagConstraints.BOTH;
		return this;
	}

	public EasyGBC right() {
		gridx++;
		return this;
	}

	public EasyGBC left() {
		gridx--;
		return this;
	}

	public EasyGBC down() {
		gridx = 0;
		gridy++;
		return this;
	}

	public EasyGBC position(int x, int y) {
		gridx = x;
		gridy = y;
		return this;
	}

	public EasyGBC noSpan() {
		gridwidth = 1;
		gridheight = 1;
		return this;
	}

	public EasyGBC spanHoriz(int n) {
		gridwidth = n;
		gridheight = 1;
		return this;
	}

	public EasyGBC insets(int t, int l, int b, int r) {
		insets.set(t, l, b, r);
		return this;
	}

	public EasyGBC noInsets() {
		insets.set(0, 0, 0, 0);
		return this;
	}

	public EasyGBC anchor(String str) {
		anchor = anchors.get(str);
		return this;
	}
}
