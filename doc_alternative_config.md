# stringApp Documentation

stringApp version 2.0.2

Last update: 2023-08-03

## Setting up an alternative STRING version

Here we will explain how you can use a different STRING version than the default one used by stringApp. 

First, check if the stringApp.props file exists in your ~/CytoscapeConfiguration/ directory. If it does not continue with [Get a stringApp properties file](#get-a-stringApp-properties-file), otherwise go to [Modify an existing stringApp properties file](#modify-an-existing-stringApp-properties-file). 

### Get a stringApp properties file
- Quit Cytoscape if it is running
- Download the stringApp.props file from [here](stringApp.props) and place it in ~/CytoscapeConfiguration/
- Start Cytoscape


### Modify an existing stringApp properties file
- Quit Cytoscape if it is running.
- Open the stringApp.props file in ~/CytoscapeConfiguration/ 
- Add the following line to it: 
`alternativeCONFIGURI=https\://jensenlab.org/assets/stringapp/string_app_v2_0_0_string11.5.json`
- Save the file
- Start Cytoscape

