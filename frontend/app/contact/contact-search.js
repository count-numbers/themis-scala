'use strict';

angular.module('dms.contactSearch', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/contacts', {
    templateUrl: 'contact/contact-search.html',
    controller: 'ContactSearchCtrl'
  });
}])

.controller('ContactSearchCtrl', function($scope, $location, Contact, Errors) {
	$scope.term = "";
	$scope.searchNow = function() {
		console.log("searching");
		$scope.results = Contact.query({q: $scope.term}, 
				function() {},
				Errors.errorCallback);
	}
});