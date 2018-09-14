'use strict';

angular.module('dms.sources', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/sources', {
    templateUrl: 'sources/sources.html',
    controller: 'SourcesCtrl'
  });
}])

.controller('SourcesCtrl', function($scope, Configuration, DocumentSources, $location, GDrive, Errors) {

	$scope.googleOAuthURL  = Configuration.backendURL + "rest/v1/google/oauthstart"
  	$scope.googleRevokeURL = Configuration.backendURL + "rest/v1/google/oauthrevoke"

    $scope.getGoogleAuthState = function() {
        return $location.search().googleauth;
    }

    $scope.sources = DocumentSources.query();
    $scope.removeSource = function(src) {
        $scope.sources.splice($scope.sources.indexOf(src), 1);
        if (src.id) src.$remove();
    }
    $scope.addFileSource = function() {
        $scope.sources.push({type:"file"});
    }
    $scope.addGDriveSource = function() {
        $scope.sources.push({type:"gdrive"});
    }
    $scope.save = function() {
        $scope.sources.forEach(function(src) {
            DocumentSources.save(src);
        });
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
   // $scope.gdrive.setFolder({id: "root", name:"Root"});

});