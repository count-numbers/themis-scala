var dmsServices = angular.module('dms.resources', ['ngResource']);

dmsServices.factory('Document', ['$resource', 'Configuration',
  function($resource, Configuration) {
    return $resource(Configuration.backendURL + 'rest/v1/document/:docId', {docId:'@id'}, {
        update: {method: 'PATCH' },

    	setContact: {method:'PUT', params:{docId:'@docId'}, url:Configuration.backendURL + 'rest/v1/document/:docId/contact', headers: {"Content-Type": "text/plain"}},
    	clearContact: {method:'DELETE', params:{docId:'@docId'}, url:Configuration.backendURL + 'rest/v1/document/:docId/contact'},
    	clearFollowup: {method:'DELETE', params:{docId:'@docId'}, url:Configuration.backendURL + 'rest/v1/document/:docId/follow-up-timestamp'},
    	
    	addTag: {method:'PUT', params:{docId:'@docId', tagName:'@tagName'}, url:Configuration.backendURL + 'rest/v1/document/:docId/tags/:tagName'},
    	deleteTag: {method:'DELETE', params:{docId:'@docId', tagName:'@tagName'}, url:Configuration.backendURL + 'rest/v1/document/:docId/tags/:tagName'},
    	
    	postComment: {method:'POST', params:{docId:'@docId'}, url:Configuration.backendURL + 'rest/v1/document/:docId/comments'},

    	addLink: {method:'POST', params:{docId:'@docId'}, url:Configuration.backendURL + 'rest/v1/document/:docId/links'},

    	queryAttention: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/documents/attention'},
    	query: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/documents'},
    	queryMeta: {method:'GET', params:{}, isArray:false, url:Configuration.backendURL + 'rest/v1/documents/meta'}
    });
  }]);

dmsServices.factory('Contact', ['$resource', 'Configuration',
  function($resource, Configuration) {
    return $resource(Configuration.backendURL + 'rest/v1/contact/:contactId', {contactId:'@id'}, {
    	getDocs: {method: 'GET', params: {contactId:'@contactId'}, isArray: true, url: Configuration.backendURL + 'rest/v1/contact/:contactId/documents'},
    	query: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/contacts'}
    });
  }]);

dmsServices.factory('Tag', ['$resource','Configuration',
  function($resource, Configuration) {
    return $resource(Configuration.backendURL + Configuration.backendURL + 'rest/v1/tag/:tagName', {tagName:'@name'}, {
      query: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/tags'}
    });
  }]);

dmsServices.factory('Activity', ['$resource','Configuration',
  function($resource, Configuration){
    return $resource(Configuration.backendURL + 'rest/v1/activity/:actvityId', {activityId:'@id'}, {
      query: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/activities'}
    });
  }]);

dmsServices.factory('User', ['$resource', '$rootScope', 'Configuration',
  function($resource, $rootScope, Configuration){
    return $resource(Configuration.backendURL + 'rest/v1/user/:username', {username: '@username'}, {
    	setPassword: {method:'PUT', params:{username: '@username'}, url:Configuration.backendURL + 'rest/v1/user/:username/password'},
    	me: {method:'GET', url:Configuration.backendURL + 'rest/v1/user/me'}
    });
}]);

dmsServices.factory('ServerConfiguration', ['$resource','Configuration', function($resource, Configuration){
    return $resource(Configuration.backendURL + 'rest/v1/configuration', {}, {});
}]);


dmsServices.factory('DocumentSources', ['$resource','Configuration',
  function($resource, Configuration){
    return $resource(Configuration.backendURL + 'rest/v1/source/:sourceId', {sourceId:'@id'}, {
      query: {method:'GET', params:{}, isArray:true, url:Configuration.backendURL + 'rest/v1/sources'}
    });
  }]);


dmsServices.factory('GDrive', ['$resource', 'Configuration',
   function($resource, Configuration) {
    return $resource(Configuration.backendURL+'rest/v1/google/drive/:folderId', {folderId:'@id'}, {
    });
}]);