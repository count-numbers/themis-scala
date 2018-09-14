'use strict';

// Declare app level module which depends on views, and components
angular.module('dms', [
  'ngRoute',
  'ngTagsInput',
  'ngCookies',
  'ngAnimate',
  'xeditable',
  'ui.bootstrap',
  'dms.configuration',
  'dms.home',
  'dms.document',
  'dms.search',
  'dms.contact',
  'dms.contactSearch',
  'dms.login',
  'dms.profile',
  'dms.sources',
  'dms.admin',
  'dms.trello',
  'dms.auth',
  'dms.resources',
  'dms.dmsDomain',
  'dms.editableMarkup'
])

.config(['$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
  $routeProvider.otherwise({redirectTo: '/home'});
  // this is necessary so XHR and $resource calls set and read cookies which we use for authentication
  $httpProvider.defaults.withCredentials = true;
}])

.run(function($rootScope, $location, $cookieStore, $http, editableOptions, Configuration, AuthenticationService) {
   $rootScope.configuration = Configuration;
   
   editableOptions.theme = 'bs3';
  
  //keep user logged in after page refresh
  AuthenticationService.onApplicationReload();
   
  // Load Trello script and append to body.
  var script = document.createElement( 'script' );
  script.type = 'text/javascript';
  script.src = "https://trello.com/1/client.js?key="+$rootScope.configuration.trelloApiKey;
  $("body").append( script );
})

.factory('Errors', function($location) {
	  var errorsService = { errors: [],
		  	/** Deletes all currently visible errors. */
		  	clear: function() { 
		  		this.errors = []; 
		  	},
		  	
		  	/** True iff there is at least one error message visible. */
		  	hasErrors: function() { 
		  		return this.messages.length > 0; 
		  	},
		  	
		  	/** Adds an error to the list of errors. */
	  		add: function(response) { 
	  			console.log("REST error:"); 
	  			console.log(response);
	  			this.errors.push({id:this.errors.length, response: response}); 
	  			// if unauthorized, we need to re-log
	  			if (response.status == 401) {
		   			$location.path('/login');
	  			}
	  		},
	  		
	  		/** Can be passed to REST calls as the error callback. */
	  		errorCallback: function(error) {
	  			errorsService.add(error);
	  		}
	   };
	  return errorsService;
})

.controller('ErrorCtrl', function($scope, Errors) {
	$scope.errors = Errors;
});
