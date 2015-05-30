var map;
var tempHeatmap;
var humiHeatmap;
var partHeatmap;
var vocsHeatmap;
var lastFetchedData;
var tempData = new google.maps.MVCArray();
var humiData = new google.maps.MVCArray();
var partData = new google.maps.MVCArray();
var vocsData = new google.maps.MVCArray();

function loadUI() {
    var gradient = [ 'rgba(0, 255, 0, 0)', 'green', 'yellow', 'red' ];
    var radius = 25;

    // Initialize the data arrays and map layers
    tempHeatmap = new google.maps.visualization.HeatmapLayer({
        data: tempData,
        maxIntensity: 40,
        gradient: gradient,
        radius: radius
    });
    humiHeatmap = new google.maps.visualization.HeatmapLayer({
        data: humiData,
        maxIntensity: 100,
        gradient: gradient,
        radius: radius
    });
    partHeatmap = new google.maps.visualization.HeatmapLayer({
        data: partData,
        maxIntensity: 100,
        gradient: gradient,
        radius: radius
    });
    vocsHeatmap = new google.maps.visualization.HeatmapLayer({
        data: vocsData,
        maxIntensity: 100,
        gradient: gradient,
        radius: radius
    });

    // Construct a legend for the heatmap
    var gradientCss = '(top';
    for (var i = 0; i < gradient.length; ++i) {
        gradientCss += ', ' + gradient[i];
    }
    gradientCss += ')';
    $('#legendGradient').css('background', '-webkit-linear-gradient' + gradientCss);
    $('#legendGradient').css('background', '-moz-linear-gradient' + gradientCss);
    $('#legendGradient').css('background', '-o-linear-gradient' + gradientCss);
    $('#legendGradient').css('background', 'linear-gradient' + gradientCss);

    FetchData('', InitializeHeatmap);
}

