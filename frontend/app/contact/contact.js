'use strict';

angular.module('dms.contact', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/contact/:contactid', {
    templateUrl: 'contact/contact.html',
    controller: 'ContactCtrl'
  });
}])

.controller('ContactCtrl', function($scope, $routeParams, Contact, Errors) {
	$scope.nameToIdentifier = function(name) {
		return name.toLowerCase().replace(/[^a-z0-9]/ig, '-');
	}

	$scope.loading = true;
	$scope.contactComplete = false;
	$scope.loadingDocs = true;

	$scope.identifierLinked = false;
	
	// load or create Contact
	if ($routeParams.contactid != "new") {
		$scope.contact = Contact.get({contactId:$routeParams.contactid}, 
				function() { // success 
					$scope.loading = false;
					$scope.contactComplete = true;
	
					// As soon as the identifier is changed once, we decouple the two.
					// We link the name to the identifier if they initially match (via nameToIdentifier())
					$scope.identifierLinked = ($scope.contact.identifier == $scope.nameToIdentifier($scope.contact.name));
				}, 
				function($response) { // error
					Errors.add($response); 
					$scope.loading = false;
				}
		);
		
		// load linked Documents
		$scope.linkedDocs = Contact.getDocs({contactId:$routeParams.contactid},
				function() { // success 
					$scope.loadingDocs = false;
				}, 
				function($response) { // error
					Errors.add($response); 
					$scope.loadingDocs = false;
				});

	} else {
		$scope.contact = new Contact();
		$scope.identifierLinked = true;
		$scope.loading = false;
		$scope.contactComplete = true;
		$scope.loadingDocs = false;
		$scope.linkedDocs = [];
	}	
	
	$scope.identifierChanged = function() {
		$scope.identifierLinked = false;
	}

	$scope.nameChanged = function() {
		if ($scope.identifierLinked) {
			$scope.contact.identifier = $scope.nameToIdentifier($scope.contact.name);
		}
	}
	
	$scope.saveContact = function() {
		$scope.savingContact = true;
		$scope.contact.$save(function() {
			$scope.savingContact = false;
			$.bootstrapGrowl("Contact saved!", { ele: 'body', type: 'info' });
		}, function(error) {
			$scope.savingContact = false;
			console.log(error);
			Errors.add(error);
		});
	}

});