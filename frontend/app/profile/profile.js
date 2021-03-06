'use strict';

angular.module('dms.profile', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/profile', {
    templateUrl: 'profile/profile.html',
    controller: 'ProfileCtrl'
  });
}])

.controller('ProfileCtrl', function($scope, $rootScope, User, AuthenticationService) {
	$scope.profile = $rootScope.profile;
	
	$scope.oldPassword = "";
	$scope.newPassword = "";
	$scope.newPasswordRepeat = "";

	$scope.passwordsMatch = function() {
		return $scope.newPassword == $scope.newPasswordRepeat;
	}
	$scope.canSubmit = function() {
		return ($scope.newPassword) && ($scope.newPassword == $scope.newPasswordRepeat);
	}
	$scope.changePassword = function() {
		User.setPassword({username: $scope.profile.username}, {newPassword: $scope.newPassword, oldPassword: $scope.oldPassword},
				function($data) { 
					AuthenticationService.setCredentials($scope.profile.username, $scope.newPassword);
					$scope.oldPassword = "";
					$scope.newPassword = "";
					$scope.newPasswordRepeat = "";
					$scope.passwordError = null;
					$('#passwordModal').modal('hide');
					$.bootstrapGrowl("Password changed!", { ele: 'body', type: 'info' }); 
				},
				function($error) { 
					$scope.passwordError = $error; 
				}
			);
	}
});