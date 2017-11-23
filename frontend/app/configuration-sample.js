// fill in values and copy to configuration.js
angular.module('dms.configuration', [])
.factory('Configuration', function() {
	return { 
		backendURL : "http://localhost:9000/themis/",
		trelloApiKey: "[key]"
	};
});
