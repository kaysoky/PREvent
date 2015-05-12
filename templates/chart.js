
function makeGraphs(data) {
	var data = data;
	var dateFormat = d3.time.format("%Y-%m-%d");
	data.forEach(function(d) {
		d["timestamp"] = dateFormat.parse(d["date_posted"])
	})

	var ndx = crossfilter(data);
	var tempDim = ndx.dimension(function(d) { return d["temperature"]; });
	var dateDim = ndx.dimension(function(d) { return d["timestamp"]; });
	var gasDim  = ndx.dimension(function(d) { return d["gas"]; });

	var numData = dateDim.group()
	var minDate = dateDim.bottom(1)[0]["timestamp"];
	var maxDate = dateDim.top(1)[0]["timestamp"];

	var timeChart = dc.barChart("#time-chart");
	var gasND = dc.numberDisplay("#number-gas");

	gasND
		.formatNumber(d3.format("d"))
		.valueAccessor(function(d){return d; });

	timeChart
		.width(600)
		.height(160)
		.margins({top: 10, right: 50, bottom: 30, left: 50})
		.dimension(dateDim)
		.group(numData)
		.transitionDuration(500)
		.x(d3.time.scale().domain([minDate, maxDate]))
		.elasticY(true)
		.xAxisLabel("Timeline")
		.yAxis().ticks(4);
}