function InitializeHeatmap(center) {
    var mapOptions = {
        zoom: 15, // Level at which some landmarks and streets are visible
        center: center,
        mapTypeId: google.maps.MapTypeId.ROADMAP
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
    $.get('/data/?' + query + (query.length > 0 ? '&' : '') + 'o=timestamp').done(function(data) {
        lastFetchedData = data;

        callback(ParseData(data));
        RefreshCharts();
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
            aggregateData[key] = $.extend({}, data[index]);
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

    // Add a point (off the render-able area) to bias the data scaling
    var coord = new google.maps.LatLng(90, 0);
    tempData.push({ location: coord, weight: 40 });
    humiData.push({ location: coord, weight: 100 });
    partData.push({ location: coord, weight: 100 });
    vocsData.push({ location: coord, weight: 100 });

    return new google.maps.LatLng(averageY / countGeo, averageX / countGeo);
}

/**
 * Remakes timeline and histogram
 */
function RefreshCharts(key) {
    // Figure out which sensor display is active
    if (!key) {
        switch ($('#sensor-type-buttons').children('.active').text()) {
            case "Particulates":
                key = 'particulate';
                break;
            case "Volatile Organics":
                key = 'gas';
                break;
            case "Temperature":
                key = 'temperature';
                break;
            case "Humidity":
                key = 'humidity';
                break;
        }
    }

    // Coerce data into D3-accessible values
    var parseDate = d3.time.format.iso.parse;
    lastFetchedData.forEach(function(d) {
        d.date = parseDate(d.timestamp);
        d[key] = +d[key];
    });

    BuildTimeline(lastFetchedData, key);
    BuildHistogram(lastFetchedData, key);
}

/**
 * Builds the D3 timeline
 */
function BuildTimeline(data, key) {
    // Determine the bounds of the graph
    var margin = {top: 20, right: 20, bottom: 30, left: 50};
    var width = $('#timeline').width() - margin.left - margin.right;
    var height = $('#timeline').height() - margin.top - margin.bottom;

    // Set up the axes
    var x = d3.time.scale().range([0, width]);
    var y = d3.scale.linear().range([height, 0]);
    var xAxis = d3.svg.axis().scale(x).orient("bottom");
    var yAxis = d3.svg.axis().scale(y).orient("left");

    // Initialize the area plot
    var area = d3.svg.area()
        .x(function(d) { return x(d.date); })
        .y0(height)
        .y1(function(d) { return y(d[key]); });

    // Build the chart area
    d3.select("#timeline").select('svg').remove();
    var svg = d3.select("#timeline").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // Determine the scope of the data
    x.domain(d3.extent(data, function(d) { return d.date; }));
    y.domain(d3.extent(data, function(d) { return d[key]; }));

    // Label the axes
    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);
    svg.append("g")
        .attr("class", "y axis")
        .call(yAxis)
        .append("text")
            .attr("transform", "rotate(-90)")
            .attr("y", 6)
            .attr("dy", ".71em")
            .style("text-anchor", "end")
            .text("Sensor Value");

    // Add the data
    svg.append("path")
        .attr("class", "area")
        .attr("d", area(data));
}

/**
 * Builds the D3 histogram
 */
function BuildHistogram(values, key) {
    // A formatter for counts.
    var formatCount = d3.format(",.0f");

    // Determine the bounds of the graph
    var margin = {top: 10, right: 30, bottom: 30, left: 30};
    var width = $('#histogram').width() - margin.left - margin.right;
    var height = $('#histogram').height() - margin.top - margin.bottom;

    // Set up the X axis
    var x = d3.scale.linear()
        .domain(d3.extent(values, function(d) { return d[key]; }))
        .range([0, width]);
    var xAxis = d3.svg.axis()
        .scale(x)
        .orient("bottom");

    // Generate a histogram using uniformly-spaced bins
    var data = d3.layout.histogram()
        .value(function(d) {return d[key]; })
        .bins(x.ticks(20))
        (values);

    // Set up the Y axis
    var y = d3.scale.linear()
        .domain([0, d3.max(data, function(d) { return d.y; })])
        .range([height, 0]);

    // Build the chart area
    d3.select("#histogram").select('svg').remove();
    var svg = d3.select("#histogram").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    var bar = svg.selectAll(".bar")
        .data(data)
        .enter().append("g")
            .attr("class", "bar")
            .attr("transform", function(d) { return "translate(" + x(d.x) + "," + y(d.y) + ")"; });

    bar.append("rect")
        .attr("x", 1)
        .attr("width", x(data[0].x + data[0].dx) - 1)
        .attr("height", function(d) { return height - y(d.y); });

    bar.append("text")
        .attr("dy", ".75em")
        .attr("y", 6)
        .attr("x", x(data[0].x + data[0].dx) / 2)
        .attr("text-anchor", "middle")
        .text(function(d) { return formatCount(d.y); });

    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);
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

    RefreshCharts('temperature');

    $('#heat_type_temp').addClass('active');
    $('#heat_type_PM, #heat_type_VOC, #heat_type_humi').removeClass('active');
}
function toggleHumiMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(map);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(null);

    RefreshCharts('humidity');

    $('#heat_type_humi').addClass('active');
    $('#heat_type_PM, #heat_type_VOC, #heat_type_temp').removeClass('active');
}
function togglePartMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(map);
    vocsHeatmap.setMap(null);

    RefreshCharts('particulate');

    $('#heat_type_PM').addClass('active');
    $('#heat_type_VOC, #heat_type_temp, #heat_type_humi').removeClass('active');
}
function toggleVocsMap() {
    tempHeatmap.setMap(null);
    humiHeatmap.setMap(null);
    partHeatmap.setMap(null);
    vocsHeatmap.setMap(map);

    RefreshCharts('gas');

    $('#heat_type_VOC').addClass('active');
    $('#heat_type_PM, #heat_type_temp, #heat_type_humi').removeClass('active');
}
