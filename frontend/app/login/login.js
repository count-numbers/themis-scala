'use strict';

angular.module('dms.login', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/login', {
    templateUrl: 'login/login.html',
    controller: 'LoginCtrl'
  });
}])

// Used by login form in navbar
.controller('LoginCtrl', function($scope, $location, AuthenticationService, Errors) {
	// reset login status
    AuthenticationService.logout();

    $scope.login = function () {
        $scope.dataLoading = true;
        Errors.clear();
        AuthenticationService.login($scope.username, $scope.password, 
        		function() {
        			$location.path('/');
        			$scope.dataLoading = false;
        		},
        		function() {
        			$scope.error = "That didn\'t work. Please try again.";
        			$scope.dataLoading = false;        			
        		});
    };
})

// used by profile dropdown in navbar
.controller('ProfileDropdownCtrl', function($scope, AuthenticationService) {
	$scope.logout = function() {
		AuthenticationService.logout();
	}
});
