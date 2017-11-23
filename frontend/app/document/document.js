'use strict';

angular.module('dms.document', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/document/:docid', {
    templateUrl: 'document/document.html',
    controller: 'DocumentCtrl'
  });
}])

.controller('DocumentCtrl', function($scope, $routeParams, Document, Contact, Tag, Errors, TrelloFacade) {	
	$scope.loading = true;
	$scope.documentComplete = false;

	$scope.documentUpdated = function() {
		$scope.activityWithComments = $scope.document.activityHistory.concat($scope.document.comments);
	}
	
	$scope.document = Document.get({docId:$routeParams.docid}, 
			function() { // success 
				$scope.loading = false;
				$scope.documentUpdated();
				$scope.documentComplete = true;
			}, 
			function($response) { // error
				Errors.add($response); 
				$scope.loading = false;
			}
	);	

	$scope.loadTags = function(query) {
        return Tag.query({q: query}).$promise;
    };
    
    $scope.addDocumentTag = function($tag) {
    	Document.addTag({docId:$routeParams.docid, tagName: $tag.name});
    	return true;
    }

    $scope.deleteDocumentTag = function($tag) {
    	Document.deleteTag({docId:$routeParams.docid, tagName: $tag.name});
    	return true;
    }
    
    $scope.submittingName = false;
    $scope.setName = function($data) {
    	$scope.submittingName = true;
    	Document.setName({docId:$routeParams.docid}, $data,
    			function(updatedDocument) {
    				$scope.submittingName = false;
    				$scope.document = updatedDocument;
    				$scope.documentUpdated();
    			},
    			function (response) {
    				$scope.submittingName = false;
    				Errors.add(response);
    			});
    	return true;
    }

    // archiving complete
    $scope.submittingArchivingComplete= false;
    $scope.markComplete = function() {
    	$scope.submittingArchivingComplete = true;
    	Document.setArchivingComplete({docId:$routeParams.docid}, true,
    			function(updatedDocument) {
    				$scope.submittingArchivingComplete = false;
    				$scope.document = updatedDocument;
    				$scope.documentUpdated();
    			},
    			function (response) {
    				$scope.submittingArchivingComplete = false;
    				Errors.add(response);
    			});
    	return true;
    }

    // action required
    $scope.actionRequired = {
    	submitting: false,
    	popoverOpen: false,
    	popoverResolveOpen: false,
    	comment: "",
    	submit : function() {
    		$scope.actionRequired.submitting = true;
	    	Document.setActionRequired({docId:$routeParams.docid}, !$scope.document.actionRequired,
	    			function(updatedDocument) {
	    				$scope.actionRequired.popoverOpen = false;
	    				$scope.actionRequired.popoverResolveOpen = false;
	    				$scope.actionRequired.submitting = false;
	    				$scope.document = updatedDocument;
	    				$scope.documentUpdated();
	    	    		if ($scope.actionRequired.comment) {
	    	    			$scope.postComment($scope.actionRequired.comment, function() {	    	    				
	    	    				$scope.actionRequired.submitting = false;
	    	    	    		// clear
	    	    	    		$scope.actionRequired.comment = "";
	    	    			});
	    	    		}
	    			},
	    			function (response) {
	    				$scope.actionRequired.submitting = false;
	    				Errors.add(response);
	    			});
	    	return true;
	    }
    }

    // description
    $scope.submittingDescription = false;
    $scope.submitDescription = function(description) {
    	$scope.submittingDescription = true;
    	$scope.document.description = description; 
    	Document.setDescription({docId:$routeParams.docid}, description,
    			function(updatedDocument) {
    				$scope.submittingDescription = false;
    				$scope.document = updatedDocument;
    				$scope.documentUpdated();
    			},
    			function(response) {
    				$scope.submittingDescription = false;
    				Errors.add(response);
    			});
    	$scope.$apply();
    }
    
    // comments
    $scope.newComment = "";
    $scope.submittingComment = false;
    $scope.submitComment = function($data) {
    	$scope.postComment($scope.newComment);
    }
    $scope.postComment = function(comment, callback) {
    	Document.postComment({docId:$routeParams.docid}, comment,
    			function(commentEntity) {
		    		$scope.newComment = ""; // reset textarea
		    		$scope.document.comments.push(commentEntity);
		    		$scope.activityWithComments.unshift(commentEntity);
		    		$scope.submittingComment = false;
		    		if (callback) {
		    			callback();
		    		}
    			},
    			function (response) {
    				Errors.add(response);
    				$scope.submittingComment = false;
    				if (callback) {
                                    callback();
                                }
    			});
    }
   
    // contact editing
    $scope.editingContact = false;
    $scope.submittingContact = false;
    $scope.toggleContactEdit = function() {
    	$scope.editingContact = !$scope.editingContact;
    };
    $scope.setContact = function() {
    	$scope.submittingContact = true;
    	Document.setContact({docId:$routeParams.docid}, $scope.contactSelect[0].selectize.items[0],
    			function(result) {
    				$scope.submittingContact = false;
    				$scope.editingContact = false;
    				$scope.document = result;
    				$scope.documentUpdated();
    			},
    			function(response) {
    				Errors.add(response);
    			});
    }
    $scope.clearContact = function() {
    	$scope.submittingContact = true;
    	Document.clearContact({docId:$routeParams.docid}, $scope.contactSelect[0].selectize.items[0],
    			function(result) {
    				$scope.submittingContact = false;
    				$scope.editingContact = false;
    				$scope.document = result;
    				$scope.documentUpdated();
    			},
    			function(response) {
    				Errors.add(response);
    			});
    }
    
    $scope.contactSelect = $('#select-contact').selectize({
    	valueField: 'id',
        labelField: 'name',
        searchField: 'name',
        create: false,
        render: {
        	option: function (contact, escape) {
        		return "<div><strong>"+escape(contact.name)+"</strong> ("+escape(contact.city)+")</div>";
        	}
        },
        load: function(query, callback) {
        	if (!query.length) return callback();
            Contact.query({q: query},
            	function(res, headers) {
            		callback(res);
            	},
                function(response) {
            		console.log("Error loading contacts");
            		console.log(response);
                    callback();
                }
            );
        }
    });

    // Trello (modal)
    $scope.submittingTrello = false;
	$scope.trelloCard = {
		desc: "",
		idList : null
	};
	/* Creates a card based on current form values and submits it to Trello. Hides the dialog on success. */ 
    $scope.createTrelloCard = function() {
    	$scope.submittingTrello = true;
    	var description = "Task created from Themis:\n\n" + $scope.trelloCard.desc + "\n\nSee: "+window.location.href;
    	var newCard = {
    			name: $scope.document.name,
    			desc: description,
    			idList: $scope.trelloCard.idList
    	}
    	Trello.post("cards", newCard,
    			function(success) {
    				$scope.submittingTrello = false;
    				
    				var newCardId = success.id;
    				var newCardUrl = success.url;    				
    				Document.addLink({docId:$routeParams.docid}, {
    					title: "Trello card",
    					url: newCardUrl,
    					type: "TRELLO_CARD"
    				},
    				function(linkResponse) {
    					$scope.document.links.push(linkResponse);
    					// reset card and UI
    					$scope.trelloCard = {
    							desc: "",
    							idList : null
    					};
    					$scope.trelloListSelect[0].selectize.clear(true);
    					$('#trelloModal').modal('hide');
    				},
    				function(error) { // add link error
    					Errors.add(error);
    				})
    			},
    			function(error) {
    				$scope.submittingTrello = false;
    				Errors.add(error);
    			});
    }
    // called when Trello dialog opens
    $scope.loadingTrelloLists = false;
    $scope.populateTrelloList = function() {
    	$scope.trelloListSelect = $('#select-trello-list').selectize({
        	valueField: 'id',
            labelField: 'name',
            searchField: 'name',
            create: false,
            onChange: function(value) {
            	$scope.trelloCard.idList = value;
            	$scope.$apply();
            },
            render: {
            	option: function (list, escape) {
            		return "<div><strong>"+escape(list.boardName)+"</strong> ("+escape(list.name)+")</div>";
            	},
    	    	item: function (list, escape) {
    	    		return "<div><strong>"+escape(list.boardName)+"</strong> ("+escape(list.name)+")</div>";
    	    	}
            },
            options: []
        });

    	$scope.loadingTrelloLists = true;
	    TrelloFacade.loadMyLists(function(lists) {
	    	$scope.trelloListSelect[0].selectize.addOption(lists);
	    	$scope.loadingTrelloLists = false;
	    	$scope.$apply();
	    });
    }
    
    // add web links
    $scope.addLink = {
    		submitting : false,
    		title: "",
    		url : "",
    		popoverOpen : false,
    		clear : function() {
    			$scope.addLink.title = "";
    			$scope.addLink.url = "";
    		},
    		submit : function() {
				Document.addLink({docId:$routeParams.docid}, {
					title: $scope.addLink.title,
					url: $scope.addLink.url,
					type: "WEB_LINK"
				},
				function(linkResponse) {
					$scope.document.links.push(linkResponse);
					$scope.addLink.clear();
					$scope.addLink.popoverOpen = false;
				},
				function(error) { // add link error
					Errors.add(error);
				});
    		},
    		isComplete: function() {
    			return $scope.addLink.title && $scope.addLink.url;
    		}
    };
    
    // follow-up
    $scope.daysFromNow = function(days) {
    	var dat = new Date();
    	dat.setDate(dat.getDate() + days);
    	return dat;
    };
    $scope.followUp = {
		minDate : new Date(),
    	date : $scope.daysFromNow(1),
    	tomorrow : function() {
    		$scope.followUp.date = $scope.daysFromNow(1);
    	},
    	nextWeek : function() {
    		$scope.followUp.date = $scope.daysFromNow(7);
    	},
    	inFourWeeks : function() {
    		$scope.followUp.date = $scope.daysFromNow(28);
    	},
    	datePickerOpen : false,
    	popoverOpen : false,
    	comment: "",
    	submitting : false,
    	openDatePicker : function($event) {
    			$scope.followUp.datePickerOpen = true;
    	},
    	submit : function() {
			$scope.followUp.submitting = true;
    		Document.setFollowup({docId:$routeParams.docid}, $scope.followUp.date,
        			function(updatedDocument) {
    					console.log(updatedDocument);
    					$scope.followUp.submitting = false;
        				$scope.followUp.popoverOpen = false;
        				$scope.document = updatedDocument;
        				$scope.documentUpdated();
	    	    		if ($scope.followUp.comment) {
	    	    			$scope.postComment($scope.followUp.comment, function() {	    	    				
	    	    				$scope.followUp.submitting = false;
	    	    	    		// clear
	    	    	    		$scope.followUp.comment = "";
	    	    			});
	    	    		}
    				},
        			function (response) {
    					$scope.followUp.submitting = false;
        				Errors.add(response);
        			});
    	},
    	clearFollowup : function() {
    		$scope.followUp.submitting = true;
    		Document.clearFollowup({docId:$routeParams.docid},
        			function(updatedDocument) {
				console.log(updatedDocument);
				$scope.followUp.submitting = false;
				$scope.document = updatedDocument;
				$scope.documentUpdated();
			},
			function (response) {
				$scope.followUp.submitting = false;
				Errors.add(response);
			});
    	}

    }
});
