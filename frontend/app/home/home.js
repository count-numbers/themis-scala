'use strict';

angular.module('dms.home', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/home', {
    templateUrl: 'home/home.html',
    controller: 'HomeCtrl'
  });
}])

.controller('HomeCtrl', function($scope, Document, Activity, Errors) {
	$scope.loadingActivityFeed = true;
	$scope.loadingDocuments    = true;
	$scope.activityStream = Activity.query({}, 
			function() { 
				$scope.loadingActivityFeed = false; 
			}, 
			function($error) { 
				$scope.error = true;
				$scope.loadingActivityFeed = false;
				Errors.add($error);
			}
	);
	$scope.documents = Document.queryAttention({}, 
			function() { 
				$scope.loadingDocuments = false; 
			}, 
			function($error) {
				$scope.error = true; 
				$scope.loadingDocuments = false; 
				Errors.add($error);
			}
	);
});