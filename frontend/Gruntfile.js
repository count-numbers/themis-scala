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
    		   'app/**/*.css',
    		   'app/**/*.scss',
    		   '!app/bower_components/**/*'
    		],
    		tasks: ['concat', 'uglify', 'sass']
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
     		      'app/sources/sources.js',
     		      'app/login/login.js',
     		      'app/admin/admin.js',
     		      'app/components/auth.js',
     		      'app/components/domain/domain.js',
     		      'app/components/domain/editable-markup.js',
     		      'app/components/util/trello.js',
     		      'app/components/util/gdrive.js'
    		],
    		dest: '../public/<%= pkg.name %>.js'
    	},
    	css: {
    		src: [

            	  'app/**/*.css',
            	  'app/**/*.scss',
            	  '!app/bower_components/**/*.css',
            	  '!app/bower_components/**/*.scss',
    		],
    		dest: '../public/css/<%= pkg.name %>.scss'
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
    		dest: '../public/libs.js'
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
    		dest: '../public/css/libs.css'
    	},

    },
    
    uglify: {
      options: {
        banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n',
        mangle: false
      },
      build: {
        src:  '../public/<%= pkg.name %>.js',
        dest: '../public/<%= pkg.name %>.min.js'
      }
    },

    sass: {
        dist: {
          options: {
            style: 'expanded'
          },
          files: {
            '../public/css/<%= pkg.name %>.css': '../public/css/<%= pkg.name %>.scss'
          }
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
	    	      'sources/*.html',
	    	      'components/**/*.html',
	    	      'static/**/*',
	    	     ],
	    	dest: '../public/'
    	},
    	fonts: {
    		expand: true,
    		cwd: 'app/bower_components/bootstrap/fonts',
	    	src: [
	    	      '*'
	    	     ],
	    	dest: '../public/fonts'    		
    	},
    },
    
    clean: {
    	 html: {
    		 src: [
    		      '../public/index.html',
			      '../public/admin',
			      '../public/contact',
			      '../public/document',
			      '../public/home',
			      '../public/login',
			      '../public/profile',
			      '../public/sources',
			      '../public/components',
			      '../public/static'
			      ] 
    	 },
    	 js: {
	        src: [
	              '../public/<%= pkg.name %>.js',
	              '../public/<%= pkg.name %>.min.js'
	              ]

    	 },
    	 libraries: {
    		 src: [ '../public/libs.js' ]
    	 },
    	 css: {
    		 src: [ '../public/css' ] 
    	 },
    	 fonts: {
    		 src: ['../public/fonts']
    	 },
    	 
    	 options: {
    		 force:true
    	 }
    }
  });


  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-sass');

  // Default task(s).
  grunt.registerTask('default', ['concat', 'uglify', 'copy', 'sass']);

};
