'use strict';

angular.module('dms.admin', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/admin', {
    templateUrl: 'admin/admin.html',
    controller: 'AdminCtrl'
  });
}])

.controller('AdminCtrl', function($scope, Errors, ServerConfiguration) {
	$scope.serverConfigJson = "";
	$scope.editing = false;
	$scope.submitting = false;
	
	$scope.serverConfig = ServerConfiguration.get({},
			function(serverConfig) {
					$scope.serverConfigJson = JSON.stringify(serverConfig, null, '\t');
			}, // success
			Errors.errorCallback
	);
	
	$scope.submit = function() {
		$scope.serverConfig = JSON.parse($scope.serverConfigJson);
		$scope.submitting = true;
		ServerConfiguration.save($scope.serverConfig,
				function() {
					$scope.editing = false;
					$scope.submitting = false;
				}, 
				function(error) {
					Errors.add(error);
					$scope.submitting = false;
				});
	}
	
	$scope.edit = function() {
		$scope.editing = true;
	}
	
	$scope.cancelEdit = function() {
		$scope.editing = false;
		$scope.serverConfigJson = JSON.stringify($scope.serverConfig, null, '\t');
	}
});