'use strict';

angular.module('dms.sources', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/sources', {
    templateUrl: 'sources/sources.html',
    controller: 'SourcesCtrl'
  });
}])

.controller('SourcesCtrl', function($scope, Configuration, DocumentSources, $location, GDrive, Errors) {

	$scope.googleOAuthURL  = Configuration.backendURL + "google/oauthstart"
  	$scope.googleRevokeURL = Configuration.backendURL + "google/oauthrevoke"

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
        $scope.sources.push({type:"gdrive", gdriveSourceFolderId: "root", gdriveArchiveFolderId: "root"});
    }
    $scope.save = function() {
        $scope.sources.forEach(function(src) {
            DocumentSources.save(src);
        });
    }
    $scope.execute = function(id) {
            console.log("Executing source "+id);
            DocumentSources.execute({id:id});
    }
});