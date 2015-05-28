function loadUI() {
    FetchData();
}

var aggregateData = {};
var tempData = [];
var humiData = [];
var partData = [];
var vocsData = [];

/**
 * Grabs data and places it in some script-wide variables
 */
function FetchData() {
    $.get('/data/').done(function(data) {
        InitializeHeatmap(ParseData(data));
    });
}

// The resolution for differentiating coordinates is 0.01
var GEO_AGGREGATION_RESOLUTION = 1000;

/**
 * Helper for folding data into an aggregate
 * And filling in the four separate data arrays
 */
function ParseData(data) {
    // Repackage data into four separate arrays
    aggregateData = {};
    for (var index in data) {
        var key = Math.round(data[index].ycoord * GEO_AGGREGATION_RESOLUTION) + ',' 
                + Math.round(data[index].xcoord * GEO_AGGREGATION_RESOLUTION);
        if (key in aggregateData) {
            aggregateData[key].temperature += data[index].temperature;
            aggregateData[key].humidity    += data[index].humidity   ;
            aggregateData[key].particulate += data[index].particulate;
            aggregateData[key].gas         += data[index].gas        ;
            aggregateData[key].count += 1;
        } else {
            aggregateData[key] = data[index];
            aggregateData[key].count = 1;
        }
    }
    
    var averageX = 0;
    var averageY = 0;
    var countGeo = 0;
    for (var key in aggregateData) {
        var coord = new google.maps.LatLng(aggregateData[key].ycoord, aggregateData[key].xcoord);
        tempData.push({ location: coord, weight: aggregateData[key].temperature / aggregateData[key].count });
        humiData.push({ location: coord, weight: aggregateData[key].humidity    / aggregateData[key].count });
        partData.push({ location: coord, weight: aggregateData[key].particulate / aggregateData[key].count });
        vocsData.push({ location: coord, weight: aggregateData[key].gas         / aggregateData[key].count });
        
        averageX += aggregateData[key].xcoord;
        averageY += aggregateData[key].ycoord;
        countGeo++;
    }
    
    return new google.maps.LatLng(averageY / countGeo, averageX / countGeo);
}

var map;
var tempHeatmap;
var humiHeatmap;
var partHeatmap;
var vocsHeatmap;

/**
 * Initializes the heatmap with data held in variables declared above
 */
function InitializeHeatmap(center) {
    var mapOptions = {
        zoom: 15, // Level at which some landmarks and streets are visible
        center: center, 
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    // Initialize Google Maps layer
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);

    tempHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(tempData)
    });
    humiHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(humiData)
    });
    partHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(partData)
    });
    vocsHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(vocsData)
    });
}

/***** Heatmap button handlers *****/

function toggleTempMap() {
    tempHeatmap.setMap(map);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(null);
}
function toggleHumiMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(map);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(null);
}
function togglePartMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(map);
    vocsHeatmap.setMap(null);
}
function toggleVocsMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(map);
}
