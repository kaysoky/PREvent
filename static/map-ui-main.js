//TODO: MOVING AVERAGE
//TODO: SORT DATA
//TODO: FILTER OUT MAP DATA SO IT DOESN"T LOOK CONCENTRATED IF YOU"RE SITTING SOMEWHERE ALL DAY
// Fetch data from the server
var temp = [];
var humi = [];
var part = [];
var vocs = [];
var testChart = [];
var humiTestChart = [];
var partTestChart = [];
var vocsTestChart = [];
var dateChart = [];
var fakeLabels = [];
var tempByHour = {};
var humiByHour = {};
var partByHour = {};
var vocsByHour = {};
function fetchData() {
    $.get('/data/').done(function(data) {
        for (var index in data) {
            var coord = new google.maps.LatLng(data[index].ycoord, data[index].xcoord);
            var date = new Date(data[index].timestamp);
            var hour = date.getHours();
            tempByHour[hour] = tempByHour[hour]||[];
            tempByHour[hour].push(data[index].temperature);
            humiByHour[hour] = humiByHour[hour]||[];
            humiByHour[hour].push(data[index].humidity);
            partByHour[hour] = partByHour[hour]||[];
            partByHour[hour].push(data[index].particulate);
            vocsByHour[hour] = vocsByHour[hour]||[];
            vocsByHour[hour].push(data[index].gas);
            dateChart.push(date);
            fakeLabels.push(intervalDate(date, index, 25));
            temp.push({ location: coord, weight: data[index].temperature });
            humi.push({ location: coord, weight: data[index].humidity });
            part.push({ location: coord, weight: data[index].particulate });
            vocs.push({ location: coord, weight: data[index].gas });
            testChart.push(data[index].temperature);
            humiTestChart.push(data[index].humidity);
            partTestChart.push(data[index].particulate);
            vocsTestChart.push(data[index].gas);
        }
        averages();
        initialize();
        var hourTicks = Object.keys(tempByHour);
        tempHour = group(tempByHour);
        humiHour = group(humiByHour);
        partHour = group(partByHour);
        vocsHour = group(vocsByHour);

        data1 = makeGraphs(testChart, fakeLabels);
        data2 = makeGraphs(humiTestChart, fakeLabels);
        data3 = makeGraphs(partTestChart, fakeLabels);
        data4 = makeGraphs(vocsTestChart, fakeLabels);
        data5 = makeFourGraphs(tempHour, humiHour, partHour, vocsHour, hourTicks);
        var tC = document.getElementById('tempChart').getContext("2d");
        var hC = document.getElementById('humiChart').getContext("2d");
        var pC = document.getElementById('partChart').getContext("2d");
        var vC = document.getElementById('vocsChart').getContext("2d");
        var tHH = document.getElementById('hourChart').getContext("2d");
        var tempChartz = new Chart(tC).Line(data1)
        var humiChartz = new Chart(hC).Line(data2, {showXLabels: 10});
        var partChartz = new Chart(pC).Line(data3)
        var vocsChartz = new Chart(vC).Line(data4)
        var hourChartz = new Chart(tHH).Line(data5);	
    });
}

function intervalDate(date, index, num) {
    if (index % num == 0) {
        var hour = date.getHours();
        var minute = date.getMinutes();
        var sec = date.getSeconds();
        var d = hour + ':' + minute + ':' + sec;
        return d
    } else {
        return ''
    }
}

function averages()	{
    var tempMean = 0;
    var humiMean = 0;
    var partMean = 0;
    var vocsMean = 0;
    for(var i = 0; i < temp.length; i++) {
        tempMean = tempMean + temp[i]['weight']
        humiMean = humiMean + humi[i]['weight']
        partMean = partMean + part[i]['weight']
        vocsMean = vocsMean + vocs[i]['weight']
    }
    tempMean = tempMean/temp.length
    humiMean = humiMean/temp.length
    partMean = partMean/temp.length
    vocsMean = vocsMean/temp.length
    var data = {
        labels: ["Temperature", "Humidity", "Particulate Matter", "Volatile Organic Compound"],
        datasets: [
            {
                label: "Graphs",
                fillColor: "rgba(220,220,220,0.5)",
                strokeColor: "rgba(220,220,220,0.8)",
                highlightFill: "rgba(220,220,220,0.75)",
                highlightStroke: "rgba(220,220,220,1)",
                data: [tempMean, humiMean, partMean, vocsMean]
            }
        ]
    };
    var ctx = document.getElementById("barChart").getContext("2d");
    var myBarChart = new Chart(ctx).Bar(data);
};	        

function group(dict) {
    var values = [];
    for(var key in dict) {
        values.push(ave(dict[key]));
    }
    return values
}

function ave(array) {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
        sum = sum + array[i];
    }
    sum = sum/array.length;
    return sum
}


var map;
var tempHeatmap;
var humiHeatmap;
var partHeatmap;
var vocsHeatmap;
function initialize() {
    var mapOptions = {
        zoom: 9,
        // TEMP: Center map over Seattle
        center: new google.maps.LatLng(47.450580, -122.307496),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
  
    // Initialize Google Maps layer
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);

    tempHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(temp)
    });
    humiHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(humi)
    });
    partHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(part)
    });
    vocsHeatmap = new google.maps.visualization.HeatmapLayer({
        data: new google.maps.MVCArray(vocs)
    });
}

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

google.maps.event.addDomListener(window, 'load', fetchData);

function makeGraphs(data1, dateChart, name) {
    var data = {
        labels: dateChart,
        datasets: [
            {
                label: name,
                fillColor: "rgba(220,220,220,0.4)",
                strokeColor: "rgba(220,100,120,50)",
                pointColor: "rgba(90,80,220,40)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(220,220,220,1)",
                data: data1
            }
        ]
    }	
    return data		    
};

function makeFourGraphs(data1, data2, data3, data4, dateChart) {
    var data = {
        labels: dateChart,
        datasets: [
            {
                label: "Temperature",
                fillColor: "rgba(220,220,220,0.2)",
                strokeColor: "rgba(220,220,220,1)",
                pointColor: "rgba(220,220,220,1)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(220,220,220,1)",
                data: data1
            },
            {
                label: "",
                fillColor: "rgba(151,187,205,0.2)",
                strokeColor: "rgba(151,187,205,1)",
                pointColor: "rgba(151,187,205,1)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(151,187,205,1)",
                data: data2
            }, 
            {
                label: "",
                fillColor: "rgba(151,187,205,0.2)",
                strokeColor: "rgba(208,0,0,1)",
                pointColor: "rgba(151,187,205,1)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(151,187,205,1)",
                data: data3
            },
            {
                label: "",
                fillColor: "rgba(151,187,205,0.2)",
                strokeColor: "rgba(200,100,115,1)",
                pointColor: "rgba(151,187,205,1)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(178,190,220,1)",
                data: data4
            }

        ]
    }	
    return data		    
};
