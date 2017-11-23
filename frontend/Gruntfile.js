module.exports = function(grunt) {

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),    

    watch: {
    	js: {
    		files: [
    		   'app/**/*.js',
    		   '!app/libs.js',
    		   '!app/<%= pkg.name %>.js',
    		   '!app/<%= pkg.name %>.min.js'    		   
    		],
    		tasks: ['concat', 'uglify']
    	},
    	css: {
    		files: [
    		   'app/*.css',
    		   'app/components/**/*.css',
 		       'app/document/document.css',
 		       'app/login/login.css'
    		],
    		tasks: ['concat', 'uglify']
    	},
  		html: {
  			files: [
  			   'app/**/*.html',
  			   '!app/bower_components/**/*'
  			],
  			tasks: ['copy:html']
  		}
    },
    
    concat: {
    	themis: {
    		separator : ';',
    		src: [
    		      'app/configuration.js',
     		      'app/app.js',
     		      'app/resources.js',
     		      'app/home/home.js',
     		      'app/document/document.js',
     		      'app/document/search.js',

     		      'app/contact/contact.js',
     		      'app/contact/contact-search.js',
     		      'app/profile/profile.js',
     		      'app/login/login.js',
     		      'app/admin/admin.js',
     		      'app/components/auth.js',
     		      'app/components/domain/domain.js',
     		      'app/components/domain/editable-markup.js',
     		      'app/components/trello/trello.js'
    		],
    		dest: '../src/main/webapp/<%= pkg.name %>.js'
    	},
    	css: {
    		src: [
    		      'app/app.css',
    		      'app/timeline.css',
    		      'app/document/document.css',
    		      'login/login.css',
    		      'app/components/domain/dms-document-list.css',
    		      'app/components/domain/dms-editable-markup.css',
    		      'app/components/domain/dms-contact.css'
    		],
    		dest: '../src/main/webapp/css/<%= pkg.name %>.css'
    	},
    	libraries: {
    		separator : ';',
    		src: [
    		      'app/bower_components/html5-boilerplate/dist/js/vendor/modernizr-2.8.3.min.js',
    		      
    		      'app/bower_components/jquery/dist/jquery.min.js',
    		      
    		      'app/bower_components/angular/angular.min.js',
    		      'app/bower_components/angular-route/angular-route.min.js',
    		      'app/bower_components/angular-resource/angular-resource.min.js',
    		      'app/bower_components/angular-cookies/angular-cookies.min.js',
    		      'app/bower_components/angular-animate/angular-animate.min.js',
    		      'app/bower_components/ng-tags-input/ng-tags-input.min.js',
    		      'app/bower_components/angular-xeditable/dist/js/xeditable.min.js',
    		      'app/bower_components/angular-bootstrap/ui-bootstrap-tpls.min.js',
    		      
    		      'app/bower_components/bootstrap/dist/js/bootstrap.min.js',
    		      'app/bower_components/bootstrap-growl/jquery.bootstrap-growl.min.js',
    		      'app/bower_components/seiyria-bootstrap-slider/dist/bootstrap-slider.min.js',
    		      'app/bower_components/selectize/dist/js/standalone/selectize.min.js',
    		      
    		      'app/bower_components/markdown/lib/markdown.js'
    		      ],
    		dest: '../src/main/webapp/libs.js'
    	},
    	libcss: {
    		src: [
				'app/bower_components/html5-boilerplate/dist/css/normalize.css',
				'app/bower_components/html5-boilerplate/dist/css/main.css',
				'app/bower_components/bootstrap/dist/css/bootstrap.css',
				'app/bower_components/ng-tags-input/ng-tags-input.min.css',
				'app/bower_components/angular-xeditable/dist/css/xeditable.css',
				'app/bower_components/seiyria-bootstrap-slider/dist/css/bootstrap-slider.min.css',
				'app/bower_components/selectize/dist/css/selectize.bootstrap3.css'
    		],
    		dest: '../src/main/webapp/css/libs.css'
    	},

    },
    
    uglify: {
      options: {
        banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n',
        mangle: false
      },
      build: {
        src:  '../src/main/webapp/<%= pkg.name %>.js',
        dest: '../src/main/webapp/<%= pkg.name %>.min.js'
      }
    },
    
    copy: {
    	html: {
    		expand: true,
    		cwd: 'app',
	    	src: [
	    	      'index.html',
	    	      'admin/*.html',
	    	      'admin/sample-config.js',
	    	      'contact/*.html',
	    	      'document/*.html',
	    	      'home/*.html',
	    	      'login/*.html',
	    	      'profile/*.html',
	    	      'components/**/*.html',
	    	      'static/**/*',
	    	     ],
	    	dest: '../src/main/webapp/'
    	},
    	fonts: {
    		expand: true,
    		cwd: 'app/bower_components/bootstrap/fonts',
	    	src: [
	    	      '*'
	    	     ],
	    	dest: '../src/main/webapp/fonts'    		
    	},
    },
    
    clean: {
    	 html: {
    		 src: [
    		      '../src/main/webapp/index.html',
			      '../src/main/webapp/admin',
			      '../src/main/webapp/contact',
			      '../src/main/webapp/document',
			      '../src/main/webapp/home',
			      '../src/main/webapp/login',
			      '../src/main/webapp/profile',
			      '../src/main/webapp/components',
			      '../src/main/webapp/static'
			      ] 
    	 },
    	 js: {
	        src: [
	              '../src/main/webapp/<%= pkg.name %>.js',
	              '../src/main/webapp/<%= pkg.name %>.min.js'
	              ]

    	 },
    	 libraries: {
    		 src: [ '../src/main/webapp/libs.js' ]
    	 },
    	 css: {
    		 src: [ '../src/main/webapp/css' ] 
    	 },
    	 fonts: {
    		 src: ['../src/main/webapp/fonts']
    	 },
    	 
    	 options: {
    		 force:true
    	 }
    }
  });

  // Load the plugin that provides the "uglify" task.
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-clean');

  // Default task(s).
  grunt.registerTask('default', ['concat', 'uglify', 'copy']);

};
