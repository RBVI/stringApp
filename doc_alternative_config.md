# stringApp Documentation

stringApp version 2.1.1

Last update: 2024-10-30


## Access to STRING v11.5 discontinued

As of today, 30 Oct 2024, stringApp will no longer have access to STRING v11.5. If you really need to work with STRING v11.5 networks, you can do that via the [web interface](https://version-11-5.string-db.org/), download the data and import it in Cytoscape as _Network from file_. 

## Setting up an alternative STRING version

Here we will explain how you can use a different STRING version than the default one used by stringApp. 

First, check if the stringApp.props file exists in your ~/CytoscapeConfiguration/ directory. If it does not continue with [Get a stringApp properties file](#get-a-stringApp-properties-file), otherwise go to [Modify an existing stringApp properties file](#modify-an-existing-stringApp-properties-file). 

### Get a stringApp properties file
- Quit Cytoscape if it is running
- Download the stringApp.props file from [here](./stringApp.props) and place it in ~/CytoscapeConfiguration/
- Start Cytoscape again


### Modify an existing stringApp properties file
- Quit Cytoscape if it is running.
- Open the stringApp.props file in ~/CytoscapeConfiguration/ 
- Add the following line to it: `alternativeCONFIGURI=https\://jensenlab.org/assets/stringapp/string_app_v2_0_0_string11.5.json`
- Save the file
- Start Cytoscape again
- Make sure that the right version is used by checking the messages in the Cytoscape Task History (icon in the bottom left corner)
