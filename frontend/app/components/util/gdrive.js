'use strict';

angular.module('dms.gdrive', [])
.directive('gdriveFileselector', function() {

    return {
        templateUrl: 'components/util/gdrive-file-selector.html',
        restrict: 'E',
        scope: {
              folderId: '=folder',
              label: '='
        },
        controller: function($scope, GDrive, Errors) {
            $scope.visible = false;
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
            $scope.toggle = function() {
                $scope.visible = !$scope.visible;
                if ($scope.visible) {
                    $scope.loadChildren();
                }
            };
            $scope.submit = function() {
                console.log("Submitting: " + $scope.folderId);
                $scope.folderId = $scope.selectedFolderId;
                $scope.visible = false;
            };

            $scope.selectedFolderId = $scope.folderId;

            console.log("Folder is "+$scope.folderId);
        }
    };
 })

