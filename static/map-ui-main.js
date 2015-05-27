function loadUI() {
    fetchData();
}

var aggregateData = {};
var tempData = [];
var humiData = [];
var partData = [];
var vocsData = [];

/**
 * Grabs data and places it in some script-wide variables
 */
function fetchData() {
    $.get('/data/').done(function(data) {
        // Repackage data into four separate arrays
        for (var index in data) {
            var coord = new google.maps.LatLng(data[index].ycoord, data[index].xcoord);
            tempData.push({ location: coord, weight: data[index].temperature });
            humiData.push({ location: coord, weight: data[index].humidity });
            partData.push({ location: coord, weight: data[index].particulate });
            vocsData.push({ location: coord, weight: data[index].gas });
        }

        // Initialize the heatmap
        initializeHeatmap();
    });
}

/**
 * Helper for transforming coordinates to a string index
 * The resolution for differentiating coordinates is 0.01
 */
function calculateGeoString(data) {
    return Math.round(data.ycoord * 100) + ',' + Math.round(data.xcoord * 100);
}

/**
 * Helper for folding data into an aggregate
 */
function aggregateDataTogether(data) {
    var key = calculateGeoString(data);
    if (key in aggregateData) {
        aggregateData[key].temperature += data.temperature;
        aggregateData[key].humidity    += data.humidity   ;
        aggregateData[key].particulate += data.particulate;
        aggregateData[key].gas         += data.gas        ;
        aggregateData[key].count += 1;
    } else {
        aggregateData[key] = data;
        aggregateData[key].count = 1;
    }
}

var map;
var tempHeatmap;
var humiHeatmap;
var partHeatmap;
var vocsHeatmap;

/**
 * Initializes the heatmap with data held in variables declared above
 */
function initializeHeatmap() {
    var mapOptions = {
        zoom: 14, // Level at which some landmarks and streets are visible
        center: new google.maps.LatLng(47.450580, -122.307496),
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
