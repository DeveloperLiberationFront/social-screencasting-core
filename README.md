##Getting setup to Build (updated 7/11/2014)##
**Note: If the last time you set up the project was before 7/11/2014, I recommend you clear out your old project setup and re-clone the repository**

1. Install [Mercurial](http://mercurial.selenic.com/downloads).  Download the package w/o TortoiseHg You won't need a gui, but if you want one, I recommend [SourceTree](http://www.sourcetreeapp.com/).  Install Eclipse Kepler.  Optionally install [The FindBugs Eclipse Plugin](http://findbugs.sourceforge.net/downloads.html).
2. Make a new folder where you will clone the source code (e.g. screencasting).  Navigate to this in a command prompt.
3. Clone https://bitbucket.org/klubick/screencasting-module to this new folder.
4. Clone this repo to the new folder.
5. Open Eclipse.  Go to File>Import.  Select General>Existing Projects into Workspace and choose the screencasting-module.  Repeat for the screencasting local hub.
6. Browse to the folder /src/etc and make a copy of log4j.settings.default.  Change this name to log4j.settings.  This will affect the logging habits of your build.  Feel free to change the .settings file as it is ignored by source control.  If you need to set up logging in code (for example, if you make your own main method in a new class), call `TestingUtils.makeSureLoggingIsSetUp();` to load the settings file.
7. Run edu.ncsu.lubick.Runner to test everything is set up well.

