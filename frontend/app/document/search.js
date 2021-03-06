'use strict';

angular.module('dms.search', ['ngRoute'])

.factory('Search', function() {
  return {
	  term:''
  };
})

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/documents', {
    templateUrl: 'document/search.html',
    controller: 'SearchCtrl'
  });
}])

.controller('SearchCtrl', function($scope, $location, Search, Document, Errors) {
	$scope.search = Search;
	$scope.minDate = new Date("01/01/2015").getTime();
	$scope.maxDate   = new Date().getTime();
	$scope.fromDate = $scope.minDate;
	$scope.toDate   = $scope.maxDate;
	$scope.dateRefersTo = "archived";

	// load min and max timestamps that make sense for querying. Then update slider and current values accordingly.
	Document.queryMeta({}, 
			function(meta) {
				$scope.slider.setAttribute("min", meta.earliestTimestamp);
				$scope.slider.setAttribute("max", meta.latestTimestamp);
				$scope.fromDate = meta.earliestTimestamp;
				$scope.toDate = meta.latestTimestamp;
				$scope.slider.setValue([$scope.fromDate, $scope.toDate]);
			},
			Errors.errorCallback);
	
   $scope.formatTime = function(unixTimestamp) {
	   var date = new Date(unixTimestamp);  
	   return date;
   };
   
   $scope.slided = function(d) {
	 $scope.fromDate = $scope.slider.getValue()[0];
	 $scope.toDate = $scope.slider.getValue()[1];
	 $scope.$apply();
   };
   
	$scope.slider = $("#search-date-slider").slider({
		min: $scope.minDate,
		max: $scope.maxDate,
		step: 1000*60*60*24,
		value: [$scope.fromDate, $scope.toDate],
		formatter: function(value) {
			return $scope.formatTime(value[0]) + " - "+$scope.formatTime(value[1]);
		}
	}).on('change', $scope.slided).data('slider');
	
	$scope.searchNow = function() {
		var searchParams = {q:Search.term};
		if ($scope.dateRefersTo == "archived") {
			searchParams.fromArchiveTimestamp = $scope.fromDate;
			searchParams.toArchiveTimestamp = $scope.toDate;
		} else if ($scope.dateRefersTo == "updated") {
			searchParams.fromModificationTimestamp = $scope.fromDate;
			searchParams.toModificationTimestamp= $scope.toDate;
		}
		Search.results = Document.query(searchParams, function() {}, Errors.errorCallback);
	}
})

.controller('SearchFormCtrl', function($scope, $location, Search, Document, Errors) {
	$scope.searchFormService = Search;
	$scope.startSearch = function() {
		$location.path("/documents");
		$scope.searchFormService.results = Document.query({q:$scope.searchFormService.term}, function() {}, Errors.errorCallback);
	}
});
