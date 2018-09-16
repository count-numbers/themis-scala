'use strict';

angular.module('dms.gdrive', [])
.directive('gdriveFileselector', function() {

    return {
        templateUrl: 'components/util/gdrive-file-selector.html',
        restrict: 'E',
        scope: {
              folderId: '=folder'
        },
        controller: function($scope, GDrive, Errors) {
            $scope.folderName = "[unknown]";
            $scope.folders = [];
            $scope.setFolder = function(folder) {
                console.log("Changing to folder: "+folder.id);
                $scope.selectedFolderId = folder.id;
                $scope.folderName = folder.name;
                $scope.loadChildren();
            };
            $scope.home = function() {
                $scope.selectedFolderId = "root";
                $scope.folderName ="(GDrive root folder)"
                $scope.loadChildren();
            };
            $scope.loadChildren = function() {
                $scope.loading = true;
                $scope.folders = GDrive.query({folderId:$scope.selectedFolderId},
                            function(data) { // success
                                $scope.folders = $scope.folders.filter(function(f) { return (f.mimeType == 'application/vnd.google-apps.folder') });
                                $scope.loading = false;
                            },
                            function($response) { // error
                                Errors.add($response);
                                $scope.loading = false;
                            }
                );
            };
            $scope.submit = function() {
                console.log("Submitting: " + $scope.folderId);
                $scope.folderId = $scope.selectedFolderId;
            };

            $scope.selectedFolderId = $scope.folderId;
            $scope.loadChildren();

            console.log("Folder is "+$scope.folderId);
        }
    };
 })

