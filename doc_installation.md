# stringApp Installation

stringApp is distributed through the [Cytoscape App Store](https://apps.cytoscape.org/apps/stringapp). To install it, open _Apps -> App Store -> Show App Store_ in Cytoscape, search for stringApp and press _Install_.

To install a developer build instead:
- Build the jar with `mvn clean package` (see the [README](README.md#building)); it is written to `target/stringApp-<version>.jar`.
- Start Cytoscape and open the _App Manager_ (go to the menu _Apps -> App Manager_). 
- Press the button _Install from File_ (bottom left corner), navigate to the jar file and press _Open_.
