var map;
var tempHeatmap;
var humiHeatmap;
var partHeatmap;
var vocsHeatmap;
var tempData = new google.maps.MVCArray();
var humiData = new google.maps.MVCArray();
var partData = new google.maps.MVCArray();
var vocsData = new google.maps.MVCArray();

function loadUI() {
    // Initialize the data arrays and map layers
    tempHeatmap = new google.maps.visualization.HeatmapLayer({
        data: tempData
    });
    humiHeatmap = new google.maps.visualization.HeatmapLayer({
        data: humiData
    });
    partHeatmap = new google.maps.visualization.HeatmapLayer({
        data: partData
    });
    vocsHeatmap = new google.maps.visualization.HeatmapLayer({
        data: vocsData
    });
    
    FetchData('', InitializeHeatmap);
}

function InitializeHeatmap(center) {
    var mapOptions = {
        zoom: 15, // Level at which some landmarks and streets are visible
        center: center,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        maxIntensity: 100
    };

    // Initialize Google Maps layer
    map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
    
    // Default to PM display
    togglePartMap();
}

// Semaphore to prevent concurrency messiness
var isWorking = false;

/**
 * Grabs data and places it in some script-wide variables
 */
function FetchData(query, callback) {
    if (isWorking) return;
    
    isWorking = true;
    $.get('/data/?' + query).done(function(data) {
        callback(ParseData(data));
        isWorking = false;
    });
}

// The resolution for differentiating coordinates is 0.01
var GEO_AGGREGATION_RESOLUTION = 1000;

/**
 * Helper for folding data into an aggregate
 * And filling in the four separate data arrays
 */
function ParseData(data) {
    // Clear all existing data
    tempData.clear();
    humiData.clear();
    partData.clear();
    vocsData.clear();
    
    // Repackage data into four separate arrays
    var aggregateData = {};
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

    // Calculate an average coordinate (only used when initializing the map)
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

/***** Heatmap button handlers *****/

function TimeToQueryBody(time) {
    return time.getFullYear() 
        + '-' + (time.getMonth() + 1)
        + '-' + time.getDate() 
        + ' ' + time.getHours()
        + ':' + time.getMinutes()
        + ':' + time.getSeconds();
}
$('#heat_time_day').click(function () {
    if (isWorking || $(this).hasClass('active')) return;

    var time = new Date();
    time.setDate(time.getDate() - 1);
    FetchData('after=' + TimeToQueryBody(time), function() {});
    
    $(this).addClass('active');
    $('#heat_time_week, #heat_time_all').removeClass('active');
});
$('#heat_time_week').click(function () {
    if (isWorking || $(this).hasClass('active')) return;

    var time = new Date();
    time.setDate(time.getDate() - 7);
    FetchData('after=' + TimeToQueryBody(time), function() {});

    $(this).addClass('active');
    $('#heat_time_day, #heat_time_all').removeClass('active');
});
$('#heat_time_all').click(function () {
    if (isWorking || $(this).hasClass('active')) return;

    var time = new Date();
    time.setDate(time.getDate() - 1);
    FetchData('', function() {});

    $(this).addClass('active');
    $('#heat_time_day, #heat_time_week').removeClass('active');
});

$('#heat_type_PM').click(togglePartMap);
$('#heat_type_VOC').click(toggleVocsMap);
$('#heat_type_temp').click(toggleTempMap);
$('#heat_type_humi').click(toggleHumiMap);

function toggleTempMap() {
    tempHeatmap.setMap(map);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(null);
    
    $('#heat_type_temp').addClass('active');
    $('#heat_type_PM, #heat_type_VOC, #heat_type_humi').removeClass('active');
}
function toggleHumiMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(map);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(null);
    
    $('#heat_type_humi').addClass('active');
    $('#heat_type_PM, #heat_type_VOC, #heat_type_temp').removeClass('active');
}
function togglePartMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(map);
    vocsHeatmap.setMap(null);
    
    $('#heat_type_PM').addClass('active');
    $('#heat_type_VOC, #heat_type_temp, #heat_type_humi').removeClass('active');
}
function toggleVocsMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(map);
    
    $('#heat_type_VOC').addClass('active');
    $('#heat_type_PM, #heat_type_temp, #heat_type_humi').removeClass('active');
}
