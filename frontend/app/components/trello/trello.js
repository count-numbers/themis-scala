angular.module('dms.trello', [])
.factory('TrelloFacade', function(Errors) {
			return {
				/** Logs the user into Trello, if not done so already. */
				login : function() {
					Trello.authorize({
						name: "Themis DMS",
						type : "popup",
						interactive: true,
						scope: { write: true, read: true }
					});
				},
				/** Loads the first list from each board of the current user. 
				 *  Sends an array of objects to the callback function. Each object has properties
				 *  id, name, boardId, and boardName. */
				loadMyLists : function(callback) {
					this.login();
					// We will send this to callback. Initialize as empty array.
					var myLists = [];
					Trello.get("/member/me/boards", function(boards) {
						// we collect only the first list in each board.
						// remember how many we collect.
						var expectedLists = boards.length;
						boards.forEach(function(board) {
							// we don't have the list yet, but we already remember the board details
							var newList = {
									boardName: board.name,
									boardId:board.id
								};
							Trello.get("/boards/"+newList.boardId+"/lists", function(lists) {
								if (lists.length == 0) {
									// no lists on this board? one list less expected
									expectedLists--;
								}
								var list = lists[0]; // we only care about the first
								// add list details to newList object
								newList.id = list.id;
								newList.name = list.name;
								myLists.push(newList);
								// done? -> callback
								if (myLists.length == expectedLists) {
									callback(myLists);
								}
							}, 
							function(error) { // lists error
								Errors.add(error);
								expectedLists--;
							});
						});
					}, function(error) { // boards error
						Errors.add(error);
					});
				}
			};
		})