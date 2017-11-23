{
	"storage" : {
		"attachments-dir": "/path/to/dir",
		"temp-dir": "/tmp/themis"
	},
	
	"web" : {
		"app-url":"http://localhost:8000/app/index.html",
		"api-url":"http://localhost:8080/themis/rest/v1"
	},
	
	"cli" : {
    	"working-dir": ".",
    	"dev-null": "/dev/null"
    },

	"sources" : [
		{
			"id": "imap",
			"type": "imap",
			"jndiName": "java:/Mail",
			"folder":null, 
			"tags": ["imap", "incoming"],
			"keepAliveFreq": 300000,
			"user": "simon"
		},
		
		{
			"id": "files",
			"type": "filesystem",
			"folder": "/path/to/document-folder",
			"tags": ["file", "incoming"],
			"user": "simon"
		}
	]
}