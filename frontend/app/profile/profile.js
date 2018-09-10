'use strict';

angular.module('dms.profile', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/profile', {
    templateUrl: 'profile/profile.html',
    controller: 'ProfileCtrl'
  });
}])

.controller('ProfileCtrl', function($scope, $rootScope, User, AuthenticationService, Configuration, $location, GDrive, Errors) {
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

	$scope.googleOAuthURL  = Configuration.backendURL + "rest/v1/google/oauthstart"
  	//$scope.googleTestURL   = Configuration.backendURL + "rest/v1/google/drive/list?id=1AhmeAXtkU2_B228e2CkXlIAOCjKBfEv5"
  	$scope.googleRevokeURL = Configuration.backendURL + "rest/v1/google/oauthrevoke"

    $scope.getGoogleAuthState = function() {
        return $location.search().googleauth;
    }

    $scope.gdrive = {
        loading: true,
        folderId: "root",
        folderName: "Root",
        folders: [],
        setFolder: function(folder) {
            console.log("Changing to folder: "+folder.id);
            $scope.gdrive.loading = true;
            $scope.gdrive.folderId = folder.id;
            $scope.gdrive.folderName = folder.name;
            $scope.gdrive.folders = GDrive.query({folderId:$scope.gdrive.folderId},
            			function(data) { // success
            			    $scope.gdrive.folders = $scope.gdrive.folders.filter(function(f) { return (f.mimeType == 'application/vnd.google-apps.folder') });
            				$scope.gdrive.loading = false;
            			},
            			function($response) { // error
            				Errors.add($response);
            				$scope.gdrive.loading = false;
            			}
            );
        }
    }
    $scope.gdrive.setFolder({id: "root", name:"Root"});

});