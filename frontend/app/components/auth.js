// Inspired by (but now largely different from): http://jasonwatmore.com/post/2014/05/26/AngularJS-Basic-HTTP-Authentication-Example.aspx
// See also: http://brewhouse.io/blog/2014/12/09/authentication-made-simple-in-single-page-angularjs-applications.html
'use strict';

angular.module('dms.auth', [])

.factory('RESTLogin', ['$resource', 'Configuration',
  function($resource, Configuration) {
    return $resource(Configuration.backendURL + 'rest/v1/login', {}, {
    	login: {method: 'POST'}, // needs username and password in body
    	refreshToken: {method: 'GET'}, // needs nothing, but only works if cookie is already present and valid
    });
  }])

.factory('AuthenticationService',
    function (Base64, $http, $cookieStore, $rootScope, $location, User, Errors, RESTLogin) {
        var service = {};

        /** To be called from the angular run() method when the application reloads to keep them logged in.
         *  Re-logs and starts timer if a cookie exists. Otherwise shows login form. */
        service.onApplicationReload = function() {
        		$rootScope.appLoading = true;
        		// on navigation event, redirect to login page if not logged in
                $rootScope.$on('$locationChangeStart', function (event, next, current) {
                     if ($location.path() !== '/login' && !$rootScope.profile) {
                         $location.path('/login');
                     }
                });
        	
          	   // keep user logged in after page refresh
        	   // see if loading profile works
        	   $rootScope.profile = User.me({},
   				    function() {
        		   		// success? all good, we can start
        		   		service.restartRefreshTokenTimeout();
        		   		$rootScope.appLoading = false;
   		   			},
   		   			function(error) {
   		   				// failed due to something else than 401 unauthorized? weird - show error.
   		   				if (error.status != 401) {
   		   					console.log("Initial profile load failed, but not because of unauthorized request:");
   		   					console.log(error);
   		   					Errors.add(error);   		   					
   		   				}
   		   				// in both cases, show login mask
   		   				$location.path('/login');
   		   				$rootScope.appLoading = false;
   		   			});
        }
        
        /** Restarts the timeout to refresh the JWT token. */
        service.restartRefreshTokenTimeout = function() {
        	// first, clear old timeout
        	if (service.refreshTokenTimeout) {
        		clearTimeout(service.refreshTokenTimeout);
        	}
        	// the restart new one
			service.refreshTokenTimeout = setTimeout(function() {
				RESTLogin.refreshToken({}, function() {
					console.log("Auth token refreshed.");
					service.restartRefreshTokenTimeout();
				}, function(error) {
					console.log("Failed to refresh auth token:");
					console.log(error);
				});
			}, 10*60*1000);

        }

        /** Calls the REST login method, implicitly setting the auth cookie, and calls the callback function on 
         *  success and error.  */
        service.login = function (username, password, successCallback, errorCallback) {
        	var token = RESTLogin.login({username: username, password: password},
            		function(token) {            			
            			// load profile
                 	   $rootScope.profile = User.me({},
              				    function() {
                 		   			// success? all good, we can start
              			   			successCallback();
              			   			service.restartRefreshTokenTimeout();
              		   			},
              		   			function(error) { 
              		   				// profile load error
              		   				console.log("Profile load failed:");
              		   				console.log(error);
              		   				Errors.add(error);
              		   				
              		   				// failure? We need to login again
              		   				$location.path('/login');
                        			errorCallback();
              		   			});
            			
            		},
            		function (error) { // login error
            			console.log("REST login error:");
            			console.log(error);
            			
            			errorCallback();
            		});
        };
  
        /** Clears the user profile and directs to the login page. */
        service.logout = function () {
            $rootScope.profile = null;
        	if (service.refreshTokenTimeout) {
        		clearTimeout(service.refreshTokenTimeout);
        	}
    		$location.path('/login');
        };
  
        return service;
    })

// Utility for Base64 encoding
.factory('Base64', function () {
    /* jshint ignore:start */
  
    var keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  
    return {
        encode: function (input) {
            var output = "";
            var chr1, chr2, chr3 = "";
            var enc1, enc2, enc3, enc4 = "";
            var i = 0;
  
            do {
                chr1 = input.charCodeAt(i++);
                chr2 = input.charCodeAt(i++);
                chr3 = input.charCodeAt(i++);
  
                enc1 = chr1 >> 2;
                enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
                enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
                enc4 = chr3 & 63;
  
                if (isNaN(chr2)) {
                    enc3 = enc4 = 64;
                } else if (isNaN(chr3)) {
                    enc4 = 64;
                }
  
                output = output +
                    keyStr.charAt(enc1) +
                    keyStr.charAt(enc2) +
                    keyStr.charAt(enc3) +
                    keyStr.charAt(enc4);
                chr1 = chr2 = chr3 = "";
                enc1 = enc2 = enc3 = enc4 = "";
            } while (i < input.length);
  
            return output;
        },
  
        decode: function (input) {
            var output = "";
            var chr1, chr2, chr3 = "";
            var enc1, enc2, enc3, enc4 = "";
            var i = 0;
  
            // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
            var base64test = /[^A-Za-z0-9\+\/\=]/g;
            if (base64test.exec(input)) {
                window.alert("There were invalid base64 characters in the input text.\n" +
                    "Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='\n" +
                    "Expect errors in decoding.");
            }
            input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
  
            do {
                enc1 = keyStr.indexOf(input.charAt(i++));
                enc2 = keyStr.indexOf(input.charAt(i++));
                enc3 = keyStr.indexOf(input.charAt(i++));
                enc4 = keyStr.indexOf(input.charAt(i++));
  
                chr1 = (enc1 << 2) | (enc2 >> 4);
                chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
                chr3 = ((enc3 & 3) << 6) | enc4;
  
                output = output + String.fromCharCode(chr1);
  
                if (enc3 != 64) {
                    output = output + String.fromCharCode(chr2);
                }
                if (enc4 != 64) {
                    output = output + String.fromCharCode(chr3);
                }
  
                chr1 = chr2 = chr3 = "";
                enc1 = enc2 = enc3 = enc4 = "";
  
            } while (i < input.length);
  
            return output;
        }
    };
  
    /* jshint ignore:end */
})
