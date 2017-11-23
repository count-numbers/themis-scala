angular.module('dms.dmsDomain', [])
.directive('dmsDocument', function() {
	return {
		templateUrl: 'components/domain/dms-document.html',
		restrict: 'E',
		scope: {
		      docId: '=',
		      name: '='
		},
	};
}).directive('dmsContact', function() {
		return {
			templateUrl: 'components/domain/dms-contact.html',
			restrict: 'E',
			scope: {
			      contact: '='
			},
		};
}).directive('dmsUser', function() {
	return {
		templateUrl: 'components/domain/dms-user.html',
		restrict: 'E',
		scope: {
		      userId: '=',
		      name: '='
		},
	};
}).directive('dmsActivityFeed', function() {
	return {
		templateUrl: 'components/domain/dms-activity-feed.html',
		restrict: 'E',
		scope: {
		      activityFeed: '=feed'
		},
	};
}).directive('dmsCommentFeed', function() {
	return {
		templateUrl: 'components/domain/dms-comment-feed.html',
		restrict: 'E',
		scope: {
		      comments: '=',
		      newComment: '=',
		      submitting: '=',
		      onsubmit: '&'
		},
	};
}).directive('dmsDocumentList', function() {
	return {
		templateUrl: 'components/domain/dms-document-list.html',
		restrict: 'E',
		scope: {
		      documents: '='
		},
	};
}).directive('dmsLoading', function() {
	return {
		templateUrl: 'components/domain/dms-loading.html',
		restrict: 'E',
		scope: {
		      loading: '='
		},
	};
}).directive('dmsRestError', function() {
	return {
		templateUrl: 'components/domain/dms-rest-error.html',
		restrict: 'E',
		scope: {
		      response: '='
		},
	};
}).directive('dmsLoadingIndicator', function() {
	return {
		template: '<img ng-if="loading" src="data:image/gif;base64,R0lGODlhEAAQAPIAAP///wAAAMLCwkJCQgAAAGJiYoKCgpKSkiH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAAAAAEAAQAAADMwi63P4wyklrE2MIOggZnAdOmGYJRbExwroUmcG2LmDEwnHQLVsYOd2mBzkYDAdKa+dIAAAh+QQJCgAAACwAAAAAEAAQAAADNAi63P5OjCEgG4QMu7DmikRxQlFUYDEZIGBMRVsaqHwctXXf7WEYB4Ag1xjihkMZsiUkKhIAIfkECQoAAAAsAAAAABAAEAAAAzYIujIjK8pByJDMlFYvBoVjHA70GU7xSUJhmKtwHPAKzLO9HMaoKwJZ7Rf8AYPDDzKpZBqfvwQAIfkECQoAAAAsAAAAABAAEAAAAzMIumIlK8oyhpHsnFZfhYumCYUhDAQxRIdhHBGqRoKw0R8DYlJd8z0fMDgsGo/IpHI5TAAAIfkECQoAAAAsAAAAABAAEAAAAzIIunInK0rnZBTwGPNMgQwmdsNgXGJUlIWEuR5oWUIpz8pAEAMe6TwfwyYsGo/IpFKSAAAh+QQJCgAAACwAAAAAEAAQAAADMwi6IMKQORfjdOe82p4wGccc4CEuQradylesojEMBgsUc2G7sDX3lQGBMLAJibufbSlKAAAh+QQJCgAAACwAAAAAEAAQAAADMgi63P7wCRHZnFVdmgHu2nFwlWCI3WGc3TSWhUFGxTAUkGCbtgENBMJAEJsxgMLWzpEAACH5BAkKAAAALAAAAAAQABAAAAMyCLrc/jDKSatlQtScKdceCAjDII7HcQ4EMTCpyrCuUBjCYRgHVtqlAiB1YhiCnlsRkAAAOwAAAAAAAAAAAA==" />',
		restrict: 'E',
		scope: {
		      loading: '='
		},
	};
});