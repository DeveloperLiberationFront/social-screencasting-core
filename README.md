##Getting setup to Build (updated 7/11/2014)##
**Note: If the last time you set up the project was before 7/11/2014, I recommend you clear out your old project setup and re-clone the repository**

1. Install [Mercurial](http://mercurial.selenic.com/downloads).  Download the package w/o TortoiseHg You won't need a gui, but if you want one, I recommend [SourceTree](http://www.sourcetreeapp.com/).  Install Eclipse Kepler.  Optionally install [The FindBugs Eclipse Plugin](http://findbugs.sourceforge.net/downloads.html).
2. Make a new folder where you will clone the source code (e.g. screencasting).  Navigate to this in a command prompt.
3. Clone https://bitbucket.org/klubick/screencasting-module to this new folder.
4. Clone this repo to the new folder.
5. Open Eclipse.  Go to File>Import.  Select General>Existing Projects into Workspace and choose the screencasting-module.  Repeat for the screencasting local hub.
6. Browse to the folder /src/etc and make a copy of log4j.settings.default.  Change this name to log4j.settings.  This will affect the logging habits of your build.  Feel free to change the .settings file as it is ignored by source control.  If you need to set up logging in code (for example, if you make your own main method in a new class), call `TestingUtils.makeSureLoggingIsSetUp();` to load the settings file.
7. Run edu.ncsu.lubick.Runner to test everything is set up well.

###Setting up Web Development###
If you will be interacting with the front end, you'll need to do a bit more setup.  We use [Bower](http://bower.io/) to manage our dependencies for things like [Angular.js](https://angularjs.org/), [JQuery](http://jquery.com/), [Bootstrap](http://getbootstrap.com/), etc.  Bower uses git to fetch dependencies, so you'll need to:

1.  Install [git](http://git-scm.com/downloads).  Remember to add this to your path and restart all shells to get git in there.
2.  Install [Node.js](http://nodejs.org/). This should include npm, the node package manager. Remember to add node and npmp to your path and restart all shells.
3.  Install Bower by running `npm install -g bower` in a command line.
4.  Navigate the shell to your folder from the earlier step 2 (screencasting) and then cd ../src/frontend/public_html.  
5.  Execute `bower install`.  This will download all the dependencies from the bower.json file.  This may take a while.
6.  Refresh your ScreencastingLocalHub project in Eclipse.  If you already have a server running, it should be smart enough to get the new files.  Otherwise, open up chrome dev tools and enable Settings>Disable cache when DevTools is open, then hit Ctrl+F5 to force a full refresh.

**Bower is not run automatically, so if you are getting an error about not being able to find a library/resource, it's probably because someone else added a new dependency.  Try re-running steps 4-6.**