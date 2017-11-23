angular.module('dms.editableMarkup', [])
.directive('dmsEditableMarkup', function() {
	return {
		templateUrl: 'components/domain/dms-editable-markup.html',
		restrict: 'E',
		scope: {
		      value: '@',
		      submit: '&'
		},
		link: function(scope, element, attrs) {
			scope.textarea = element.find("textarea")[0];
			scope.discard = element.find("button")[0];
		},
		controller: function($scope) {
			$scope.editing = false;
			$scope.toggle = function() {
				$scope.editing = !$scope.editing;
				if ($scope.editing) {
					// start editing -> copy value so we can discard if desired
					$scope.editedValue = angular.copy($scope.value);
					setTimeout(function() { $scope.textarea.focus(); }, 1);
				}
			};
			$scope.focusLost = function($event) {
				setTimeout(function() {
					var target = document.activeElement;
					// clicked on discard -> do nothing
					if (target == $scope.discard) {
						console.log("Discarding");
						$scope.editedValue = angular.copy($scope.value);
						$scope.editing = false;
						$scope.$apply();
					} else {
						console.log("Saving");
						$scope.editing = false;
						if ($scope.value != $scope.editedValue) {
							$scope.value = $scope.editedValue;
							$scope.submit({value:$scope.value});
						} else {
							console.log("Nothing changed");
						}
						$scope.$apply();
					}
				}, 1);
			};
		}
	};
})
.filter("markdown", function($sce) {
	return function(textAsMarkdown) {
		return $sce.trustAsHtml(markdown.toHTML(textAsMarkdown));
	};
});