JobsheetMaker - CLI version 1.0
By Rowan Walker
Created on 2017-01-16
Last updated on 2017-08-09

NOTE: This file serves as an artifact from the prototype version of this
project (the CLI version); the instructions contained are outdated.

1. Overview
-----------

JobsheetMaker is a program that receives CSV files downloaded from Malinko as
inputs, specifically Field Workers' jobsheet data from the Day Overview page.
It then fills out and outputs custom PDF jobsheets to be used by workers in
their work days.

2. Instructions
---------------

This version of JobsheetMaker does not come with a GUI; this feature is
planned for a future release.

In the meantime, JobsheetMaker may be launched without arguments by
doubleclicking "JobsheetMaker.jar" found in the same folder as this document.
This will cause the program to look for a file called "input.csv" in the local
directory, and use it as input. Having found the file, it will then output a
PDF jobsheet named according to the field worker in the input file.

If you replace the "input.csv" file with one you have downloaded from Malinko
and doubleclick "JobsheetMaker.jar", it will convert your data to a PDF.

NOTE: JobsheetMaker uses two template PDF files ("Day Overview Template.pdf"
and "Single Job Template.pdf") to produce the combined output. These files are
required in order for the program to work. If they are deleted, JobsheetMaker
will crash!

3. Command line arguments
-------------------------

JobsheetMaker may also be launched from the command line. To do this, open a
command prompt and type:

    java -jar JobsheetMaker.jar

This will run the program without arguments. Additional csv files may be given
as arguments to be processed simultaneously. For example, to load and process
"apple.csv", "banana.csv" and "carrot.csv" all at once, type:

    java -jar JobsheetMaker.jar apple.csv banana.csv carrot.csv

There is no limit to how many extra files may be specified.

4. Command line tips
--------------------

Using a command line can be tricky, so here are some tips:

    - To open a command line at a folder, hold Shift and rightclick the
      folder. There will be an "Open command window here" option in the menu.

    - When typing commands or arguments, you can hit TAB to automatically
      complete a partial input. For instance, you can type "java -jar Jobs"
      then hit TAB to complete your input to "java -jar JobsheetMaker.java".
      This also works for arguments.

5. Contact
----------

If you have any questions, please email Rowan at <rowan.walker1@gmail.com>.