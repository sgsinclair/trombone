Trombone
========

This is an experimental rewrite of Trombone (version 4), the back-end system
for Voyant Tools. Please note that everything here is in flux and likely to
change. In fact, this entire codebase may be abandoned if significant
difficulties are encountered. If you're interested in the current version of
Voyant Tools (version 3.x available through
[voyant-tools.org](http://voyant-tools.org/], please see 
[here] (http://dev.hermeneuti.ca/~hermeneutica/voyeur/trac/)
instead.

Trombone is a multi-purpose text analysis library. It makes it relatively easy
to create corpora from a variety of sources (URLs, files, strings, etc.) and in
a variety of formats (XML, HTML, PDF, etc.). Some basic term frequency,
distribution, and keyword-in-context functionality is available (intended for
use by more sophisticated front-end interfaces like Voyant Tools).

## Quick Reference ##

While awaiting more detailed documentation, here's a quick overview of how to use Trombone in stand-alone mode. The most important thing is to run org.voyanttools.trombone.Controller as a Java Application (in Eclipse, just right-click on the file, choose *Run As* and select *Java Application*). This should produce results in the console (not very interesting ones since we haven't provided any parameters). To run more interesting operations, edit the run configuration that was created when you just ran the application. Use quotes around key=value pairs that have spaces. Here are some useful parameters:

* **storage**: *file|memory* use *file* to force things to be written (and reused) on disk
* importing – each of these can be duplicated (e.g. string=the string=a)
	* **file**: the location of a file or directory to import 
	* **uri**: the location of a URI to import
	* **string**: a string to import
* tools: specify the tool key and use one of these values:
	* **corpus.CorpusMetadata**: get high-level metadata (this is useful for minimal output during corpus ingestion)
	* **corpus.CorpusTerms**: list the terms in the corpus
	* **corpus.DocumentTerms**: list the terms in each document
		* **docIndex**: *number* the location of the document(s) in the corpus
		* **docId**: the document ID


License
-------

Trombone is currently under a GPLv3 license. See the [license file](.license.txt).